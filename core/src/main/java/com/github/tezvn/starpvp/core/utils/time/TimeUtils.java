package com.github.tezvn.starpvp.core.utils.time;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class TimeUtils {

    private long rawOldTime;

    private long rawNewTime;

    private String numFormat = "[0-9]+[\\.]?[0-9]*";

    private static final String DEFAULT_TIME_ZONE = "GMT+7";

    private static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";

    private TimeUtils(long rawOldTime) {
        if (rawOldTime < 1)
            throw new IllegalArgumentException("Old time must equals or greater than 1");
        setNewTime(rawOldTime);
        setOldTime(rawOldTime);
    }

    private TimeUtils(long rawOldTime, long rawNewTime) {
        setOldTime(rawOldTime);
        setNewTime(rawNewTime);
    }

    public static TimeUtils newInstance() {
        return new TimeUtils(System.currentTimeMillis());
    }

    public static TimeUtils of(long rawOldTime) {
        return new TimeUtils(rawOldTime);
    }

    public static TimeUtils of(long rawOldTime, long rawNewTime) {
        return new TimeUtils(rawOldTime, rawNewTime);
    }

    public long getOldTime() {
        return getRawOldTime() + getTimeOffset();
    }

    public long getRawOldTime() {
        return this.rawOldTime;
    }

    public TimeUtils setOldTime(long rawOldTime) {
        this.rawOldTime = rawOldTime;
        return this;
    }

    public long getNewTime() {
        return getRawNewTime() + getTimeOffset();
    }

    public long getRawNewTime() {
        return this.rawNewTime;
    }

    public int getTimeOffset() {
        int defaultOffset = TimeZone.getDefault().getRawOffset();
        return defaultOffset < 0 ? Math.abs(TimeZone.getDefault().getRawOffset()) + getTimeZone().getRawOffset()
                : Math.abs(getTimeZone().getRawOffset() - defaultOffset);
    }

    public TimeZone getTimeZone() {
        return TimeZone.getTimeZone(DEFAULT_TIME_ZONE);
    }

    public long getDuration() {
        return this.rawNewTime - this.rawOldTime;
    }

    public TimeUtils setNewTime(long rawNewTime) {
        this.rawNewTime = rawNewTime;
        return this;
    }

    public String format() {
        return format(DEFAULT_DATE_FORMAT);
    }

    public String format(String pattern) {
        return new SimpleDateFormat(pattern).format(this.getNewTime());
    }

    public TimeUtils add(TimeUnits unit, int amount) {
        return modifyTime(unit, amount, 0);
    }

    public TimeUtils add(long millis) {
        this.rawNewTime += millis;
        return this;
    }

    public TimeUtils subtract(TimeUnits unit, int amount) {
        return modifyTime(unit, amount, 1);
    }

    public TimeUtils subtract(long millis) {
        this.rawNewTime = Math.max(this.rawNewTime - millis, this.rawOldTime);
        return this;
    }

    private TimeUtils modifyTime(TimeUnits unit, int amount, int operation) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.rawNewTime),
                TimeZone.getTimeZone(DEFAULT_TIME_ZONE).toZoneId());
        switch (unit) {
            case DAY:
                zdt = zdt.plusDays(amount);
                break;
            case HOUR:
                zdt = zdt.plusHours(amount);
                break;
            case MINUTE:
                zdt = zdt.plusMinutes(amount);
                break;
            case MONTH:
                zdt = zdt.plusMonths(amount);
                break;
            case SECOND:
                zdt = zdt.plusSeconds(amount);
                break;
            case WEEK:
                zdt = zdt.plusWeeks(amount);
                break;
            case YEAR:
                zdt = zdt.plusYears(amount);
                break;
        }
        this.rawNewTime = zdt.toInstant().toEpochMilli();
        return this;
    }

    public TimeUtils add(String format) {
        return modifyDuration(format, 0);
    }

    public TimeUtils subtract(String format) {
        return modifyDuration(format, 1);
    }

    private TimeUtils modifyDuration(String format, int math) {
        StringBuilder numbers = new StringBuilder();
        StringBuilder units = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            String str = String.valueOf(format.charAt(i));
            if (isNumber(str)) {
                numbers.append(str);
            } else {
                units.append(str).append(",");
                numbers.append(",");
            }
        }
        if (units.length() < 1)
            return this;
        String[] splitNumbers = numbers.toString().split(",");
        String[] splitUnits = units.toString().split(",");
        for (int i = 0; i < splitNumbers.length; i++) {
            Optional<TimeUnits> opt = TimeUnits.parse(splitUnits[i]);
            if (!opt.isPresent())
                continue;
            TimeUnits unit = opt.get();
            int amount = Math.abs(Integer.parseInt(splitNumbers[i]));
            if (math == 0)
                this.add(unit, amount);
            else if (math == 1)
                this.subtract(unit, amount);
        }
        return this;
    }

    public String getFullDuration() {
        LocalDateTime old = LocalDateTime.ofInstant(Instant.ofEpochMilli(getOldTime()), getTimeZone().toZoneId());
        LocalDateTime current = LocalDateTime.ofInstant(Instant.ofEpochMilli(getNewTime()), getTimeZone().toZoneId());
        int seconds = (int) Duration.between(old, current).getSeconds();
        int years = seconds / 31104000;
        int months = seconds / (2419200 + 172800);
        int weeks = seconds / 604800;
        int days = (seconds % 604800) / 86400;
        int hours = ((seconds % 604800) % 86400) / 3600;
        int mins = (((seconds % 604800) % 86400) % 3600) / 60;
        int secs = (((seconds % 604800) % 86400) % 3600) % 60;
        String yearsFormat = years > 0 ? years + "y" + " " : "";
        String monthsFormat = months > 0 ? months + "M" + " " : "";
        String weeksFormat = weeks > 0 ? weeks + "w" + " " : "";
        String daysFormat = (days > 0 ? days + "d" + " " : "");
        String hoursFormat = (hours > 0 ? hours + "h" + " " : "");
        String minsFormat = (mins > 0 ? mins + "m" + " " : "");
        String secsFormat = (secs > 0 ? secs + "s" + " " : "");
        return yearsFormat + monthsFormat + weeksFormat + daysFormat + hoursFormat + minsFormat + secsFormat;
    }

    public String getShortDuration() {
        LocalDateTime old = LocalDateTime.ofInstant(Instant.ofEpochMilli(getOldTime()), getTimeZone().toZoneId());
        LocalDateTime current = LocalDateTime.ofInstant(Instant.ofEpochMilli(getNewTime()), getTimeZone().toZoneId());
        int seconds = (int) Duration.between(old, current).getSeconds();
        int years = seconds / 31104000 % 365;
        int months = seconds / (2419200 + 172800) % 12;
        int weeks = seconds / 604800;
        int days = (seconds % 604800) / 86400;
        int hours = ((seconds % 604800) % 86400) / 3600;
        int mins = (((seconds % 604800) % 86400) % 3600) / 60;
        int secs = (((seconds % 604800) % 86400) % 3600) % 60;
        String yearsFormat = years > 0 ? years + "y" + " " : "";
        String monthsFormat = months > 0 ? months + "M" + " " : "";
        String weeksFormat = weeks > 0 ? weeks + "w" + " " : "";
        String daysFormat = (days > 0 ? days + "d" + " " : "");
        String hoursFormat = (hours > 0 ? hours + "h" + " " : "");
        String minsFormat = (mins > 0 ? mins + "m" + " " : "");
        String secsFormat = (secs > 0 ? secs + "s" + " " : "");

        if (years > 0)
            return yearsFormat + monthsFormat;
        else if (months > 0)
            return monthsFormat + weeksFormat;
        else if (weeks > 0)
            return weeksFormat + daysFormat;
        else if (days > 0)
            return daysFormat + hoursFormat;
        else if (hours > 0)
            return hoursFormat + minsFormat;
        else if (mins > 0)
            return minsFormat + secsFormat;
        else if (secs > 0)
            return secsFormat;
        return "";
    }

    @Deprecated
    public static Date toDate(String format) {
        if (format == null || format.isEmpty())
            throw new IllegalArgumentException("format cannot be null!");
        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        try {
            return sdf.parse(format);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static long toTimeMillis(String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
        return ZonedDateTime.parse(format, dtf).toInstant().toEpochMilli();
    }

    public static String format(long millis) {
        return new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(millis);
    }

    private boolean isNumber(String str) {
        return Pattern.matches(this.numFormat, str);
    }

}
