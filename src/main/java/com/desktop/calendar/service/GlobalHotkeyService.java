package com.desktop.calendar.service;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

/**
 * 全局快捷键服务
 * 使用 Windows API RegisterHotKey 注册系统级快捷键
 * 在独立守护线程中监听消息循环
 */
public class GlobalHotkeyService {

    private static final Preferences PREFS = Preferences.userNodeForPackage(GlobalHotkeyService.class);
    private static final String KEY_MODIFIERS = "hotkey_modifiers";
    private static final String KEY_VK = "hotkey_vk";

    // 默认快捷键: Ctrl+Shift+C
    private static final int DEFAULT_MODIFIERS = 0x0002 | 0x0004; // MOD_CONTROL | MOD_SHIFT
    private static final int DEFAULT_VK = 0x43; // 'C'

    private static final int HOTKEY_ID = 1;
    private static final int WM_HOTKEY = 0x0312;

    private final Stage stage;
    private volatile boolean running = false;
    private int currentModifiers;
    private int currentVk;

    public GlobalHotkeyService(Stage stage) {
        this.stage = stage;
        // 从 Preferences 读取上次的快捷键设置
        this.currentModifiers = PREFS.getInt(KEY_MODIFIERS, DEFAULT_MODIFIERS);
        this.currentVk = PREFS.getInt(KEY_VK, DEFAULT_VK);
    }

    /**
     * 启动全局快捷键监听（在守护线程中运行）
     */
    public void start() {
        if (running) return;
        if (!isWindows()) {
            System.err.println("全局快捷键仅支持 Windows 系统");
            return;
        }

        running = true;
        Thread thread = new Thread(() -> {
            try {
                registerHotkey();
                // 消息循环
                WinUser.MSG msg = new WinUser.MSG();
                while (running) {
                    if (User32.INSTANCE.GetMessage(msg, null, 0, 0) > 0) {
                        if (msg.message == WM_HOTKEY && msg.wParam.intValue() == HOTKEY_ID) {
                            Platform.runLater(this::toggleWindow);
                        }
                    }
                    // 短暂休眠避免 CPU 空转
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                System.err.println("全局快捷键监听异常: " + e.getMessage());
            } finally {
                unregisterHotkey();
            }
        }, "GlobalHotkeyThread");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 停止全局快捷键监听
     */
    public void stop() {
        running = false;
        unregisterHotkey();
    }

    /**
     * 动态修改快捷键
     */
    public void setHotkey(int modifiers, int vk) {
        unregisterHotkey();
        this.currentModifiers = modifiers;
        this.currentVk = vk;
        // 保存到 Preferences
        PREFS.putInt(KEY_MODIFIERS, modifiers);
        PREFS.putInt(KEY_VK, vk);
        registerHotkey();
    }

    /**
     * 获取当前快捷键描述文本
     */
    public String getHotkeyText() {
        StringBuilder sb = new StringBuilder();
        if ((currentModifiers & 0x0002) != 0) sb.append("Ctrl+");
        if ((currentModifiers & 0x0004) != 0) sb.append("Shift+");
        if ((currentModifiers & 0x0001) != 0) sb.append("Alt+");
        if ((currentModifiers & 0x0008) != 0) sb.append("Win+");
        sb.append((char) currentVk);
        return sb.toString();
    }

    /**
     * 解析快捷键字符串为 modifiers + vk
     * 支持格式: "Ctrl+Shift+C"
     */
    public static int[] parseHotkey(String text) {
        int modifiers = 0;
        int vk = 0;
        String upper = text.toUpperCase().trim();
        String[] parts = upper.split("\\+");
        for (String part : parts) {
            switch (part.trim()) {
                case "CTRL", "CONTROL" -> modifiers |= 0x0002;
                case "SHIFT" -> modifiers |= 0x0004;
                case "ALT" -> modifiers |= 0x0001;
                case "WIN" -> modifiers |= 0x0008;
                default -> {
                    if (part.length() == 1) {
                        vk = part.charAt(0);
                    }
                }
            }
        }
        return new int[]{modifiers, vk};
    }

    public int getCurrentModifiers() { return currentModifiers; }
    public int getCurrentVk() { return currentVk; }

    private void registerHotkey() {
        try {
            boolean result = User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID, currentModifiers, currentVk);
            if (result) {
                System.out.println("全局快捷键注册成功: " + getHotkeyText());
            } else {
                System.err.println("全局快捷键注册失败（可能被其他程序占用）: " + getHotkeyText());
            }
        } catch (Exception e) {
            System.err.println("全局快捷键注册异常: " + e.getMessage());
        }
    }

    private void unregisterHotkey() {
        try {
            User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID);
        } catch (Exception ignored) {
            // 可能未注册过，忽略
        }
    }

    private void toggleWindow() {
        if (stage.isShowing()) {
            WindowManager.savePosition(stage);
            stage.hide();
        } else {
            stage.show();
            stage.toFront();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
