package com.alarmdispatcher.lau.alarmdispatcher;

import java.io.Serializable;

/**
 * Created by lau on 2/19/16.
 */
public class AlarmClockState implements Serializable {

    private boolean ringing;
    private boolean snoozed;
    private boolean hot;
    private boolean displayOn;

    public boolean isRinging() {
        return ringing;
    }

    public void setRinging(boolean ringing) {
        this.ringing = ringing;
    }

    public boolean isSnoozed() {
        return snoozed;
    }

    public void setSnoozed(boolean snoozed) {
        this.snoozed = snoozed;
    }

    public boolean isHot() {
        return hot;
    }

    public void setHot(boolean hot) {
        this.hot = hot;
    }

    public boolean isDisplayOn() {
        return displayOn;
    }

    public void setDisplayOn(boolean displayOn) {
        this.displayOn = displayOn;
    }

    @Override
    public String toString() {
        return "AlarmClockState{" +
                "ringing=" + ringing +
                ", snoozed=" + snoozed +
                ", hot=" + hot +
                ", displayOn=" + displayOn +
                '}';
    }
}
