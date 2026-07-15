package com.desktop.calendar.model;

import java.time.LocalDate;

/**
 * 日程事件模型
 * 存储用户自定义的日程安排
 */
public class CalendarEvent {

    private String id;
    private String title;
    private String description;
    private LocalDate date;
    private boolean allDay;
    private String startTime;
    private String endTime;
    private String color;
    private boolean recurring;
    private String repeatType;

    public CalendarEvent() {}

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CalendarEvent event = new CalendarEvent();
        public Builder id(String id) { event.id = id; return this; }
        public Builder title(String title) { event.title = title; return this; }
        public Builder description(String description) { event.description = description; return this; }
        public Builder date(LocalDate date) { event.date = date; return this; }
        public Builder allDay(boolean allDay) { event.allDay = allDay; return this; }
        public Builder startTime(String startTime) { event.startTime = startTime; return this; }
        public Builder endTime(String endTime) { event.endTime = endTime; return this; }
        public Builder color(String color) { event.color = color; return this; }
        public Builder recurring(boolean recurring) { event.recurring = recurring; return this; }
        public Builder repeatType(String repeatType) { event.repeatType = repeatType; return this; }
        public CalendarEvent build() { return event; }
    }
}
