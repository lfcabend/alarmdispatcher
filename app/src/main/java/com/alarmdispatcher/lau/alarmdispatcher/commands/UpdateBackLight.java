package com.alarmdispatcher.lau.alarmdispatcher.commands;

/**
 * Created by lau on 2/21/16.
 */
public class UpdateBackLight extends AlarmCommand {

    public static final String INS = "D";

    private boolean on;

    public UpdateBackLight(boolean on) {
        this.on = on;
    }

    public UpdateBackLight() {
    }

    @Override
    public String getInstruction() {
        return INS;
    }

    public String getCommandMessage() {
        return on ? "1" : "0";
    }

    public String toString() {
        return on ? "BackLight on" : "BackLight off";
    }

}
