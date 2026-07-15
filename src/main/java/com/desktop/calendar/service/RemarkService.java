package com.desktop.calendar.service;

import com.desktop.calendar.model.CalendarEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 待办事项 Markdown 文件服务
 * 在程序所在目录的 remark/ 下按 YYYY-MM-DD.md 格式读写每日待办
 */
public class RemarkService {

    private static final Path REMARK_DIR;

    static {
        // 程序所在目录下的 remark 文件夹
        String jarDir = System.getProperty("user.dir");
        REMARK_DIR = Paths.get(jarDir, "remark");
    }

    /**
     * 获取 remark 目录路径
     */
    public static Path getRemarkDir() {
        return REMARK_DIR;
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
        return REMARK_DIR.resolve(
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
        Path monthDir = REMARK_DIR.resolve(
                String.format("%04d/%02d", date.getYear(), date.getMonthValue()));
        Path yearDir = REMARK_DIR.resolve(String.valueOf(date.getYear()));

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
