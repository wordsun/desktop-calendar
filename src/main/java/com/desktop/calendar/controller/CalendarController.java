package com.desktop.calendar.controller;

import com.desktop.calendar.model.CalendarEvent;
import com.desktop.calendar.model.LunarDate;
import com.desktop.calendar.service.CalendarService;
import com.desktop.calendar.service.LunarService;
import com.desktop.calendar.util.DateUtil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * 日历控制器
 * 协调视图与数据服务之间的交互
 * 处理月份切换、日期选择、日程增删等业务逻辑
 */
public class CalendarController {

    private final CalendarService calendarService;
    private final LunarService lunarService;
    private YearMonth currentYearMonth;
    private LocalDate selectedDate;

    public CalendarController(CalendarService calendarService, LunarService lunarService) {
        this.calendarService = calendarService;
        this.lunarService = lunarService;
        this.currentYearMonth = YearMonth.now();
        this.selectedDate = LocalDate.now();
    }

    /**
     * 切换到上一个月
     */
    public void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
    }

    /**
     * 切换到下一个月
     */
    public void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
    }

    /**
     * 跳转到今天
     */
    public void goToToday() {
        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now();
    }

    /**
     * 选择某个日期
     */
    public void onDateSelected(LocalDate date) {
        this.selectedDate = date;
    }

    /**
     * 获取当前显示的月份文本
     */
    public String getCurrentMonthText() {
        return DateUtil.formatMonth(currentYearMonth);
    }

    /**
     * 获取当前显示的年月
     */
    public YearMonth getCurrentYearMonth() {
        return currentYearMonth;
    }

    /**
     * 获取选中日期
     */
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    /**
     * 获取选中日期的农历信息
     */
    public LunarDate getLunarDate(LocalDate date) {
        return lunarService.toLunar(date);
    }

    /**
     * 获取某月有事件的日期集合
     */
    public Set<LocalDate> getEventDatesInMonth(YearMonth yearMonth) {
        return calendarService.getEventDatesInMonth(yearMonth);
    }

    /**
     * 获取指定日期的事件列表
     */
    public List<CalendarEvent> getEventsForDate(LocalDate date) {
        return calendarService.getEventsForDate(date);
    }

    /**
     * 添加日程事件
     */
    public void addEvent(CalendarEvent event) {
        calendarService.addEvent(event);
    }

    /**
     * 删除日程事件
     */
    public void removeEvent(String eventId) {
        calendarService.removeEvent(eventId);
    }

    /**
     * 查询某日是否为假日（法定假日 + 农历传统节日）
     */
    public boolean isHoliday(LocalDate date) {
        // 先查静态假日配置
        if (calendarService.getHoliday(date).isPresent()) {
            return true;
        }
        // 再查农历传统节日
        LunarDate lunar = lunarService.toLunar(date);
        if (lunar != null && lunar.getFestival() != null) {
            return true;
        }
        // 仅清明是法定假期的节气
        if (lunar != null && "清明".equals(lunar.getSolarTerm())) {
            return true;
        }
        return false;
    }

    /**
     * 获取日期的农历信息
     */
    public LunarDate getLunarDateInfo(LocalDate date) {
        return lunarService.toLunar(date);
    }

    /**
     * 查询某日是否为调休工作日
     */
    public boolean isWorkday(LocalDate date) {
        return calendarService.isWorkday(date);
    }

    /**
     * 刷新节假日配置（在线更新后调用）
     */
    public void refreshHolidayConfig() {
        calendarService.refreshHolidayConfig();
    }
}
