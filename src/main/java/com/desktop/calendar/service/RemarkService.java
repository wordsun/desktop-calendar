package com.desktop.calendar.service;

import com.desktop.calendar.model.CalendarEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * 待办事项 Markdown 文件服务
 * 在可配置的 remark/ 目录下按 YYYY-MM-DD.md 格式读写每日待办
 * 路径可通过 Preferences 配置，默认在程序运行目录的 remark/ 下
 */
public class RemarkService {

    private static final Preferences PREFS = Preferences.userNodeForPackage(RemarkService.class);
    private static final String KEY_REMARK_DIR = "remark_dir";

    /**
     * 获取当前 remark 根目录路径
     */
    public static Path getRemarkDir() {
        String customPath = PREFS.get(KEY_REMARK_DIR, null);
        if (customPath != null && !customPath.trim().isEmpty()) {
            return Paths.get(customPath.trim());
        }
        // 默认：程序运行目录下的 remark
        return Paths.get(System.getProperty("user.dir"), "remark");
    }

    /**
     * 设置 remark 目录路径（保存到 Preferences）
     */
    public static void setRemarkDir(String path) {
        if (path == null || path.trim().isEmpty()) {
            PREFS.remove(KEY_REMARK_DIR);
        } else {
            PREFS.put(KEY_REMARK_DIR, path.trim());
        }
    }

    /**
     * 获取已保存的 remark 目录路径（用于设置面板显示）
     */
    public static String getSavedRemarkDir() {
        return PREFS.get(KEY_REMARK_DIR, "");
    }

    /**
     * 保存指定日期的待办列表到 MD 文件
     * 如果事件列表为空，删除对应文件
     */
    public static void saveDayEvents(LocalDate date, List<CalendarEvent> events) {
        try {
            Path file = getDayFile(date);
            if (events == null || events.isEmpty()) {
                Files.deleteIfExists(file);
                // 清理空目录（按年/月删除空目录）
                cleanupEmptyDirs(date);
                return;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, buildMarkdown(date, events));
        } catch (IOException e) {
            System.err.println("保存待办MD文件失败: " + e.getMessage());
        }
    }

    /**
     * 读取指定日期的待办 MD 文件内容
     */
    public static String loadDayMarkdown(LocalDate date) {
        try {
            Path file = getDayFile(date);
            if (Files.exists(file)) {
                return Files.readString(file);
            }
        } catch (IOException e) {
            System.err.println("读取待办MD文件失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取指定日期对应的 MD 文件路径
     * 格式：remark/YYYY/MM/DD.md
     */
    private static Path getDayFile(LocalDate date) {
        return getRemarkDir().resolve(
                String.format("%04d/%02d/%02d.md",
                        date.getYear(), date.getMonthValue(), date.getDayOfMonth())
        );
    }

    /**
     * 构建 Markdown 内容
     */
    private static String buildMarkdown(LocalDate date, List<CalendarEvent> events) {
        StringBuilder sb = new StringBuilder();
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        sb.append("# ").append(dateStr).append(" 待办事项\n\n");

        for (int i = 0; i < events.size(); i++) {
            CalendarEvent e = events.get(i);
            String checkbox = e.isAllDay() ? "- [ ]" : "- [x]";
            sb.append(checkbox).append(" ").append(e.getTitle()).append("\n");
            if (e.getDescription() != null && !e.getDescription().isEmpty()) {
                sb.append("  - 备注：").append(e.getDescription()).append("\n");
            }
            if (i < events.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 清理空目录（日期文件删除后，尝试删除空的月/年目录）
     */
    private static void cleanupEmptyDirs(LocalDate date) throws IOException {
        Path remarkDir = getRemarkDir();
        Path monthDir = remarkDir.resolve(
                String.format("%04d/%02d", date.getYear(), date.getMonthValue()));
        Path yearDir = remarkDir.resolve(String.valueOf(date.getYear()));

        // 删除空的月份目录
        if (Files.exists(monthDir) && isDirEmpty(monthDir)) {
            Files.delete(monthDir);
        }
        // 删除空的年份目录
        if (Files.exists(yearDir) && isDirEmpty(yearDir)) {
            Files.delete(yearDir);
        }
    }

    private static boolean isDirEmpty(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream.findAny().isEmpty();
        }
    }
}
