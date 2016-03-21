package com.alarmdispatcher.lau.alarmdispatcher.commands;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lau on 2/19/16.
 */
public abstract class AlarmCommand implements Serializable {


    private static final Map<String, Class<? extends AlarmCommand>> commands = new HashMap<String, Class<? extends AlarmCommand>>() {{
        put(SetTimeCommand.INS, SetTimeCommand.class);
        put(SnoozeAlarmCommand.INS, SnoozeAlarmCommand.class);
        put(TurnOffAlarmCommand.INS, TurnOffAlarmCommand.class);
        put(TurnOnAlarmCommand.INS, TurnOnAlarmCommand.class);
        put(UpdateDataCommand.INS, UpdateDataCommand.class);
        put(UpdateBackLight.INS, UpdateBackLight.class);
    }};



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

    public static AlarmCommand parseACommand(String message) {
        if (message.length() < 1) {
            throw new IllegalArgumentException("Invalid length, needs at least 1 for instruction: " + message);
        }

        Class<? extends AlarmCommand> clazz = commands.get(String.valueOf(message.charAt(0)));
        if (clazz == null) {
            throw new IllegalArgumentException("Unsupported instruction: " + message);
        }

        AlarmCommand alarmCommand;
        try {
            alarmCommand = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return alarmCommand.parseCommand(message);
    }


}
