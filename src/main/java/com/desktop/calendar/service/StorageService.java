package com.desktop.calendar.service;

import com.desktop.calendar.model.CalendarEvent;
import com.desktop.calendar.model.HolidayConfig;
import com.desktop.calendar.util.JsonUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地存储服务
 * 负责JSON文件的读写，管理日程数据和节假日配置的持久化
 */
public class StorageService {

    private static final String DATA_DIR = System.getProperty("user.home") + "/.desktop-calendar";
    private static final String EVENTS_FILE = "events.json";
    private static final String HOLIDAY_FILE = "holidays.json";

    private final Path dataPath;
    private final Path eventsFile;
    private final Path holidayFile;

    public StorageService() {
        this.dataPath = Paths.get(DATA_DIR);
        this.eventsFile = dataPath.resolve(EVENTS_FILE);
        this.holidayFile = dataPath.resolve(HOLIDAY_FILE);
    }

    /**
     * 初始化存储目录和默认配置文件
     */
    public void init() {
        try {
            Files.createDirectories(dataPath);

            // 如果节假日配置文件不存在，创建默认配置
            if (!Files.exists(holidayFile)) {
                saveHolidayConfig(createDefaultHolidayConfig());
            }

            // 如果日程文件不存在，创建空文件
            if (!Files.exists(eventsFile)) {
                saveEvents(new ArrayList<>());
            }
        } catch (IOException e) {
            System.err.println("初始化存储目录失败: " + e.getMessage());
        }
    }

    /**
     * 加载所有日程事件
     */
    public List<CalendarEvent> loadEvents() {
        try {
            List<CalendarEvent> events = JsonUtil.readFromFile(
                    eventsFile,
                    JsonUtil.getType(new TypeToken<List<CalendarEvent>>() {})
            );
            return events != null ? events : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("加载日程数据失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 保存日程事件列表
     */
    public void saveEvents(List<CalendarEvent> events) {
        try {
            JsonUtil.writeToFile(eventsFile, events);
        } catch (IOException e) {
            System.err.println("保存日程数据失败: " + e.getMessage());
        }
    }

    /**
     * 加载节假日配置
     */
    public HolidayConfig loadHolidayConfig() {
        try {
            return JsonUtil.readFromFile(holidayFile, HolidayConfig.class);
        } catch (IOException e) {
            System.err.println("加载节假日配置失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 保存节假日配置
     */
    public void saveHolidayConfig(HolidayConfig config) {
        try {
            JsonUtil.writeToFile(holidayFile, config);
        } catch (IOException e) {
            System.err.println("保存节假日配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据目录路径
     */
    public Path getDataPath() {
        return dataPath;
    }

    /**
     * 从在线API自动更新节假日数据（timor.tech 免费API）
     * 每次启动时后台调用，失败不影响正常使用
     */
    public void autoUpdateHolidays() {
        int currentYear = LocalDate.now().getYear();
        try {
            HolidayConfig config = fetchHolidayFromApi(currentYear);
            if (config != null) {
                saveHolidayConfig(config);
                System.out.println("节假日数据已自动更新: " + currentYear + "年");
            }
        } catch (Exception e) {
            System.err.println("自动更新节假日失败(使用本地缓存): " + e.getMessage());
        }
    }

    /**
     * 从 timor.tech API 拉取指定年份的节假日数据
     * API文档: https://timor.tech/api/holiday
     */
    private HolidayConfig fetchHolidayFromApi(int year) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://timor.tech/api/holiday/year/" + year))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("节假日API返回异常: " + response.statusCode());
                return null;
            }

            JsonObject json = JsonUtil.fromJson(response.body(), JsonObject.class);
            if (json == null || !json.has("holiday")) {
                return null;
            }

            JsonObject holidayObj = json.getAsJsonObject("holiday");
            List<HolidayConfig.Holiday> holidays = new ArrayList<>();
            Map<String, String> workdays = new HashMap<>();

            for (Map.Entry<String, JsonElement> entry : holidayObj.entrySet()) {
                JsonObject item = entry.getValue().getAsJsonObject();
                boolean isHoliday = item.has("holiday") && item.get("holiday").getAsBoolean();
                String name = item.has("name") ? item.get("name").getAsString() : "";
                String dateStr = item.has("date") ? item.get("date").getAsString() : "";

                if (isHoliday && !name.isEmpty()) {
                    // 休息日（法定假日或周末）
                    String mmdd = dateStr.length() >= 10 ? dateStr.substring(5) : entry.getKey();
                    holidays.add(HolidayConfig.Holiday.builder()
                            .name(name)
                            .date(mmdd)
                            .type("LEGAL")
                            .build());
                } else if (!isHoliday && !name.isEmpty()) {
                    // 调休工作日
                    workdays.put(dateStr, name);
                }
            }

            return HolidayConfig.builder()
                    .year(year)
                    .holidays(holidays)
                    .workdays(workdays)
                    .build();
        } catch (Exception e) {
            System.err.println("拉取节假日API失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 创建 2026 年默认节假日配置（基于国务院办公厅发布格式）
     */
    private HolidayConfig createDefaultHolidayConfig() {
        List<HolidayConfig.Holiday> holidays = new ArrayList<>();

        // 元旦 1.1-1.3
        holidays.add(HolidayConfig.Holiday.builder().name("元旦").date("01-01").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("元旦").date("01-02").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("元旦").date("01-03").type("LEGAL").build());

        // 春节 1.26-2.1（农历除夕到正月初七）
        holidays.add(HolidayConfig.Holiday.builder().name("春节").date("01-26").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("春节").date("01-27").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("春节").date("01-28").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("春节").date("01-29").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("春节").date("01-30").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("春节").date("01-31").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("春节").date("02-01").type("LEGAL").build());

        // 清明节 4.4-4.6
        holidays.add(HolidayConfig.Holiday.builder().name("清明节").date("04-04").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("清明节").date("04-05").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("清明节").date("04-06").type("LEGAL").build());

        // 劳动节 5.1-5.5
        holidays.add(HolidayConfig.Holiday.builder().name("劳动节").date("05-01").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("劳动节").date("05-02").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("劳动节").date("05-03").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("劳动节").date("05-04").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("劳动节").date("05-05").type("LEGAL").build());

        // 端午节 5.31-6.2
        holidays.add(HolidayConfig.Holiday.builder().name("端午节").date("05-31").type("TRADITIONAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("端午节").date("06-01").type("TRADITIONAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("端午节").date("06-02").type("TRADITIONAL").build());

        // 中秋节 9.25-9.27
        holidays.add(HolidayConfig.Holiday.builder().name("中秋节").date("09-25").type("TRADITIONAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("中秋节").date("09-26").type("TRADITIONAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("中秋节").date("09-27").type("TRADITIONAL").build());

        // 国庆节 10.1-10.7
        holidays.add(HolidayConfig.Holiday.builder().name("国庆节").date("10-01").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("国庆节").date("10-02").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("国庆节").date("10-03").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("国庆节").date("10-04").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("国庆节").date("10-05").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("国庆节").date("10-06").type("LEGAL").build());
        holidays.add(HolidayConfig.Holiday.builder().name("国庆节").date("10-07").type("LEGAL").build());

        // 调休工作日
        Map<String, String> workdays = new HashMap<>();
        workdays.put("2026-01-24", "春节");  // 周六上班
        workdays.put("2026-02-07", "春节");  // 周六上班
        workdays.put("2026-04-26", "劳动节"); // 周日上班
        workdays.put("2026-10-10", "国庆节"); // 周六上班

        return HolidayConfig.builder()
                .year(2026)
                .holidays(holidays)
                .workdays(workdays)
                .build();
    }
}
