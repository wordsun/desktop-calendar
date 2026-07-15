package com.desktop.calendar.model;

import java.util.List;
import java.util.Map;

/**
 * 节假日配置模型
 * 从JSON文件加载的节假日和调休配置
 */
public class HolidayConfig {

    /** 日期类型枚举 — 渲染层根据 type 直接映射颜色 */
    public enum DayType {
        NORMAL_WORKDAY,   // 普通工作日（周一~周五，未被调整）
        NORMAL_WEEKEND,   // 普通周末（周六/周日，未被调整）
        HOLIDAY,          // 法定假日（国家发布）
        ADJUSTED_WORKDAY  // 调休工作日（周末被调整为上班）
    }

    private int year;
    private List<Holiday> holidays;
    private Map<String, String> workdays;

    public HolidayConfig() {}

    // Getters & Setters
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public List<Holiday> getHolidays() { return holidays; }
    public void setHolidays(List<Holiday> holidays) { this.holidays = holidays; }
    public Map<String, String> getWorkdays() { return workdays; }
    public void setWorkdays(Map<String, String> workdays) { this.workdays = workdays; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final HolidayConfig config = new HolidayConfig();
        public Builder year(int year) { config.year = year; return this; }
        public Builder holidays(List<Holiday> holidays) { config.holidays = holidays; return this; }
        public Builder workdays(Map<String, String> workdays) { config.workdays = workdays; return this; }
        public HolidayConfig build() { return config; }
    }

    /**
     * 单个假日定义
     */
    public static class Holiday {
        private String name;
        private String date;
        private boolean adjusted;
        private String type;

        public Holiday() {}

        // Getters & Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public boolean isAdjusted() { return adjusted; }
        public void setAdjusted(boolean adjusted) { this.adjusted = adjusted; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        // Builder
        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private final Holiday holiday = new Holiday();
            public Builder name(String name) { holiday.name = name; return this; }
            public Builder date(String date) { holiday.date = date; return this; }
            public Builder adjusted(boolean adjusted) { holiday.adjusted = adjusted; return this; }
            public Builder type(String type) { holiday.type = type; return this; }
            public Holiday build() { return holiday; }
        }
    }
}
