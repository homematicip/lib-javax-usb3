package javax.usb3.event;

import javax.usb3.exception.UsbException;

public interface IUsbPipeEventListener {
	public void pipeDataIn(byte[] bytes);
	public void pipeError(UsbException e);
}
