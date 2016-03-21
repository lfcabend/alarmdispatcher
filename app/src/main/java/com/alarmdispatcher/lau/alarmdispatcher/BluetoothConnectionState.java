package com.alarmdispatcher.lau.alarmdispatcher;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by lau on 3/3/16.
 */
public class BluetoothConnectionState implements Serializable {

    public static final long FIVE_MIN = 1000L * 60 * 5;
    private boolean bluetoothAvailable;
    private boolean bluetoothOn;
    private boolean alarmClockPresent;
    private boolean connected;
    private Date lastTimeUpdated;

    public boolean isBluetoothAvailable() {
        return bluetoothAvailable;
    }

    public void setBluetoothAvailable(boolean bluetoothAvailable) {
        this.bluetoothAvailable = bluetoothAvailable;
    }

    public boolean isBluetoothOn() {
        return bluetoothOn;
    }

    public void setBluetoothOn(boolean bluetoothOn) {
        this.bluetoothOn = bluetoothOn;
    }

    public boolean isAlarmClockPresent() {
        return alarmClockPresent;
    }

    public void setAlarmClockPresent(boolean alarmClockPresent) {
        this.alarmClockPresent = alarmClockPresent;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Date getLastTimeUpdated() {
        return lastTimeUpdated;
    }

    public void setLastTimeUpdated(Date lastTimeUpdated) {
        this.lastTimeUpdated = lastTimeUpdated;
    }

    public boolean isAlive() {
        if (lastTimeUpdated == null) {
            return false;
        }
        Date now = new Date();
        long diff = now.getTime() - lastTimeUpdated.getTime();
        return diff < FIVE_MIN;
    }
}
