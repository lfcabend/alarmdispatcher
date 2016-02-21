package com.alarmdispatcher.lau.alarmdispatcher.commands;

/**
 * Created by lau on 2/19/16.
 */
public class TurnOnAlarmCommand extends AlarmCommand {

    public static final String INS = "F";

    @Override
    public String getInstruction() {
        return INS;
    }

    @Override
    public String toString() {
        return "Turn on";
    }

}
