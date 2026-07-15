package com.desktop.calendar.service;

import com.desktop.calendar.model.CalendarEvent;
import com.desktop.calendar.model.HolidayConfig;
import com.desktop.calendar.util.DateUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日历核心服务
 * 负责日程管理、节假日查询、月视图数据生成
 */
public class CalendarService {

    private final StorageService storageService;
    private HolidayConfig holidayConfig;

    public CalendarService(StorageService storageService) {
        this.storageService = storageService;
        this.holidayConfig = storageService.loadHolidayConfig();
    }

    /**
     * 获取指定日期的所有事件（日程+节日）
     */
    public List<CalendarEvent> getEventsForDate(LocalDate date) {
        List<CalendarEvent> events = storageService.loadEvents();
        return events.stream()
                .filter(e -> e.getDate().equals(date))
                .collect(Collectors.toList());
    }

    /**
     * 获取某月所有有事件的日期
     */
    public Set<LocalDate> getEventDatesInMonth(YearMonth yearMonth) {
        List<CalendarEvent> events = storageService.loadEvents();
        return events.stream()
                .filter(e -> {
                    LocalDate d = e.getDate();
                    return d.getYear() == yearMonth.getYear()
                            && d.getMonthValue() == yearMonth.getMonthValue();
                })
                .map(CalendarEvent::getDate)
                .collect(Collectors.toSet());
    }

    /**
     * 添加日程事件，同步保存 MD 文件
     */
    public void addEvent(CalendarEvent event) {
        List<CalendarEvent> events = storageService.loadEvents();
        events.add(event);
        storageService.saveEvents(events);
        // 同步更新 remark MD 文件
        syncRemarkFile(event.getDate());
    }

    /**
     * 删除日程事件，同步更新 MD 文件
     */
    public void removeEvent(String eventId) {
        List<CalendarEvent> events = storageService.loadEvents();
        // 获取要删除的事件日期（用于后续同步）
        CalendarEvent toRemove = events.stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst().orElse(null);
        events.removeIf(e -> e.getId().equals(eventId));
        storageService.saveEvents(events);
        // 同步更新 remark MD 文件
        if (toRemove != null) {
            syncRemarkFile(toRemove.getDate());
        }
    }

    /**
     * 同步指定日期的待办到 remark MD 文件
     */
    private void syncRemarkFile(LocalDate date) {
        List<CalendarEvent> dayEvents = getEventsForDate(date);
        RemarkService.saveDayEvents(date, dayEvents);
    }

    /**
     * 查询某日是否为节假日
     */
    public Optional<HolidayConfig.Holiday> getHoliday(LocalDate date) {
        if (holidayConfig == null || holidayConfig.getHolidays() == null) {
            return Optional.empty();
        }
        String mmdd = date.format(DateUtil.SHORT_DATE_FMT);
        return holidayConfig.getHolidays().stream()
                .filter(h -> h.getDate().equals(mmdd))
                .findFirst();
    }

    /**
     * 查询某日是否为调休工作日
     */
    public boolean isWorkday(LocalDate date) {
        if (holidayConfig == null || holidayConfig.getWorkdays() == null) {
            return false;
        }
        return holidayConfig.getWorkdays().containsKey(DateUtil.formatDate(date));
    }

    /**
     * 刷新节假日配置
     */
    public void refreshHolidayConfig() {
        this.holidayConfig = storageService.loadHolidayConfig();
    }
}
