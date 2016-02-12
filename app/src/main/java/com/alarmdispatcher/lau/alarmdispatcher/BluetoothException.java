package com.alarmdispatcher.lau.alarmdispatcher;

/**
 * Created by Lau on 2/8/2016.
 */
public class BluetoothException extends Exception {
    public BluetoothException() {
    }

    public BluetoothException(String detailMessage) {
        super(detailMessage);
    }

    public BluetoothException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BluetoothException(Throwable throwable) {
        super(throwable);
    }
}
