package com.alarmdispatcher.lau.alarmdispatcher.commands;

import com.alarmdispatcher.lau.alarmdispatcher.AlarmClockState;

/**
 * Created by lau on 2/20/16.
 */
public class UpdateDataCommand extends AlarmCommand {

    public static final String INS = "U";
    public static final int COMMAND_LENGTH = 5;
    public static final int RINGING_POS = 1;
    public static final int SNOOZED_POS = 2;
    public static final int HOT_POS = 3;
    public static final int DISPLAY_ON_POS = 4;
    public static final char NOT_SET = '0';
    private AlarmClockState alarmClockState;

    @Override
    public String getInstruction() {
        return INS;
    }

    @Override
    public UpdateDataCommand parseCommand(String commandString) {
        if (!commandString.startsWith(INS)) {
            throw new IllegalArgumentException(
                    "Invalid commands String, instruction is not U:" + commandString);
        }

        if (commandString.length() != COMMAND_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid commands length, length is not 4:" + commandString);
        }

        boolean ringing = commandString.charAt(RINGING_POS) != NOT_SET;
        boolean snoozed = commandString.charAt(SNOOZED_POS) != NOT_SET;
        boolean hot = commandString.charAt(HOT_POS) != NOT_SET;
        boolean displayOn = commandString.charAt(DISPLAY_ON_POS) != NOT_SET;

        alarmClockState = new AlarmClockState();
        alarmClockState.setHot(hot);
        alarmClockState.setRinging(ringing);
        alarmClockState.setSnoozed(snoozed);
        alarmClockState.setDisplayOn(displayOn);


        return this;
    }

    public AlarmClockState getAlarmClockState() {
        return alarmClockState;
    }

    @Override
    public String toString() {
        return alarmClockState.toString();
    }
}
