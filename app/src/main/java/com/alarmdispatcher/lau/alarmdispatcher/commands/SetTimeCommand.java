package com.alarmdispatcher.lau.alarmdispatcher.commands;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by lau on 2/19/16.
 */
public class SetTimeCommand extends AlarmCommand {

    public static final String INS = "T";

    @Override
    public String getInstruction() {
        return INS;
    }

    @Override
    public String getCommandMessage() {
        Date date = new Date();
        // TssmmhhWDDMMYYYY aka set time
        // TssmmhhWDDMMYYYY aka set time
        ////T355720619112011
        //  S130101 21022016
        //  S562201 11022016
        //  S3924011
        Calendar calendar = Calendar.getInstance();
        String weekOfDay = String.valueOf(calendar.get(Calendar.DAY_OF_WEEK));
            SimpleDateFormat df = new SimpleDateFormat("ssmmHHddMMyyyy");
        String format = df.format(date);
        return format.substring(0, 6) + weekOfDay + format.substring(6, format.length());
    }

    @Override
    public String toString() {
        return "Set Time";
    }
}
