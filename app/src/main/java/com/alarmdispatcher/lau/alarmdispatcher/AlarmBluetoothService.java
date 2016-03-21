package com.alarmdispatcher.lau.alarmdispatcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alarmdispatcher.lau.alarmdispatcher.commands.AlarmCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.SetTimeCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.SwitchHotness;
import com.alarmdispatcher.lau.alarmdispatcher.commands.UpdateDataCommand;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.alarmdispatcher.lau.alarmdispatcher.BluetoothHandler.*;
import static com.alarmdispatcher.lau.alarmdispatcher.Constants.*;
import static com.alarmdispatcher.lau.alarmdispatcher.commands.AlarmCommand.parseACommand;

/**
 * Created by lau on 3/2/16.
 */
public class AlarmBluetoothService extends Service
        implements ConnectionCallbackHandler {

    public static final String TAG = "AlarmBluetoothService";
    public static final String CONNECT_ACTION = "CONNECT";
    public static final String COMMAND_BUNDLE_KEY = "COMMAND";

    private BluetoothHandler bluetoothHandler;

    private List<OnChangeHandler> changeHandlers = new ArrayList<>();

    public AlarmBluetoothService() {
        Log.i("AlarmBluetoothService", "Constructor");
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        AlarmBluetoothService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AlarmBluetoothService.this;
        }
    }

    public interface OnChangeHandler {

        void onChange();

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("AlarmBluetoothService", "onCreate");
    }

    private void initializeBluetoothHandler() {
        Log.i(TAG, "Initializing bluetooth handler");
        if (bluetoothHandler != null) {
            cleanupHandler();
        } else {
            bluetoothHandler = new BluetoothHandler();
        }

        try {
            bluetoothHandler.initialize();
            connect();
        } catch (BluetoothException e) {
            Log.e(TAG, "Failed to initialize bluetooth handler", e);
            cleanupHandler();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize bluetooth handler", e);
            cleanupHandler();
        }
    }

    private void connect() throws BluetoothException {
        Log.i(TAG, "Connect");
        if (bluetoothHandler.getDeviceNames().contains(DEVICE_NAME)) {
            Log.d(TAG, "found clock");
            String address = bluetoothHandler.getAllDevices().get(DEVICE_NAME);
            bluetoothHandler.connect(address, this);
        } else {
            Log.d(TAG, "clock is not paired");
        }
    }

    private void scheduleConnectionRetry() {
        Log.i(TAG, "Scheduling alarm manager");
        AlarmManager alarmMananger = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long nextRetry = System.currentTimeMillis() + 1000 * 60 * 1;
        int requestCode = 0;
        Intent intent = new Intent(this, AlarmBluetoothService.class);
        int flags = 0;
        PendingIntent pendingIntent = PendingIntent.getService(this, requestCode, intent, flags);
        alarmMananger.set(AlarmManager.RTC_WAKEUP, nextRetry, pendingIntent);
    }

    public void updateBlueToothState(Date lastTimeUdated) {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        BluetoothConnectionState bluetoothConnectionState =
                initializeBluetoothConnectionState(settings);

        bluetoothConnectionState.setBluetoothAvailable(isBluetoothAvailable());
        bluetoothConnectionState.setBluetoothOn(isBluetoothOn());

        if (lastTimeUdated != null) {
            bluetoothConnectionState.setLastTimeUpdated(lastTimeUdated);
        }

        if (bluetoothHandler != null) {
            bluetoothConnectionState.setConnected(bluetoothHandler.isConnected());
            String deviceName = BluetoothHandler.DEVICE_NAME;
            boolean alarmClockPresent = bluetoothHandler.containsPairedDevice(deviceName);
            bluetoothConnectionState.setAlarmClockPresent(alarmClockPresent);
        } else {
            bluetoothConnectionState.setConnected(false);
            bluetoothConnectionState.setAlarmClockPresent(false);
        }
        updateConnectionState(settings, bluetoothConnectionState);

    }

    private void updateConnectionState(SharedPreferences settings, BluetoothConnectionState bluetoothConnectionState) {
        Gson gson = new Gson();
        SharedPreferences.Editor edit = settings.edit();
        String alarmStateJSON = gson.toJson(bluetoothConnectionState);
        edit.putString(CONNECTION_STATE, alarmStateJSON);
        edit.commit();
    }

    private BluetoothConnectionState initializeBluetoothConnectionState(SharedPreferences settings) {
        Gson gson = new Gson();
        BluetoothConnectionState bluetoothConnectionState;
        if (settings.contains(CONNECTION_STATE)) {
            String jsonString = settings.getString(CONNECTION_STATE, null);
            bluetoothConnectionState = gson.fromJson(jsonString, BluetoothConnectionState.class);
        } else {
            bluetoothConnectionState = new BluetoothConnectionState();
        }
        return bluetoothConnectionState;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("AlarmBluetoothService", "onDestroy");
        if (bluetoothHandler != null) {
            bluetoothHandler.close();
        }
        changeHandlers.clear();
    }


    public void addOnChangeHandler(OnChangeHandler onChangeHandler) {
        changeHandlers.add(onChangeHandler);
    }

    public void removeOnChangeHandler(OnChangeHandler onChangeHandler) {
        changeHandlers.remove(onChangeHandler);
    }

    private void notifyOnChange() {
        for (OnChangeHandler onChangeHandler : changeHandlers) {
            onChangeHandler.onChange();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        try {
            //always try to connect if not connected
            if (!isConnected()) {
                initializeBluetoothHandler();
            }

            AlarmCommand alarmCommand = getAlarmCommand(intent);

            if (alarmCommand != null) {
                Log.i(TAG, "Contains command key in bundle");
                sendCommand(alarmCommand);
            } else {
                Log.i(TAG, "Sending default time command");
                sendTimeCommand();
            }

            if (intent != null) {
                BootReceiver.completeWakefulIntent(intent);
            }
            Log.i(TAG, "Completed service @ " + SystemClock.elapsedRealtime());
            return START_STICKY;
        } finally {
            scheduleConnectionRetry();
        }
    }

    private AlarmCommand getAlarmCommand(Intent intent) {
        if (intent != null) {
            Log.i(TAG, "Intent is not null");

            if (intent.getExtras() != null) {
                Log.i(TAG, "Extras is not null");

                if (intent.getExtras().containsKey(COMMAND_BUNDLE_KEY)) {
                    Log.i(TAG, "contains command bundle key");
                    return (AlarmCommand) intent.getExtras().get(COMMAND_BUNDLE_KEY);
                } else {
                    Log.i(TAG, "Does not contain command bundle key");
                }
            } else {
                Log.i(TAG, "Extras is null");
            }
        }
        return null;
    }

    private boolean isConnected() {
        return bluetoothHandler != null && bluetoothHandler.isConnected();
    }

    public void sendCommand(AlarmCommand alarmCommand) {
        if (isConnected()) {
            try {
                Log.i(TAG, "Sending command " + alarmCommand);
                bluetoothHandler.writeMessage(alarmCommand.getMessage());
            } catch (BluetoothException e) {
                Log.e(TAG, "could not send command", e);
                cleanupHandler();
            }
        } else {
            Log.i(TAG, "Not sending command since not connected");
        }
    }

    private void cleanupHandler() {
        Log.i(TAG, "Cleaning up bluetooth handler");

        if (bluetoothHandler != null) {
            bluetoothHandler.close();
        }
        bluetoothHandler = null;
    }

    private void sendTimeCommand() {
        AlarmCommand alarmCommand = new SetTimeCommand();
        sendCommand(alarmCommand);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "binding");
        return mBinder;
    }


    @Override
    public void connectionSuccessful() {
        boolean enabled = getHotness();
        Log.i(TAG, "Connection Successful");
        sendHotnessCommand(enabled);
        sendTimeCommand();
        updateBlueToothState(null);
        notifyOnChange();
    }

    private boolean getHotness() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        return settings.getBoolean(SWITCH_STATE, false);
    }

    private void sendHotnessCommand(boolean enabled) {
        AlarmCommand alarmCommand = new SwitchHotness(enabled);
        sendCommand(alarmCommand);
    }


    @Override
    public void connectionFailed(BluetoothException e) {
        Log.i(TAG, "Connection Failed");
        scheduleConnectionRetry();
        notifyOnChange();
    }

    @Override
    public void messageRead(String message) {
        try {
            Log.i(TAG, "Message read: " + message);

            AlarmCommand alarmCommand = parseACommand(message);
            if (UpdateDataCommand.INS.equals(alarmCommand.getInstruction())) {

                UpdateDataCommand updateDataCommand = (UpdateDataCommand) alarmCommand;
                Log.d(TAG, "Updated data command: " + updateDataCommand);

                AlarmClockState alarmClockState = updateDataCommand.getAlarmClockState();
                updateClockState(alarmClockState);
                updateSwitchState(alarmClockState);

                updateBlueToothState(new Date());
                notifyOnChange();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during processing command", e);
        }
    }

    private void updateSwitchState(AlarmClockState alarmClockState) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        settings.edit().putBoolean(SWITCH_STATE, alarmClockState.isHot());
    }

    private void updateClockState(AlarmClockState alarmClockState) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor edit = settings.edit();
        Gson gson = new Gson();
        String alarmStateJSON = gson.toJson(alarmClockState);
        edit.putString(ALARM_STATE, alarmStateJSON);
        edit.commit();
    }

    @Override
    public void errorOccurred(BluetoothException e) {
        Log.e(TAG, "Error occurred", e);

    }

    @Override
    public void disconnected() {
        Log.d(TAG, "Disconnected");
        scheduleConnectionRetry();
        notifyOnChange();
    }
}
