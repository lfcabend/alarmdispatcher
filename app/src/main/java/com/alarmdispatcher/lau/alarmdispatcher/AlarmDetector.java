package com.alarmdispatcher.lau.alarmdispatcher;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class AlarmDetector extends AppCompatActivity
        implements BluetoothHandler.DiscoveredDeviceCallBack, BluetoothHandler.ConnectionCallbackHandler {

    public final static String TAG = AlarmDetector.class.getName();

    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

    private BluetoothHandler bluetoothHandler;
    private List<String> messages = new ArrayList<>();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("intent: " + intent);
            String action = intent.getAction();
            String alarmState = null;
            if (action.equals(ALARM_ALERT_ACTION)) {
                alarmState = "Ringing";
            } else if (action.equals(ALARM_DISMISS_ACTION)) {
                alarmState = "Dismissed";
            } else if (action.equals(ALARM_SNOOZE_ACTION)) {
                alarmState = "Snoozed";
            } else if (action.equals(ALARM_DONE_ACTION)) {
                alarmState = "Done";
            }
            if (alarmState != null) {
                UpdateAlarmStateText(alarmState);
            }
        }
    };
    private ArrayAdapter listAdapter;

    private void UpdateAlarmStateText(String alarmState) {
        TextView view = (TextView) findViewById(R.id.textView2);
        view.setText(alarmState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_detector);

        IntentFilter filter = new IntentFilter(ALARM_ALERT_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DONE_ACTION);
        registerReceiver(mReceiver, filter);

        initializeList();
        TextView statusField = (TextView) findViewById(R.id.textView4);

        bluetoothHandler = new BluetoothHandler();
        try {
            bluetoothHandler.initialize();

            if (bluetoothHandler.getDeviceNames().contains(BluetoothHandler.DEVICE_NAME)) {
                Log.d(TAG, "found clock");
                statusField.setText("Clock found");
                String address = bluetoothHandler.getAllDevices().get(BluetoothHandler.DEVICE_NAME);
                bluetoothHandler.connect(address, this);
            } else {
                statusField.setText("Clock is not in range");
            }

        } catch (BluetoothException e) {
            e.printStackTrace();
        }
    }

    private void initializeList() {
        ListView list = (ListView) findViewById(R.id.listView);
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, messages);
        list.setAdapter(listAdapter);
    }

    private void updateStatus(String obj) {
        TextView statusField = (TextView) findViewById(R.id.textView4);
        statusField.setText(obj);
    }

    private void handleDiscoveredDevice(BluetoothDevice obj) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_alarm_detector, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void deviceDiscovered(BluetoothDevice device) {
    }

    @Override
    public void connectionSuccessful() {
        Log.d(TAG, "Going to dispatch successfull connection");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus("Connection successful");
            }
        });
    }

    @Override
    public void connectionFailed(final BluetoothException e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus("Connection failed; " + e.getMessage());
            }
        });
    }

    @Override
    public void messageRead(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messages.add(message);
                listAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void errorOccurred(final BluetoothException e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatus("error occurred; " + e.getMessage());
            }
        });
    }

    @Override
    public void disconnected() {

    }
}
