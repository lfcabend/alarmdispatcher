package com.alarmdispatcher.lau.alarmdispatcher;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Lau on 2/8/2016.
 */
public class BluetoothHandler {

    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String DEVICE_NAME = "HC-06";
    public static String PIN = "1234";

    private static final String TAG = BluetoothHandler.class.getSimpleName();

    private BluetoothAdapter defaultAdapter;
    private Map<String, String> pairedDevices;
    private Map<String, String> discoveredDevices;

    private volatile BluetoothSocket bluetoothSocket;
    private volatile boolean connected;

    private ExecutorService executorService;

    public BluetoothHandler() {
        executorService = Executors.newFixedThreadPool(1);
    }

    public void startDiscovery() throws BluetoothException {
        if (defaultAdapter != null) {
            defaultAdapter.startDiscovery();
        } else {
            throw new BluetoothException("Not initialized");
        }
    }


    public interface DiscoveredDeviceCallBack {

        void deviceDiscovered(BluetoothDevice device);

    }

    public interface ConnectionCallbackHandler {

        void connectionSuccessful();

        void connectionFailed(BluetoothException e);

        void messageRead(String message);

        void errorOccurred(BluetoothException e);

        void disconnected();

    }


    public BroadcastReceiver getBroadcastReceiver(final DiscoveredDeviceCallBack discoveredDeviceCallBack) {
        return new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    if (discoveredDevices == null) {
                        discoveredDevices = new HashMap<String, String>();
                    }
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String name = TextUtils.isEmpty(device.getName()) ? device.getAddress() : device.getName();
                    Log.d(TAG, "Device discovered " + name + " with address: " + device.getAddress());
                    discoveredDevices.put(name, device.getAddress());
                    discoveredDeviceCallBack.deviceDiscovered(device);
                }
            }
        };
    }

    public void initialize() throws BluetoothException {
        Log.d(TAG, "initializing bluetooth");
        defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!isBluetoothAvailable()) {
            throw new BluetoothException("No bluetooth available");
        } else if (!isBluetoothOn()) {
            throw new BluetoothException("Turn on bluetooth");
        }
        Log.d(TAG, "Received bluetooth adapter");
        refreshPairedDevicesList();
    }

    public boolean containsPairedDevice(String name) {
        if (pairedDevices != null) {
            return pairedDevices.containsKey(name);
        } else {
            return false;
        }
    }

    public void refreshPairedDevicesList() {
        this.pairedDevices = getPairedDevicesList();
    }

    public static boolean isBluetoothAvailable() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    public static boolean isBluetoothOn() {
        return BluetoothAdapter.getDefaultAdapter() != null &&
                BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public List<String> getDeviceNames() {
        if (pairedDevices != null) {
            return new ArrayList<String>(pairedDevices.keySet());
        } else {
            return new ArrayList<String>();
        }
    }

    public List<String> getAllDevicesNames() {

        return new ArrayList<String>(getAllDevices().keySet());
    }

    public boolean isPairedWithClock() {
        return pairedDevices.containsKey(DEVICE_NAME);
    }

    public Map<String, String> getAllDevices() {
        Map<String, String> all = new HashMap<String, String>();
        if (pairedDevices != null) {
            all.putAll(pairedDevices);
        }
        if (discoveredDevices != null) {
            all.putAll(discoveredDevices);
        }
        return all;
    }

    public void connect(final String address, final ConnectionCallbackHandler connectionCallbackHandler)
            throws BluetoothException {
        if (!isConnected()) {
            executorService.submit(new AsyncConnector(address, connectionCallbackHandler));
        } else {
            throw new BluetoothException("isAlready connected");
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void writeMessage(String message) throws BluetoothException {
        writeMessage((message + "\n").getBytes(Charset.forName("UTF-8")));
    }

    public void writeMessage(byte[] message) throws BluetoothException {
        if (isConnected()) {
            try {
                Log.d(TAG, "Writing message: " + toHex(message));
                bluetoothSocket.getOutputStream().write(message);
                bluetoothSocket.getOutputStream().flush();
            } catch (IOException e) {
                throw new BluetoothException("Error writing message", e);
            }
        } else {
            throw new BluetoothException("Not connected");
        }
    }


    private static String toHex(byte[] bytes) {
        return String.format("%040x", new BigInteger(1, bytes));
    }


    private Map<String, String> getPairedDevicesList() {
        Map<String, String> pairedDevices = new HashMap<>();
        Set<BluetoothDevice> boundedDevices = defaultAdapter.getBondedDevices();
        for (BluetoothDevice bt : boundedDevices) {
            String name = TextUtils.isEmpty(bt.getName()) ? bt.getAddress() : bt.getName();
            Log.d(TAG, "Paired device " + name + " with adress: " + bt.getAddress());
            pairedDevices.put(name, bt.getAddress());
        }
        return pairedDevices;
    }

    public void close() {
        Log.d(TAG, "closing handler");
        connected = false;
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
            } finally {
                bluetoothSocket = null;
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private class AsyncConnector implements Runnable {

        public static final int LF = 10;
        public static final int CR = 13;
        private String address;
        private ConnectionCallbackHandler connectionCallbackHandler;
        private volatile InputStream inputStream;
        private ByteArrayOutputStream out = new ByteArrayOutputStream();
        private boolean readLF = false;
        private boolean readCR = false;

        public AsyncConnector(String address, ConnectionCallbackHandler connectionCallbackHandler)
                throws BluetoothException {

            try {
                this.address = address;
                this.connectionCallbackHandler = connectionCallbackHandler;
                Log.d(TAG, "Connecting with address: " + address);
                BluetoothDevice clockDevice = defaultAdapter.getRemoteDevice(address);//connects to the device's address and checks if it's available
                Log.d(TAG, "Creating insecureRfcommSocketToServiceRecord: " + address);
                bluetoothSocket = clockDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                throw new BluetoothException("Could not create connection; " + e.getMessage(), e);
            }
        }

        @Override
        public void run() {
            connect();
            Log.d(TAG, "Going to read data");
            readWhileConnected();
            Log.d(TAG, "Connection handler thread is done");
        }

        private void readWhileConnected() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (isConnected()) {
                try {
                    // Read from the InputStream
                    readLine();
                    if (readCR) {
                        handleMessageRead();
                    }
                } catch (IOException e) {
                    handleReadError(new BluetoothException("Error while reading: " + e.getMessage(), e));
                } catch (Exception e) {
                    handleReadError(new BluetoothException("Error while reading: " + e.getMessage(), e));
                }
            }
        }

        private void handleMessageRead() {
            String message = new String(out.toByteArray(), Charset.forName("UTF-8"));
            Log.d(TAG, "read message: " + message);
            reset();
            connectionCallbackHandler.messageRead(message);
        }

        private void handleReadError(BluetoothException e2) {
            Log.e(TAG, "Error while reading message: " + e2.getMessage(), e2);
            close();
            connectionCallbackHandler.errorOccurred(e2);
            connectionCallbackHandler.disconnected();
        }

        private void reset() {
            out.reset();
            readCR = false;
            readLF = false;
        }


        private void readLine() throws IOException {
            Log.d(TAG, "Going to read a line");
            int read = inputStream.read();
            Log.d(TAG, "Read: " + read + "");
            if (read == LF) {
                Log.d(TAG, "read LF");
                readLF = true;
            } else if (read == CR) {
                Log.d(TAG, "read CR");
                readCR = true;
            } else {
                out.write(read);
            }
        }

        private void connect() {
            try {
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                Log.d(TAG, "Trying to connect to address: " + address);
                bluetoothSocket.connect();
                connected = true;
                Log.d(TAG, "Connected with address: " + address);
                inputStream = bluetoothSocket.getInputStream();
                connectionCallbackHandler.connectionSuccessful();
            } catch (IOException e) {
                connectionCallbackHandler.
                        connectionFailed(new BluetoothException("Could not connect; " + e.getMessage(), e));
            } catch (IllegalArgumentException e) {
                connectionCallbackHandler.
                        connectionFailed(new BluetoothException("Address " + address + " is invalid" + e.getMessage(), e));
            }
        }
    }


}
