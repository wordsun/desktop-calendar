package com.desktop.calendar.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.prefs.Preferences;

/**
 * 主题色管理器
 * 支持5种主题色：蓝色、绿色、橙色、紫色、红色
 * 持久化到 Preferences，提供实时预览属性
 */
public class ThemeManager {

    public enum ThemeColor {
        BLUE("#4A90D9", "蓝色"),
        GREEN("#27AE60", "绿色"),
        ORANGE("#E67E22", "橙色"),
        PURPLE("#8E44AD", "紫色"),
        RED("#E74C3C", "红色");

        private final String hex;
        private final String displayName;

        ThemeColor(String hex, String displayName) {
            this.hex = hex;
            this.displayName = displayName;
        }

        public String getHex() { return hex; }
        public String getDisplayName() { return displayName; }

        /** 生成带透明度的颜色值 */
        public String withAlpha(double alpha) {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, alpha);
        }
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String KEY_THEME = "theme_color";

    private static final ThemeManager INSTANCE = new ThemeManager();
    private final StringProperty currentColorHex = new SimpleStringProperty();

    private ThemeManager() {
        String saved = PREFS.get(KEY_THEME, ThemeColor.BLUE.hex);
        currentColorHex.set(saved);
    }

    public static ThemeManager getInstance() { return INSTANCE; }

    /** 获取当前主题色十六进制值 */
    public static String getCurrentColorHex() {
        return INSTANCE.currentColorHex.get();
    }

    /** 获取当前主题色枚举 */
    public static ThemeColor getCurrentTheme() {
        String hex = getCurrentColorHex();
        for (ThemeColor tc : ThemeColor.values()) {
            if (tc.hex.equals(hex)) return tc;
        }
        return ThemeColor.BLUE;
    }

    /** 设置主题色并持久化 */
    public static void setTheme(ThemeColor theme) {
        INSTANCE.currentColorHex.set(theme.hex);
        PREFS.put(KEY_THEME, theme.hex);
    }

    /** 主题色属性（用于监听变化） */
    public static StringProperty colorProperty() {
        return INSTANCE.currentColorHex;
    }
}
