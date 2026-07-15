package com.desktop.calendar.service;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

/**
 * 窗口管理服务
 * 负责窗口位置持久化（Preferences API）和屏幕边缘自动吸附
 */
public class WindowManager {

    private static final Preferences PREFS = Preferences.userNodeForPackage(WindowManager.class);
    private static final String KEY_X = "window_x";
    private static final String KEY_Y = "window_y";
    private static final String KEY_WIDTH = "window_width";
    private static final String KEY_HEIGHT = "window_height";
    private static final String KEY_SCALE = "window_scale";
    private static final double SNAP_THRESHOLD = 20.0;
    private static final double DEFAULT_X = 100;
    private static final double DEFAULT_Y = 100;
    private static final double DEFAULT_WIDTH = 384;
    private static final double DEFAULT_HEIGHT = 480;
    private static final double DEFAULT_SCALE = 1.0;
    public static final double MIN_SCALE = 0.6;
    public static final double MAX_SCALE = 2.0;
    public static final double SCALE_STEP = 0.1;

    /**
     * 恢复窗口位置（启动时调用）
     * 校验坐标是否在可见屏幕范围内，避免窗口跑到断屏区域
     */
    public static void restorePosition(Stage stage) {
        double x = PREFS.getDouble(KEY_X, DEFAULT_X);
        double y = PREFS.getDouble(KEY_Y, DEFAULT_Y);
        double w = PREFS.getDouble(KEY_WIDTH, DEFAULT_WIDTH);
        double h = PREFS.getDouble(KEY_HEIGHT, DEFAULT_HEIGHT);

        // 使用保存的宽高校验（启动时 stage 尺寸可能还是 0）
        if (!isPositionVisible(x, y, w, h)) {
            x = DEFAULT_X;
            y = DEFAULT_Y;
        }

        stage.setX(x);
        stage.setY(y);
    }

    /**
     * 恢复窗口缩放比例
     */
    public static double restoreScale() {
        return PREFS.getDouble(KEY_SCALE, DEFAULT_SCALE);
    }

    /**
     * 保存窗口位置 + 尺寸（关闭/隐藏时调用）
     */
    public static void savePosition(Stage stage) {
        double x = stage.getX();
        double y = stage.getY();
        double w = stage.getWidth();
        double h = stage.getHeight();
        if (x >= -1000 && y >= -1000) {
            PREFS.putDouble(KEY_X, x);
            PREFS.putDouble(KEY_Y, y);
        }
        if (w > 0 && h > 0) {
            PREFS.putDouble(KEY_WIDTH, w);
            PREFS.putDouble(KEY_HEIGHT, h);
        }
    }

    /**
     * 保存缩放比例
     */
    public static void saveScale(double scale) {
        PREFS.putDouble(KEY_SCALE, scale);
    }

    /**
     * 边缘吸附（拖拽结束时调用）
     * 距离屏幕边缘 < 20px 时自动吸附到边缘
     */
    public static void snapToEdge(Stage stage) {
        double x = stage.getX();
        double y = stage.getY();
        double w = stage.getWidth();
        double h = stage.getHeight();

        // 获取所有屏幕的总包围盒
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            minX = Math.min(minX, bounds.getMinX());
            minY = Math.min(minY, bounds.getMinY());
            maxX = Math.max(maxX, bounds.getMaxX());
            maxY = Math.max(maxY, bounds.getMaxY());
        }

        boolean snapped = false;

        // 左边缘吸附
        if (Math.abs(x - minX) < SNAP_THRESHOLD) {
            x = minX;
            snapped = true;
        }
        // 右边缘吸附
        if (Math.abs((x + w) - maxX) < SNAP_THRESHOLD) {
            x = maxX - w;
            snapped = true;
        }
        // 上边缘吸附
        if (Math.abs(y - minY) < SNAP_THRESHOLD) {
            y = minY;
            snapped = true;
        }
        // 下边缘吸附
        if (Math.abs((y + h) - maxY) < SNAP_THRESHOLD) {
            y = maxY - h;
            snapped = true;
        }

        if (snapped) {
            stage.setX(x);
            stage.setY(y);
        }

        // 保存最终位置
        savePosition(stage);
    }

    /**
     * 检查坐标是否在某个屏幕的可见范围内（放宽50px容差）
     */
    private static boolean isPositionVisible(double x, double y, double w, double h) {
        if (w <= 0) w = 320;
        if (h <= 0) h = 380;
        double margin = 50;

        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getBounds();
            // 窗口至少有一部分在屏幕内
            if (x + w > bounds.getMinX() - margin &&
                x < bounds.getMaxX() + margin &&
                y + h > bounds.getMinY() - margin &&
                y < bounds.getMaxY() + margin) {
                return true;
            }
        }
        return false;
    }
}
