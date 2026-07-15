package com.desktop.calendar.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 开机自启服务
 * 通过 Windows reg 命令操作注册表 HKCU\...\Run 实现开机自启动
 * 非 Windows 环境下降级为 no-op
 */
public class AutoStartService {

    private static final String REG_PATH = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "DesktopCalendar";
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    /**
     * 检查是否已设置开机自启
     */
    public static boolean isAutoStart() {
        if (!IS_WINDOWS) {
            System.err.println("开机自启功能仅支持 Windows 系统");
            return false;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", REG_PATH, "/v", APP_NAME);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(APP_NAME) && line.contains("REG_SZ")) {
                        return true;
                    }
                }
            }
            process.waitFor();
            return false;
        } catch (Exception e) {
            System.err.println("检查开机自启状态失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 设置/取消开机自启
     */
    public static void setAutoStart(boolean enable) {
        if (!IS_WINDOWS) {
            System.err.println("开机自启功能仅支持 Windows 系统");
            return;
        }
        try {
            if (enable) {
                String jarPath = getJarPath();
                String command = "javaw -jar \"" + jarPath + "\"";
                ProcessBuilder pb = new ProcessBuilder("reg", "add", REG_PATH, "/v", APP_NAME,
                        "/t", "REG_SZ", "/d", command, "/f");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();
                System.out.println("已设置开机自启: " + command);
            } else {
                ProcessBuilder pb = new ProcessBuilder("reg", "delete", REG_PATH, "/v", APP_NAME, "/f");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();
                System.out.println("已取消开机自启");
            }
        } catch (Exception e) {
            System.err.println("设置开机自启失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前运行的 jar 文件路径
     */
    private static String getJarPath() {
        try {
            String path = AutoStartService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            // Windows 路径去掉前导斜杠
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path.replace("/", "\\");
        } catch (Exception e) {
            System.err.println("获取 jar 路径失败: " + e.getMessage());
            return "DesktopCalendar.jar";
        }
    }
}
