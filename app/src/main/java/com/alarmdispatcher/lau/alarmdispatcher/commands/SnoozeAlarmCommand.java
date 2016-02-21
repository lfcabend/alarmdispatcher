package com.alarmdispatcher.lau.alarmdispatcher.commands;

/**
 * Created by lau on 2/19/16.
 */
public class SnoozeAlarmCommand extends AlarmCommand {

    public static final String INS = "Z";


    @Override
    public String getInstruction() {
        return INS;
    }

    @Override
    public String toString() {
        return "Snooze";
    }

}
