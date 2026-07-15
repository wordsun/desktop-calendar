package com.desktop.calendar.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 日期工具类
 * 提供日历相关的日期计算、格式化等辅助方法
 */
public class DateUtil {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy年MM月");
    public static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    /**
     * 判断是否为今天
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now());
    }

    /**
     * 判断是否为周末
     */
    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /**
     * 获取某月的所有日期（包含前后补齐的日期，用于日历网格显示）
     * 返回6行7列共42天的列表
     */
    public static List<LocalDate> getMonthGridDates(YearMonth yearMonth) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate firstDay = yearMonth.atDay(1);

        // 计算第一天是周几（周一=0，周日=6）
        int dayOfWeek = firstDay.getDayOfWeek().getValue() - 1;

        // 向前补齐
        LocalDate startDate = firstDay.minusDays(dayOfWeek);

        // 生成42天（6周）
        for (int i = 0; i < 42; i++) {
            dates.add(startDate.plusDays(i));
        }

        return dates;
    }

    /**
     * 获取两个日期之间的天数差
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 格式化日期为 yyyy-MM-dd
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    /**
     * 格式化月份为 yyyy年MM月
     */
    public static String formatMonth(YearMonth yearMonth) {
        return yearMonth != null ? yearMonth.format(MONTH_FMT) : "";
    }

    /**
     * 获取中文星期
     */
    public static String getChineseWeekDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "一";
            case TUESDAY -> "二";
            case WEDNESDAY -> "三";
            case THURSDAY -> "四";
            case FRIDAY -> "五";
            case SATURDAY -> "六";
            case SUNDAY -> "日";
        };
    }
}
