package com.alarmdispatcher.lau.alarmdispatcher.commands;

/**
 * Created by lau on 2/20/16.
 */
public class SwitchHotness extends AlarmCommand {

    public static final String INS = "A";

    private boolean on;

    public SwitchHotness(boolean on) {
        this.on = on;
    }

    public SwitchHotness() {
    }

    @Override
    public String getInstruction() {
        return INS;
    }

    public String getCommandMessage() {
        return on ? "1" : "0";
    }

    public String toString() {
        return on ? "Switch on" : "Switch off";
    }

}
