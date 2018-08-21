package javax.usb3.event;

import java.util.ArrayList;
import java.util.List;

import javax.usb3.exception.UsbException;
import javax.usb3.ri.UsbPipe;

public class UsbPipeReader implements Runnable{
	
	private Thread thread;
	
	private List<IUsbPipeEventListener> listeners = new ArrayList<IUsbPipeEventListener>();
	private UsbPipe pipe;
	private boolean pauseReader = false;
	private final Object pauseLock = new Object();
	
	public UsbPipeReader(UsbPipe pipe) {
		this.pipe = pipe;
	}

	/**
	 * Request the USB pipe and notifys the listeners when there is a message
	 */
	public void run() {
		while (listeners.size()>0) {
			synchronized (pauseLock) {
				if (listeners.size()==0) { // may have changed while waiting to
			                    			// synchronize on pauseLock
			        break;
			    }
				if (pauseReader) {
                    try {
                        pauseLock.wait(); // will cause this Thread to block until 
                                          // another thread calls pauseLock.notifyAll()
                                          // Note that calling wait() will 
                                          // relinquish the synchronized lock that this 
                                          // thread holds on pauseLock so another thread
                                          // can acquire the lock to call notifyAll()
                                          // (link with explanation below this code)
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (listeners.size()==0) { // running might have changed since we paused
                        break;
                    }
                }
			}
			if(!pipe.getUsbEndpoint().getUsbInterface().isClaimed()) {
				try {
					pipe.getUsbEndpoint().getUsbInterface().claim();
				}catch (UsbException e) {
					for (IUsbPipeEventListener listener : listeners) {
						listener.pipeError(e);
					}
				}
			}
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
				int length = pipe.syncSubmit(data);
				if (length>0) {
					for (IUsbPipeEventListener listener : listeners) {
						listener.pipeDataIn(data);
					}
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
	
	public void stop() {
		listeners = new ArrayList<IUsbPipeEventListener>();
	}
	
	public synchronized void addListener(IUsbPipeEventListener listener) {
		this.listeners.add(listener);
		if(this.listeners.size()==1) {
			this.start();
		}
	}
	
	public synchronized void removeListener(IUsbPipeEventListener listener) {
		this.listeners.remove(listener);
	}

	public UsbPipe getPipe() {
		return pipe;
	}

	public void setPipe(UsbPipe pipe) {
		this.pipe = pipe;
	}

	public synchronized boolean isPauseReader() {
		return pauseReader;
	}

	public synchronized void pauseReader() {
		this.pauseReader = true;
	}
	
	public synchronized void resumeReader() {
        synchronized (pauseLock) {
        	pauseReader = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
	}
	
	
	
	

}
