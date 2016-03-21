package com.alarmdispatcher.lau.alarmdispatcher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.alarmdispatcher.lau.alarmdispatcher.commands.AlarmCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.SnoozeAlarmCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.TurnOffAlarmCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.TurnOnAlarmCommand;

import app.akexorcist.bluetotohspp.library.BluetoothService;

/**
 * Created by lau on 3/5/16.
 */
public class AlarmReceiver extends BroadcastReceiver {

    public final static String TAG = AlarmReceiver.class.getName();

    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

    // Samsung
    public static final String SAMSUNG_ALARM_ALERT_ACTION = "com.samsung.sec.android.clockpackage.alarm.ALARM_ALERT";

    @Override
    public void onReceive(Context context, Intent intent) {


        String action = intent.getAction();
        Log.i(TAG, "Alarm receiver with action " + action);

        AlarmCommand alarmCommand = null;
        if (action.equals(ALARM_ALERT_ACTION) ||
                action.equals(SAMSUNG_ALARM_ALERT_ACTION )) {
            alarmCommand = new TurnOnAlarmCommand();
        } else if (action.equals(ALARM_DISMISS_ACTION)) {
            alarmCommand = new TurnOffAlarmCommand();
        } else if (action.equals(ALARM_SNOOZE_ACTION)) {
            alarmCommand = new SnoozeAlarmCommand();
        } else if (action.equals(ALARM_DONE_ACTION)) {
            alarmCommand = new TurnOffAlarmCommand();
        }
        Intent service = new Intent(context, AlarmBluetoothService.class);

        service = service.putExtra(AlarmBluetoothService.COMMAND_BUNDLE_KEY, alarmCommand);
        // Start the service, keeping the device awake while it is launching.
        Log.i(TAG, "Starting service @ " + SystemClock.elapsedRealtime());
        Log.i(TAG, "Sending alarm command: " + alarmCommand);
//        startWakefulService(context, service);
        context.startService(service);
    }

}