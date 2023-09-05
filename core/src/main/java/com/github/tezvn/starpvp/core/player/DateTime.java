package com.github.tezvn.starpvp.core.player;

import com.github.tezvn.starpvp.core.utils.time.TimeUtils;

public class DateTime {

    private int day;

    private int month;

    private int year;

    public DateTime(long millis) {
        String[] split = TimeUtils.format(millis).split(" ")[0].split("/");
        if(split.length != 3) throw new IllegalArgumentException();
        this.day = Integer.parseInt(split[0]);
        this.month = Integer.parseInt(split[1]);
        this.year = Integer.parseInt(split[2]);
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public boolean compare(DateTime other) {
        if(getYear() > other.getYear()) return true;
        if(getMonth() > other.getMonth()) return true;
        return getDay() > other.getDay();
    }

}
