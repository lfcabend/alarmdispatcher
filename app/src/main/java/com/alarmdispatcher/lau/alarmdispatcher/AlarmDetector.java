package com.alarmdispatcher.lau.alarmdispatcher;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.alarmdispatcher.lau.alarmdispatcher.AlarmBluetoothService.OnChangeHandler;
import com.alarmdispatcher.lau.alarmdispatcher.commands.AlarmCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.SetTimeCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.SnoozeAlarmCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.SwitchHotness;
import com.alarmdispatcher.lau.alarmdispatcher.commands.TurnOffAlarmCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.TurnOnAlarmCommand;
import com.alarmdispatcher.lau.alarmdispatcher.commands.UpdateBackLight;
import com.alarmdispatcher.lau.alarmdispatcher.commands.UpdateDataCommand;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alarmdispatcher.lau.alarmdispatcher.Constants.*;

public class AlarmDetector extends AppCompatActivity implements OnChangeHandler {

    public final static String TAG = AlarmDetector.class.getName();

    private AlarmBluetoothService bluetoothService;
    private boolean mBound = false;


    private List<AlarmCommand> commands2Send = Arrays.asList(new SetTimeCommand(),
            new SnoozeAlarmCommand(), new TurnOffAlarmCommand(), new TurnOnAlarmCommand(),
            new SwitchHotness(true), new SwitchHotness(false),
            new UpdateBackLight(true), new UpdateBackLight(false));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_detector);

    }

    @Override
    protected void onStart() {
        super.onStart();

        initializeState();
        initializeSpinner();


        Intent intent = new Intent(this, AlarmBluetoothService.class);
        startService(intent);

        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            AlarmBluetoothService.LocalBinder binder = (AlarmBluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            mBound = true;
            bluetoothService.addOnChangeHandler(AlarmDetector.this);
            bluetoothService.updateBlueToothState(null);
            initializeState();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            bluetoothService.removeOnChangeHandler(AlarmDetector.this);
        }
    };

    private void initializeState() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        AlarmClockState alarmClockState;
        if (settings.contains(ALARM_STATE)) {
            String jsonString = settings.getString(ALARM_STATE, null);
            Gson gson = new Gson();
            alarmClockState = gson.fromJson(jsonString, AlarmClockState.class);
        } else {
            alarmClockState = new AlarmClockState();
        }

        CheckBox armedCheckBox = (CheckBox) findViewById(R.id.armedCheckBox);
        armedCheckBox.setChecked(alarmClockState.isHot());
        CheckBox ringingCheckbox = (CheckBox) findViewById(R.id.ringingCheckBox);
        ringingCheckbox.setChecked(alarmClockState.isRinging());
        CheckBox displayOnCheckBox = (CheckBox) findViewById(R.id.displayOnCheckBox);
        displayOnCheckBox.setChecked(alarmClockState.isDisplayOn());
        CheckBox snoozedCheckBox = (CheckBox) findViewById(R.id.snoozedCheckBox);
        snoozedCheckBox.setChecked(alarmClockState.isSnoozed());

        BluetoothConnectionState bluetoothConnectionState;
        if (settings.contains(Constants.CONNECTION_STATE)) {
            String jsonString = settings.getString(Constants.CONNECTION_STATE, null);
            Gson gson = new Gson();
            bluetoothConnectionState = gson.fromJson(jsonString, BluetoothConnectionState.class);
        } else {
            bluetoothConnectionState = new BluetoothConnectionState();
        }

        CheckBox bluetoothAvailableCheckbox = (CheckBox) findViewById(R.id.bluetoothAvailableCheckBox);
        bluetoothAvailableCheckbox.setChecked(bluetoothConnectionState.isBluetoothAvailable());
        CheckBox bluetoothOnCheckbox = (CheckBox) findViewById(R.id.bluetoothOnCheckBox);
        bluetoothOnCheckbox.setChecked(bluetoothConnectionState.isBluetoothOn());
        CheckBox alarmPresentCheckbox = (CheckBox) findViewById(R.id.alarmPresentCheckBox);
        alarmPresentCheckbox.setChecked(bluetoothConnectionState.isAlarmClockPresent());
        CheckBox connectedCheckbox = (CheckBox) findViewById(R.id.connectedCheckBox);
        connectedCheckbox.setChecked(bluetoothConnectionState.isConnected());
        CheckBox heartBeatCheckBox = (CheckBox) findViewById(R.id.heartBeatCheckBox);
        heartBeatCheckBox.setChecked(bluetoothConnectionState.isAlive());

        Switch switchButton = (Switch) findViewById(R.id.switch1);
        switchButton.setChecked(alarmClockState.isHot());

    }

    private void initializeSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<AlarmCommand> adapter = new ArrayAdapter<AlarmCommand>(
                this, android.R.layout.simple_list_item_1, commands2Send);


        spinner.setAdapter(adapter);
    }

    public void sendCommand(View view) {
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        AlarmCommand selectedItem = (AlarmCommand) spinner.getSelectedItem();
        Log.i(TAG, "Selected item: " + selectedItem);

        bluetoothService.sendCommand(selectedItem);
    }

    public void alarmSwitchClicked(View view) {
        Switch switchButton = (Switch) findViewById(R.id.switch1);

        boolean enabled = switchButton.isChecked();


        Log.i(TAG, "Switched alarm " + (enabled ? "on" : "off"));
        AlarmCommand alarmCommand = new SwitchHotness(enabled);

        storeAlarmSwitch(enabled);

        bluetoothService.sendCommand(alarmCommand);

    }

    private void storeAlarmSwitch(boolean enabled) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        settings.edit().putBoolean(SWITCH_STATE, enabled);
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

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
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
    public void onChange() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                initializeState();
            }
        });
    }
}
