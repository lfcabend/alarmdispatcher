package com.alarmdispatcher.lau.alarmdispatcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;


/**
 * Created by lau on 3/2/16.
 */
public class BootReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
// This is the Intent to deliver to our service.
        Intent service = new Intent(context, AlarmBluetoothService.class);

        // Start the service, keeping the device awake while it is launching.
        Log.i("BootReceiver", "Starting service @ " + SystemClock.elapsedRealtime());
        startWakefulService(context, service);
    }
}
