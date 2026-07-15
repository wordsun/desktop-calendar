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
     * 清空所有日程事件，同步清理 remark MD 文件
     * @return 被清除的事件总数
     */
    public int clearAllEvents() {
        List<CalendarEvent> events = storageService.loadEvents();
        int count = events.size();
        if (count > 0) {
            // 清除每个日期的 remark MD 文件
            for (CalendarEvent event : events) {
                RemarkService.saveDayEvents(event.getDate(), new ArrayList<>());
            }
            storageService.saveEvents(new ArrayList<>());
        }
        return count;
    }

    /**
     * 同步指定日期的待办到 remark MD 文件
     */
    private void syncRemarkFile(LocalDate date) {
        List<CalendarEvent> dayEvents = getEventsForDate(date);
        RemarkService.saveDayEvents(date, dayEvents);
    }

    /**
     * 获取指定日期的 DayType（统一判断逻辑，渲染层直接映射颜色）
     * 优先级：法定假日 > 调休工作日 > 普通周末 > 普通工作日
     * 仅当日期年份与配置年份匹配时才返回假日/调休数据
     */
    public HolidayConfig.DayType getDayType(LocalDate date) {
        String mmdd = date.format(DateUtil.SHORT_DATE_FMT);
        String fullDate = DateUtil.formatDate(date);
        int year = date.getYear();

        // 仅当配置年份与查询日期年份一致时才生效（防止跨年误匹配）
        if (holidayConfig != null && holidayConfig.getYear() == year) {
            // 1. 检查是否为法定假日（国家发布）
            if (holidayConfig.getHolidays() != null) {
                for (HolidayConfig.Holiday h : holidayConfig.getHolidays()) {
                    if (h.getDate().equals(mmdd)) {
                        return HolidayConfig.DayType.HOLIDAY;
                    }
                }
            }

            // 2. 检查是否为调休工作日（周末补班）
            if (holidayConfig.getWorkdays() != null
                    && holidayConfig.getWorkdays().containsKey(fullDate)) {
                return HolidayConfig.DayType.ADJUSTED_WORKDAY;
            }
        }

        // 3. 默认周末判断
        if (DateUtil.isWeekend(date)) {
            return HolidayConfig.DayType.NORMAL_WEEKEND;
        }

        // 4. 普通工作日
        return HolidayConfig.DayType.NORMAL_WORKDAY;
    }

    /**
     * 刷新节假日配置
     */
    public void refreshHolidayConfig() {
        this.holidayConfig = storageService.loadHolidayConfig();
    }
}
