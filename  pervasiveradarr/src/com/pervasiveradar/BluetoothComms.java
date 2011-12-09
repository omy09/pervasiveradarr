

package com.pervasiveradar;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.pervasiveradar.AbstractPacket;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothComms {
    // Debugging
    private static final String TAG = "BluetoothComms";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket  
    private static final String NAME_INSECURE = "BluetoothInsecure";

    // Unique UUID for this application   
    private static final UUID MY_UUID_INSECURE =
       UUID.fromString("54a0d176-f32b-4f86-81bc-657c0e9f45e4");    	

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;  
    ObjectOutputStream out;
    private BluetoothSocket mmSocket;   
    boolean isAccepted;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothComms(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
          
    }  
  
    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;        
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }


    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode.  */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");			        					        		    	
        
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);   
        // Start the thread to listen on a BluetoothServerSocket
       
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.setPriority(Thread.MAX_PRIORITY);
            mInsecureAcceptThread.start();
        }
    }
    
    public void cancelConnectionAttempt() {
        try {
            mmSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "close() of connect  socket failed", e);
        }
    }
    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect    
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}       

        // Cancel any thread currently running a connection
       // if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        if (mConnectedThread == null) {
        	mConnectThread = new ConnectThread(device);        
        	mConnectThread.start();         
        	setState(STATE_CONNECTING); 
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        if (D) Log.d(TAG, "connected");        
     // Cancel the thread that completed the connection
        if (mConnectThread != null) {
        	mConnectThread.cancel();
        	mConnectThread = null;
        	}        
    // Cancel the accept thread         
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        } 
        // Cancel any thread currently running a connection
       // if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}         
        if (mState == STATE_CONNECTING && !isAccepted)
        	DisasterOperations.connAttemptsSucceded++;			
		isAccepted = false;	
		
        // Start the thread to manage the connection and perform transmissions
        if (mState != STATE_CONNECTED ) {
        	setState(STATE_CONNECTED); 
        	
        	// Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(DisasterOperations.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(DisasterOperations.DEVICE_NAME, device.getName());
            bundle.putString(DisasterOperations.DEVICE_ADDRESS, device.getAddress());
            msg.setData(bundle);
            mHandler.sendMessage(msg);  
                    	
        	mConnectedThread = new ConnectedThread(socket);       		
       		mConnectedThread.start();
       		
       	 
        } else
			try {
				socket.close();
			} catch (IOException e) {
				Log.e(TAG,"closing socket inside connected since state == CONNECTED", e);
			}
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mConnectThread != null) {
        	 if (mState != STATE_CONNECTING) {
        		 mConnectThread.cancel();
        		 mConnectThread = null;
        	 }
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }  
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(AbstractPacket out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionDelayed(BluetoothDevice device) {
        Message msg = mHandler.obtainMessage(DisasterOperations.MESSAGE_DELAY);
        Bundle bundle = new Bundle();
        bundle.putParcelable(DisasterOperations.DEVICE, device);      
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(DisasterOperations.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(DisasterOperations.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);        
        // Start the service over to restart listening mode
        synchronized (this) {
        	if (mState != STATE_CONNECTED)
            	setState(STATE_LISTEN); 
        	//if (mState != STATE_CONNECTED)
        		//BluetoothService.this.start();
        	// Cancel any thread attempting to make a connection
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}       
            
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {            
        // Start the service over to restart listening mode
    	
    	this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        volatile BluetoothSocket acceptSocket = null;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;            

            // Create a new listening server socket
            try {               
              tmp = mAdapter.listenUsingRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);                
            } catch (Exception e) {
                Log.d(TAG,  "listen() failed");
                tmp =  null;
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread" );           
           if (mmServerSocket != null) {
            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception                	
                    acceptSocket = mmServerSocket.accept();                   
                    cancel();                                   
                    Log.i(TAG,"accepted incoming connection");
                    
                } catch (IOException e) {
                   // Log.e(TAG, "accept() failed", e);
                    break;
                }
               /* catch (NullPointerException e) {
                	Log.e(TAG, "accept() failed: restarting", e);                    
                    synchronized (this) {
                    	stop();
                    	start();                    	
                    }
                    break; 
                }*/
                
                // If a connection was accepted
                if (acceptSocket != null) {
                    synchronized (this) {                    	
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                        	isAccepted = true;
                            connected(acceptSocket, acceptSocket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                acceptSocket.close();
                            } catch (Exception e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
        }
            if (D) Log.i(TAG, "END mAcceptThread " );
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
            	if (mmServerSocket !=  null)
            		mmServerSocket.close();               
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
      
        private final BluetoothDevice mmDevice;
       

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {               
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);                
            } catch (IOException e) {
                Log.e(TAG,  "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread ");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception             	        
                   		mmSocket.connect();
                                              
                Log.i(TAG,"connection attempt succeded");
            } catch (IOException e) {            	
            	//Log.e(TAG, "connection attempt failed", e);
                try { 
                	Thread.yield();
                    mmSocket.close();                    
                } catch (Exception e2) {
                    Log.e(TAG, "unable to close()  socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
            }
            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "close() of connect  socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();                
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");          

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream                   
                    ObjectInputStream in = new ObjectInputStream(mmInStream);
                    Object buffer;
					try {						
						buffer = in.readObject();
						// Send the obtained bytes to the UI Activity
	                    mHandler.obtainMessage(DisasterOperations.MESSAGE_READ, -1, -1, buffer)
	                            .sendToTarget();
					} catch (Exception e) {	}                   
                    
                } catch (Exception e) {
                    Log.e(TAG, "disconnected", e);                     
                    connectionLost();                                  
                   
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public synchronized void write(AbstractPacket buffer) {
            try {                  
                out = new ObjectOutputStream(mmOutStream);
                // Send it
                out.writeObject(buffer);                
                out.flush();     
               
            } catch (Exception e) {
                Log.e(TAG, "Exception during write", e);
                connectionLost();
            }
        }

        public void cancel() {
            try {
            	
                mmSocket.close();                 
            } catch (Exception e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}