/*
 * Copyright (C) 2011 Klaus Reimer <k@ailis.de>
 * See LICENSE.md for licensing information.
 */
package org.usb4java.javax;

import java.util.List;
import javax.usb.*;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;
import javax.usb.exception.UsbException;
import javax.usb.exception.UsbNotActiveException;
import javax.usb.exception.UsbNotClaimedException;
import javax.usb.exception.UsbNotOpenException;
import javax.usb.util.DefaultUsbControlIrp;
import javax.usb.util.DefaultUsbIrp;

/**
 * usb4java implementation of UsbPipe.
 * <p>
 * @author Klaus Reimer (k@ailis.de)
 */
public final class Pipe implements UsbPipe {

  /**
   * The endpoint this pipe belongs to.
   */
  private final Endpoint endpoint;

  /**
   * The USB pipe listeners.
   */
  private final PipeListenerList listeners = new PipeListenerList();

  /**
   * If pipe is open or not.
   */
  private boolean opened;

  /**
   * The request queue.
   */
  private final IrpQueue queue;

  /**
   * Constructor.
   * <p>
   * @param endpoint The endpoint this pipe belongs to.
   */
  Pipe(final Endpoint endpoint) {
    this.endpoint = endpoint;
    this.queue = new IrpQueue(this);
  }

  /**
   * Returns the USB device.
   * <p>
   * @return The USB device.
   */
  public UsbDevice getDevice() {
    return this.endpoint.getUsbInterface().getUsbConfiguration().getUsbDevice();
  }

  /**
   * Ensures the pipe is active.
   * <p>
   * @throws UsbNotActiveException When pipe is not active
   */
  private void checkActive() {
    if (!isActive()) {
      throw new UsbNotActiveException("Pipe is not active.");
    }
  }

  /**
   * Ensures the interface is active.
   * <p>
   * @throws UsbNotClaimedException When interface is not claimed.
   */
  private void checkClaimed() {
    if (!this.endpoint.getUsbInterface().isClaimed()) {
      throw new UsbNotClaimedException("Interface is not claimed.");
    }
  }

  /**
   * Ensures the device is connected.
   * <p>
   * @throws UsbDisconnectedException When device has been disconnected.
   */
//  private void checkConnected() {    getDevice().checkConnected();  }
  /**
   * Ensures the pipe is open.
   * <p>
   * @throws UsbNotOpenException When pipe is not open.
   */
  private void checkOpen() {
    if (!isOpen()) {
      throw new UsbNotOpenException("Pipe is not open.");
    }
  }

  @Override
  public void open() throws UsbException {
    checkActive();
    checkClaimed();
//    checkConnected();
    if (this.opened) {
      throw new UsbException("Pipe is already open");
    }
    this.opened = true;
  }

  @Override
  public void close() throws UsbException {
    checkActive();
    checkClaimed();
//    checkConnected();
    if (!this.opened) {
      throw new UsbException("Pipe is already closed");
    }
    if (this.queue.isBusy()) {
      throw new UsbException("Pipe is still busy");
    }
    this.opened = false;
  }

  @Override
  public boolean isActive() {
    final UsbInterface iface = this.endpoint.getUsbInterface();
    final UsbConfiguration config = iface.getUsbConfiguration();
    return iface.isActive() && config.isActive();
  }

  @Override
  public boolean isOpen() {
    return this.opened;
  }

  @Override
  public UsbEndpoint getUsbEndpoint() {
    return this.endpoint;
  }

  @Override
  public int syncSubmit(final byte[] data) throws UsbException {
    final UsbIrp irp = asyncSubmit(data);
    irp.waitUntilComplete();
    if (irp.isUsbException()) {
      throw irp.getUsbException();
    }
    return irp.getActualLength();
  }

  @Override
  public UsbIrp asyncSubmit(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("data must not be null");
    }
    final UsbIrp irp = createUsbIrp();
    irp.setAcceptShortPacket(true);
    irp.setData(data);
    asyncSubmit(irp);
    return irp;
  }

  @Override
  public void syncSubmit(final UsbIrp irp) throws UsbException {
    if (irp == null) {
      throw new IllegalArgumentException("irp must not be null");
    }
    asyncSubmit(irp);
    irp.waitUntilComplete();
    if (irp.isUsbException()) {
      throw irp.getUsbException();
    }
  }

  @Override
  public void asyncSubmit(final UsbIrp irp) {
    if (irp == null) {
      throw new IllegalArgumentException("irp must not be null");
    }
    checkActive();
//    checkConnected();
    checkOpen();
    this.queue.add(irp);
  }

  @Override
  public void syncSubmit(final List<UsbIrp> list) throws UsbException {
    for (final UsbIrp irp : list) {
      syncSubmit(irp);
    }
  }

  @Override
  public void asyncSubmit(final List<UsbIrp> list) {
    for (final UsbIrp irp : list) {
      asyncSubmit(irp);
    }
  }

  @Override
  public void abortAllSubmissions() {
    checkActive();
//    checkConnected();
    checkOpen();
    this.queue.abort();
  }

  @Override
  public UsbIrp createUsbIrp() {
    return new DefaultUsbIrp();
  }

  @Override
  public UsbControlIrp createUsbControlIrp(final byte bmRequestType,
                                           final byte bRequest,
                                           final short wValue, final short wIndex) {
    return new DefaultUsbControlIrp(bmRequestType, bRequest, wValue, wIndex);
  }

  @Override
  public void addUsbPipeListener(final UsbPipeListener listener) {
    this.listeners.add(listener);
  }

  @Override
  public void removeUsbPipeListener(final UsbPipeListener listener) {
    this.listeners.remove(listener);
  }

  /**
   * Sends event to all event listeners.
   * <p>
   * @param irp Then request package
   */
  public void sendEvent(final UsbIrp irp) {
    if (irp.isUsbException()) {
      this.listeners.errorEventOccurred(new UsbPipeErrorEvent(this, irp));
    } else {
      this.listeners.dataEventOccurred(new UsbPipeDataEvent(this, irp));
    }
  }

  @Override
  public String toString() {
    return String.format("USB pipe of endpoint %02x",
                         this.endpoint.getUsbEndpointDescriptor().bEndpointAddress());
  }
}