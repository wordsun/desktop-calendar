package com.desktop.calendar.service;

import com.desktop.calendar.model.CalendarEvent;
import com.desktop.calendar.model.HolidayConfig;
import com.desktop.calendar.util.JsonUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
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
     * 每次启动时从 classpath 资源文件加载默认节假日配置，确保配置始终保持最新
     */
    public void init() {
        try {
            Files.createDirectories(dataPath);

            // 每次启动从资源文件加载默认节假日配置
            try (InputStream in = getClass().getResourceAsStream("/holidays_2026.json")) {
                if (in != null) {
                    String content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    Files.writeString(holidayFile, content, java.nio.charset.StandardCharsets.UTF_8);
                    System.out.println("默认节假日配置已从资源文件加载");
                } else {
                    System.err.println("默认节假日资源文件 /holidays_2026.json 未找到");
                }
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
     * 拉取本年数据；若国家已发布次年数据则一并更新
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

        // 尝试拉取下一年数据（若 API 已发布则更新，否则保持本地默认）
        int nextYear = currentYear + 1;
        try {
            String nextYearFile = DATA_DIR + "/holidays_" + nextYear + ".json";
            if (!Files.exists(Paths.get(nextYearFile))) {
                HolidayConfig nextConfig = fetchHolidayFromApi(nextYear);
                if (nextConfig != null && nextConfig.getHolidays() != null && !nextConfig.getHolidays().isEmpty()) {
                    JsonUtil.writeToFile(Paths.get(nextYearFile), nextConfig);
                    System.out.println("次年节假日数据已获取: " + nextYear + "年");
                }
            }
        } catch (Exception e) {
            // 次年数据尚未发布或 API 不可用，静默跳过
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

}