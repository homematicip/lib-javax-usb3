package javax.usb3.event;

import java.util.ArrayList;
import java.util.List;

import javax.usb3.exception.UsbException;
import javax.usb3.ri.UsbPipe;

public class UsbPipeReader implements Runnable{
	
	private Thread thread;
	
	private List<IUsbPipeEventListener> listeners = new ArrayList<IUsbPipeEventListener>();
	private UsbPipe pipe;
	
	public UsbPipeReader(UsbPipe pipe) {
		this.pipe = pipe;
	}

	/**
	 * Request the USB pipe and notifys the listeners when there is a message
	 */
	public void run() {
		while (listeners.size()>0) {
			if(!pipe.isOpen()) {
				try {
					pipe.open();
				} catch (UsbException e) {
					for (IUsbPipeEventListener listener : listeners) {
						listener.pipeError(e);
					}
				}
				
			}
			try {
				byte[] data = new byte[64];
				pipe.syncSubmit(data);
				for (IUsbPipeEventListener listener : listeners) {
					listener.pipeDataIn(data);
				}
			} catch (UsbException e) {
				for (IUsbPipeEventListener listener : listeners) {
					listener.pipeError(e);
				}
			}
			
		}
		
	}
	
	public void start () {
	      if (thread == null) {
	    	  thread = new Thread (this, "UsbPipeReader");
	    	  thread.start ();
	      }
	   }
	
	public void addListener(IUsbPipeEventListener listener) {
		this.listeners.add(listener);
		if(this.listeners.size()==1) {
			this.start();
		}
	}
	
	public void removeListener(IUsbPipeEventListener listener) {
		this.listeners.remove(listener);
	}

	public UsbPipe getPipe() {
		return pipe;
	}

	public void setPipe(UsbPipe pipe) {
		this.pipe = pipe;
	}
	
	

}
