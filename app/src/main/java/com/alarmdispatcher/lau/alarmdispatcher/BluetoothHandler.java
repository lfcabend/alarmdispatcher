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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Lau on 2/8/2016.
 */
public class BluetoothHandler {

    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String DEVICE_NAME = "HC-06";
    public static String PIN = "HC-06";

    private static final String TAG = BluetoothHandler.class.getSimpleName();

    private BluetoothAdapter defaultAdapter;
    private Map<String, String> pairedDevices;
    private Map<String, String> discoveredDevices;

    private BluetoothSocket bluetoothSocket;

    public BluetoothHandler() {
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
        if (defaultAdapter == null) {
            throw new BluetoothException("No bluetooth available");
        } else if (!defaultAdapter.isEnabled()) {
            throw new BluetoothException("Turn on bluetooth");
        }
        Log.d(TAG, "Received bluetooth adapter");
        this.pairedDevices = getPairedDevicesList();
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

    public BluetoothSocket connect(String address) throws BluetoothException {
        try {
            Log.d(TAG, "Connecting with address: " + address);
            BluetoothDevice clockDevice = defaultAdapter.getRemoteDevice(address);//connects to the device's address and checks if it's available
            Log.d(TAG, "Creating insecureRfcommSocketToServiceRecord: " + address);
            BluetoothSocket bluetoothSocket = clockDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            bluetoothSocket.connect();
            Log.d(TAG, "Connected with address: " + address);
            return bluetoothSocket;
        } catch (IOException e) {
            throw new BluetoothException("Could not connect; " + e.getMessage(), e);
        } catch(IllegalArgumentException e) {
            throw new BluetoothException("Address " + address + " is invalid" + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        boolean b = bluetoothSocket == null;
        Log.d(TAG, "isConnected: " + b);
        return b;
    }

    public void writeMessage(String message) throws BluetoothException {
        if (isConnected()) {
            try {
                Log.d(TAG, "Writing message: " + message);
                bluetoothSocket.getOutputStream().write(message.getBytes());
            } catch (IOException e) {
                throw new BluetoothException("Error writing message", e);
            }
        }
    }

    public String readString() throws BluetoothException {
        if (isConnected()) {
            ByteArrayOutputStream out = readByteArrayInternal();
            String s = new String(out.toByteArray());
            Log.d(TAG, "read message: " + s);
            return s;
        } else {
            throw new BluetoothException("Not connected");
        }
    }

    public byte[] readByteArray() throws BluetoothException {
        if (isConnected()) {
            ByteArrayOutputStream out = readByteArrayInternal();
            byte[] bytes = out.toByteArray();
            Log.d(TAG, "read bytes: " + toHex(bytes));
            return bytes;
        } else {
            throw new BluetoothException("Not connected");
        }
    }

    private static String toHex(byte[] bytes) {
        return String.format("%040x", new BigInteger(1, bytes));
    }

    @NonNull
    private ByteArrayOutputStream readByteArrayInternal() throws BluetoothException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream inputStream = bluetoothSocket.getInputStream();
            int available = inputStream.available();
            while (available > 0) {
                byte[] buffer = new byte[1024];
                int read = inputStream.read(buffer);
                out.write(buffer, 0, read);
            }
            return out;
        } catch (IOException e) {
            throw new BluetoothException("Error writing message", e);
        }
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
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
            } finally {
                bluetoothSocket = null;
            }
        }
    }

}
