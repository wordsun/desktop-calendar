package com.desktop.calendar.service;

import com.desktop.calendar.model.LunarDate;

import java.time.LocalDate;
import java.util.*;

/**
 * 农历计算服务（完整实现）
 * 基于天文算法计算农历日期、24节气、农历节日
 * 算法参考：Jean Meeus《Astronomical Algorithms》
 */
public class LunarService {

    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] ZODIAC = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
    private static final String[] MONTH_NAMES = {
            "正月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "冬月", "腊月"
    };
    private static final String[] DAY_NAMES = {
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };
    private static final String[] SOLAR_TERM_NAMES = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
            "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };

    /** 缓存已计算的农历年结构 */
    private final Map<Integer, LunarYearInfo> yearCache = new HashMap<>();

    // ==================== 公开 API ====================

    /**
     * 将公历日期转换为农历日期（含节气、节日信息）
     */
    public LunarDate toLunar(LocalDate date) {
        int solarYear = date.getYear();
        LunarYearInfo yearInfo = getLunarYearInfo(solarYear);

        int daysSinceStart = (int) (date.toEpochDay() - yearInfo.startDate.toEpochDay());

        // 查找所属农历月
        int lunarMonthIndex = -1;
        for (int i = 0; i < yearInfo.monthStartDays.length; i++) {
            if (daysSinceStart >= yearInfo.monthStartDays[i]) {
                lunarMonthIndex = i;
            } else {
                break;
            }
        }
        // 边界：如果在年初之前，使用前一年
        if (lunarMonthIndex < 0) {
            LunarYearInfo prevYear = getLunarYearInfo(solarYear - 1);
            daysSinceStart = (int) (date.toEpochDay() - prevYear.startDate.toEpochDay());
            for (int i = 0; i < prevYear.monthStartDays.length; i++) {
                if (daysSinceStart >= prevYear.monthStartDays[i]) {
                    lunarMonthIndex = i;
                } else break;
            }
            if (lunarMonthIndex >= 0) {
                int day = daysSinceStart - prevYear.monthStartDays[lunarMonthIndex] + 1;
                int absMonth = prevYear.monthNumbers.length > lunarMonthIndex
                        ? prevYear.monthNumbers[lunarMonthIndex] : 11;
                return buildLunarDate(date, prevYear.lunarYear, absMonth, day, prevYear);
            }
        }
        // 边界：如果在年末之后，使用下一年
        if (lunarMonthIndex < 0 || lunarMonthIndex >= yearInfo.monthNumbers.length) {
            LunarYearInfo nextYear = getLunarYearInfo(solarYear + 1);
            daysSinceStart = (int) (date.toEpochDay() - nextYear.startDate.toEpochDay());
            for (int i = 0; i < nextYear.monthStartDays.length; i++) {
                if (daysSinceStart >= nextYear.monthStartDays[i]) {
                    lunarMonthIndex = i;
                } else break;
            }
            if (lunarMonthIndex >= 0 && lunarMonthIndex < nextYear.monthNumbers.length) {
                int day = daysSinceStart - nextYear.monthStartDays[lunarMonthIndex] + 1;
                int absMonth = nextYear.monthNumbers[lunarMonthIndex];
                return buildLunarDate(date, nextYear.lunarYear, absMonth, day, nextYear);
            }
        }

        if (lunarMonthIndex < 0 || lunarMonthIndex >= yearInfo.monthNumbers.length) {
            // 降级处理
            return buildFallback(date);
        }

        int lunarDay = daysSinceStart - yearInfo.monthStartDays[lunarMonthIndex] + 1;
        int absMonthNum = yearInfo.monthNumbers[lunarMonthIndex];
        return buildLunarDate(date, yearInfo.lunarYear, absMonthNum, lunarDay, yearInfo);
    }

    /**
     * 获取指定日期的节气（如果当天是节气日）
     */
    public String getSolarTerm(LocalDate date) {
        for (int i = 0; i < 24; i++) {
            LocalDate termDate = calculateSolarTermDate(date.getYear(), i);
            if (termDate.equals(date)) {
                return SOLAR_TERM_NAMES[i];
            }
        }
        return null;
    }

    /**
     * 获取农历节日（如果当天是农历节日）
     */
    public String getLunarFestival(int lunarMonth, int lunarDay) {
        String key = lunarMonth + "-" + lunarDay;
        return LUNAR_FESTIVALS.getOrDefault(key, null);
    }

    /** 农历节日表 */
    private static final Map<String, String> LUNAR_FESTIVALS = new LinkedHashMap<>();
    static {
        LUNAR_FESTIVALS.put("1-1", "春节");
        LUNAR_FESTIVALS.put("1-15", "元宵节");
        LUNAR_FESTIVALS.put("2-2", "龙抬头");
        LUNAR_FESTIVALS.put("5-5", "端午节");
        LUNAR_FESTIVALS.put("7-7", "七夕");
        LUNAR_FESTIVALS.put("7-15", "中元节");
        LUNAR_FESTIVALS.put("8-15", "中秋节");
        LUNAR_FESTIVALS.put("9-9", "重阳节");
        LUNAR_FESTIVALS.put("12-8", "腊八节");
        LUNAR_FESTIVALS.put("12-23", "小年");
        LUNAR_FESTIVALS.put("12-30", "除夕");
    }

    // ==================== 构建 LunarDate ====================

    private LunarDate buildLunarDate(LocalDate date, int lunarYear, int absMonth, int lunarDay, LunarYearInfo yearInfo) {
        boolean isLeap = absMonth < 0;
        int monthNum = Math.abs(absMonth);
        monthNum = Math.max(1, Math.min(12, monthNum));
        lunarDay = Math.max(1, Math.min(30, lunarDay));

        String ganZhi = calculateGanZhi(lunarYear);
        String zodiac = calculateZodiac(lunarYear);
        String monthName = (isLeap ? "闰" : "") + MONTH_NAMES[monthNum - 1];
        String dayName = DAY_NAMES[Math.min(lunarDay - 1, 29)];

        String solarTerm = getSolarTerm(date);
        String festival = getLunarFestival(monthNum, lunarDay);
        // 除夕特殊处理：腊月最后一天
        if (festival == null && monthNum == 12) {
            int lastDay = getLunarMonthDays(lunarYear, absMonth);
            if (lunarDay == lastDay) festival = "除夕";
        }

        return LunarDate.builder()
                .year(lunarYear)
                .month(monthNum)
                .day(lunarDay)
                .ganZhiYear(ganZhi)
                .zodiac(zodiac)
                .monthName(monthName)
                .dayName(dayName)
                .leapMonth(isLeap)
                .solarTerm(solarTerm)
                .festival(festival)
                .build();
    }

    private LunarDate buildFallback(LocalDate date) {
        return LunarDate.builder()
                .year(date.getYear()).month(date.getMonthValue()).day(date.getDayOfMonth())
                .ganZhiYear(calculateGanZhi(date.getYear()))
                .zodiac(calculateZodiac(date.getYear()))
                .monthName(MONTH_NAMES[Math.min(date.getMonthValue() - 1, 11)])
                .dayName(DAY_NAMES[Math.min(date.getDayOfMonth() - 1, 29)])
                .build();
    }

    // ==================== 农历年结构计算 ====================

    /**
     * 计算指定公历年对应的农历年结构
     */
    private LunarYearInfo getLunarYearInfo(int lunarYear) {
        return yearCache.computeIfAbsent(lunarYear, this::computeLunarYearInfo);
    }

    private LunarYearInfo computeLunarYearInfo(int lunarYear) {
        // 找到农历年的起点：包含冬至的农历月（十一月）的新月
        LocalDate winterSolsticePrev = calculateSolarTermDate(lunarYear - 1, 23); // 前一年冬至
        double jdWS = calculateNewMoonJD(winterSolsticePrev);
        LocalDate month11Start = getNewMoonDate(jdWS);
        if (month11Start.isAfter(winterSolsticePrev.plusDays(30))) {
            // 如果新月在冬至之后太远，取前一个新月
            month11Start = getNewMoonDate(jdWS - 29);
        }

        // 找到下一个冬至
        LocalDate winterSolsticeCurr = calculateSolarTermDate(lunarYear, 23);
        double jdWS2 = calculateNewMoonJD(winterSolsticeCurr);
        LocalDate nextMonth11Start = getNewMoonDate(jdWS2);
        if (nextMonth11Start.isAfter(winterSolsticeCurr.plusDays(30))) {
            nextMonth11Start = getNewMoonDate(jdWS2 - 29);
        }
        // 确保 nextMonth11Start 在 month11Start 之后
        if (!nextMonth11Start.isAfter(month11Start)) {
            nextMonth11Start = getNewMoonDate(jdWS2 + 29);
        }

        // 列出两个冬至之间的所有农历月初一
        List<LocalDate> monthStarts = new ArrayList<>();
        LocalDate current = month11Start;
        while (current.isBefore(nextMonth11Start)) {
            monthStarts.add(current);
            double nextJd = calculateNewMoonJD(current.plusDays(28));
            current = getNewMoonDate(nextJd);
            if (!current.isAfter(monthStarts.get(monthStarts.size() - 1))) {
                current = getNewMoonDate(nextJd + 1);
            }
        }

        int totalMonths = monthStarts.size();
        int leapMonthIndex = -1;

        if (totalMonths == 13) {
            // 找第一个不含中气的月作为闰月
            for (int i = 1; i < 13; i++) { // 跳过第一个月（十一月，必含冬至）
                LocalDate mStart = monthStarts.get(i);
                LocalDate mEnd = (i + 1 < totalMonths) ? monthStarts.get(i + 1) : nextMonth11Start;
                if (!hasZhongqi(mStart, mEnd)) {
                    leapMonthIndex = i;
                    break;
                }
            }
        }

        // 计算月序号
        int[] monthNumbers = new int[totalMonths];
        int mNum = 11;
        for (int i = 0; i < totalMonths; i++) {
            if (leapMonthIndex >= 0 && i == leapMonthIndex) {
                monthNumbers[i] = -mNum; // 闰月用负数标记
            } else {
                if (leapMonthIndex >= 0 && i > leapMonthIndex) {
                    monthNumbers[i] = mNum;
                } else {
                    monthNumbers[i] = mNum;
                }
                mNum++;
                if (mNum > 12) mNum = 1;
            }
        }

        // 构建结果
        LunarYearInfo info = new LunarYearInfo();
        info.lunarYear = lunarYear;
        info.startDate = month11Start;
        info.monthCount = totalMonths;
        info.monthNumbers = monthNumbers;
        info.monthStartDays = new int[totalMonths];
        long startEpoch = month11Start.toEpochDay();
        for (int i = 0; i < totalMonths; i++) {
            info.monthStartDays[i] = (int) (monthStarts.get(i).toEpochDay() - startEpoch);
        }
        return info;
    }

    /**
     * 检查一个农历月是否包含中气（major solar term）
     */
    private boolean hasZhongqi(LocalDate monthStart, LocalDate monthEnd) {
        for (int i = 0; i < 12; i++) {
            LocalDate zhongqiDate = calculateSolarTermDate(monthStart.getYear(), i * 2);
            if (!zhongqiDate.isBefore(monthStart) && zhongqiDate.isBefore(monthEnd)) {
                return true;
            }
            // 也检查前一年/后一年的中气
            LocalDate zhongqiDateNext = calculateSolarTermDate(monthStart.getYear() + 1, i * 2);
            if (!zhongqiDateNext.isBefore(monthStart) && zhongqiDateNext.isBefore(monthEnd)) {
                return true;
            }
        }
        return false;
    }

    private int getLunarMonthDays(int lunarYear, int absMonthNum) {
        LunarYearInfo info = getLunarYearInfo(lunarYear);
        for (int i = 0; i < info.monthCount; i++) {
            if (info.monthNumbers[i] == absMonthNum) {
                if (i + 1 < info.monthCount) {
                    return info.monthStartDays[i + 1] - info.monthStartDays[i];
                }
                return 29;
            }
        }
        return 29;
    }

    /** 农历年结构内部类 */
    private static class LunarYearInfo {
        int lunarYear;
        LocalDate startDate;   // 农历十一月初一的公历日期
        int monthCount;        // 12 或 13
        int[] monthNumbers;    // 每月序号（负数=闰月）
        int[] monthStartDays;  // 每月初一距 startDate 的天数
    }

    // ==================== 天文算法 ====================

    /**
     * 计算新月日期（基于 Meeus 算法）
     */
    private double calculateNewMoonJD(LocalDate approxDate) {
        double jd = calendarToJD(approxDate.getYear(), approxDate.getMonthValue(), approxDate.getDayOfMonth());
        double k = (jd - 2451550.1) / 29.530588861;
        k = Math.round(k);
        return computeNewMoonK(k);
    }

    private LocalDate getNewMoonDate(double jd) {
        // 转换为北京时间（UTC+8）的日期
        double jdBeijing = jd + 8.0 / 24.0;
        return jdToDate(jdBeijing);
    }

    /**
     * Meeus 新月计算（Ch. 49）
     */
    private double computeNewMoonK(double k) {
        double T = k / 1236.85;
        double JDE = 2451550.09766 + 29.530588861 * k
                + 0.00015437 * T * T
                - 0.000000150 * T * T * T;

        double M = Math.toRadians(2.5534 + 29.10535670 * k - 0.0107038 * T * T - 0.0000100 * T * T * T);
        double Mp = Math.toRadians(201.5643 + 385.81693528 * k + 0.0107582 * T * T + 0.00001238 * T * T * T);
        double F = Math.toRadians(160.7108 + 390.67050284 * k - 0.0016118 * T * T - 0.00000227 * T * T * T);

        double correction = -0.40720 * Math.sin(Mp)
                + 0.17241 * Math.sin(M)
                + 0.01608 * Math.sin(2 * Mp)
                + 0.01039 * Math.sin(2 * F)
                + 0.00739 * Math.sin(Mp - M)
                - 0.00514 * Math.sin(Mp + M)
                + 0.00208 * Math.sin(-Mp + M)
                - 0.00111 * Math.sin(Mp - 2 * F)
                - 0.00057 * Math.sin(Mp + 2 * F);

        JDE += correction;

        // Delta T 近似（秒）
        double year = 2000.0 + (JDE - 2451545.0) / 365.25;
        double deltaT;
        if (year >= 2005 && year <= 2050) {
            double u = (year - 2000.0);
            deltaT = 62.92 + 0.32217 * u + 0.005589 * u * u;
        } else {
            deltaT = 67.0;
        }
        return JDE - deltaT / 86400.0; // 转为 UT
    }

    /**
     * 计算节气日期（基于太阳黄经）
     * @param year 公历年
     * @param termIndex 节气索引 0-23（小寒=0, 大寒=1, ..., 冬至=23）
     */
    private LocalDate calculateSolarTermDate(int year, int termIndex) {
        double targetLon = (285 + termIndex * 15.0) % 360.0;
        double approxJd = 2451623.8 + (year - 2000) * 365.2422 + termIndex * 15.2184;

        // 迭代求解太阳到达目标黄经的日期
        for (int iter = 0; iter < 5; iter++) {
            double lon = calculateSolarLongitude(approxJd);
            double diff = targetLon - lon;
            while (diff > 180) diff -= 360;
            while (diff < -180) diff += 360;
            approxJd += diff / 360.0 * 365.25;
        }

        // 转为北京时间日期
        double jdBeijing = approxJd + 8.0 / 24.0;
        return jdToDate(jdBeijing);
    }

    /**
     * 太阳黄经计算（Meeus Ch.25 简化版）
     */
    private double calculateSolarLongitude(double jd) {
        double T = (jd - 2451545.0) / 36525.0;
        double L0 = 280.46646 + 36000.76983 * T + 0.0003032 * T * T;
        double M = Math.toRadians(357.52911 + 35999.05029 * T - 0.0001537 * T * T);
        double C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * Math.sin(M)
                + (0.019993 - 0.000101 * T) * Math.sin(2 * M)
                + 0.000289 * Math.sin(3 * M);
        double omega = Math.toRadians(125.04 - 1934.136 * T);
        double lon = L0 + C - 0.00569 - 0.00478 * Math.sin(omega);
        return ((lon % 360) + 360) % 360;
    }

    // ==================== 日期工具 ====================

    private double calendarToJD(int year, int month, int day) {
        if (month <= 2) { year--; month += 12; }
        int A = year / 100;
        int B = 2 - A + A / 4;
        return (int) (365.25 * (year + 4716)) + (int) (30.6001 * (month + 1)) + day + B - 1524.5;
    }

    private LocalDate jdToDate(double jd) {
        jd += 0.5;
        int Z = (int) Math.floor(jd);
        double F = jd - Z;
        int A;
        if (Z < 2299161) { A = Z; }
        else { int alpha = (int) ((Z - 1867216.25) / 36524.25); A = Z + 1 + alpha - alpha / 4; }
        int B = A + 1524;
        int C = (int) ((B - 122.1) / 365.25);
        int D = (int) (365.25 * C);
        int E = (int) ((B - D) / 30.6001);
        int day = B - D - (int) (30.6001 * E);
        int month = (E < 14) ? E - 1 : E - 13;
        int year = (month > 2) ? C - 4716 : C - 4715;
        return LocalDate.of(year, month, Math.max(1, day));
    }

    // ==================== 天干地支/生肖 ====================

    private String calculateGanZhi(int year) {
        int ganIndex = (year - 4) % 10;
        int zhiIndex = (year - 4) % 12;
        return TIAN_GAN[ganIndex] + DI_ZHI[zhiIndex];
    }

    private String calculateZodiac(int year) {
        return ZODIAC[(year - 4) % 12];
    }
}
