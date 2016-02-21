package com.alarmdispatcher.lau.alarmdispatcher.commands;

/**
 * Created by lau on 2/19/16.
 */
public abstract class AlarmCommand {

    public abstract String getInstruction();

    public String getCommandMessage() {
        return "";
    }

    public <T extends AlarmCommand> T parseCommand(String message) {
        throw new UnsupportedOperationException("Not parsable");
    }

    public String getMessage() {
        return new StringBuilder(getInstruction()).
                append(getCommandMessage()).toString();
    }

}
