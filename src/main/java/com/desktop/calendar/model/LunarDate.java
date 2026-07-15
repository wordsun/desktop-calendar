package com.desktop.calendar.model;

/**
 * 农历日期模型
 * 封装农历的天干地支年月日、生肖等信息
 */
public class LunarDate {

    private int year;
    private int month;
    private int day;
    private String ganZhiYear;
    private String zodiac;
    private String monthName;
    private String dayName;
    private boolean leapMonth;
    private String solarTerm;
    private String festival;

    public LunarDate() {}

    // Getters & Setters
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public String getGanZhiYear() { return ganZhiYear; }
    public void setGanZhiYear(String ganZhiYear) { this.ganZhiYear = ganZhiYear; }
    public String getZodiac() { return zodiac; }
    public void setZodiac(String zodiac) { this.zodiac = zodiac; }
    public String getMonthName() { return monthName; }
    public void setMonthName(String monthName) { this.monthName = monthName; }
    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }
    public boolean isLeapMonth() { return leapMonth; }
    public void setLeapMonth(boolean leapMonth) { this.leapMonth = leapMonth; }
    public String getSolarTerm() { return solarTerm; }
    public void setSolarTerm(String solarTerm) { this.solarTerm = solarTerm; }
    public String getFestival() { return festival; }
    public void setFestival(String festival) { this.festival = festival; }

    /**
     * 获取简短显示文本
     */
    public String getShortDisplay() {
        if (festival != null) {
            return festival;
        }
        if (solarTerm != null) {
            return solarTerm;
        }
        return dayName;
    }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final LunarDate date = new LunarDate();
        public Builder year(int year) { date.year = year; return this; }
        public Builder month(int month) { date.month = month; return this; }
        public Builder day(int day) { date.day = day; return this; }
        public Builder ganZhiYear(String ganZhiYear) { date.ganZhiYear = ganZhiYear; return this; }
        public Builder zodiac(String zodiac) { date.zodiac = zodiac; return this; }
        public Builder monthName(String monthName) { date.monthName = monthName; return this; }
        public Builder dayName(String dayName) { date.dayName = dayName; return this; }
        public Builder leapMonth(boolean leapMonth) { date.leapMonth = leapMonth; return this; }
        public Builder solarTerm(String solarTerm) { date.solarTerm = solarTerm; return this; }
        public Builder festival(String festival) { date.festival = festival; return this; }
        public LunarDate build() { return date; }
    }
}
