package com.desktop.calendar.util;

/**
 * 样式工具类
 * 提供毛玻璃效果、颜色、CSS样式相关的常量和方法
 */
public class StyleUtil {

    /** 毛玻璃背景色（半透明白色） */
    public static final String BG_GLASS = "-fx-background-color: rgba(255, 255, 255, 0.75); "
            + "-fx-background-radius: 12; "
            + "-fx-border-radius: 12;";

    /** 深色毛玻璃背景 */
    public static final String BG_GLASS_DARK = "-fx-background-color: rgba(30, 30, 30, 0.8); "
            + "-fx-background-radius: 12; "
            + "-fx-border-radius: 12;";

    /** 今日高亮样式 */
    public static final String TODAY_HIGHLIGHT = "-fx-background-color: #4A90D9; "
            + "-fx-background-radius: 50%; "
            + "-fx-text-fill: white;";

    /** 周末文字颜色 */
    public static final String WEEKEND_COLOR = "#E74C3C";

    /** 农历文字颜色 */
    public static final String LUNAR_COLOR = "#888888";

    /** 节日文字颜色 */
    public static final String FESTIVAL_COLOR = "#E67E22";

    /** 节气文字颜色（草绿色） */
    public static final String SOLAR_TERM_COLOR = "#2E8B57";

    /** 默认字体大小（已放大1.2倍） */
    public static final double FONT_SIZE_NORMAL = 17;
    public static final double FONT_SIZE_SMALL = 12;
    public static final double FONT_SIZE_TITLE = 22;

    /** 圆角半径 */
    public static final double BORDER_RADIUS = 12.0;

    /** 窗口阴影效果CSS */
    public static final String DROP_SHADOW = "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);";

    /**
     * 生成带透明度的背景色CSS
     */
    public static String glassBackground(int opacity) {
        return String.format(
                "-fx-background-color: rgba(255, 255, 255, %.2f); " +
                "-fx-background-radius: %f; " +
                "-fx-border-radius: %f;",
                opacity / 100.0, BORDER_RADIUS, BORDER_RADIUS
        );
    }

    /**
     * 生成带圆角的背景色CSS
     */
    public static String roundedBackground(String color, double radius) {
        return String.format(
                "-fx-background-color: %s; -fx-background-radius: %f; -fx-border-radius: %f;",
                color, radius, radius
        );
    }
}
