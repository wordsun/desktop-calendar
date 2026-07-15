package com.desktop.calendar;

import com.desktop.calendar.service.*;
import com.desktop.calendar.view.CalendarView;
import com.desktop.calendar.view.SettingsDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * 桌面日历插件启动类
 * 负责初始化透明无边框窗口，集成所有系统级服务
 */
public class Main extends Application {

    private double xOffset = 0;
    private double yOffset = 0;
    private double scaleFactor = 1.0;
    private double resizeStartX = 0;
    private double resizeStartY = 0;
    private double resizeStartW = 0;
    private double resizeStartH = 0;
    private boolean resizing = false;
    private static final double BASE_WIDTH = 384;
    private static final double BASE_HEIGHT = 480;
    private static final double MIN_WINDOW_W = 250;
    private static final double MIN_WINDOW_H = 350;
    private static final double MAX_WINDOW_W = 800;
    private static final double MAX_WINDOW_H = 1000;

    @Override
    public void start(Stage primaryStage) {
        // 1. 初始化存储服务
        StorageService storageService = new StorageService();
        storageService.init();

        // 2. 创建主视图
        CalendarView calendarView = new CalendarView(storageService);
        StackPane root = new StackPane();
        root.getChildren().add(calendarView.getView());

        // 右下角拖拽缩放手柄
        Pane resizeHandle = createResizeHandle(primaryStage, root);
        root.getChildren().add(resizeHandle);
        StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);

        // 3. 恢复缩放比例
        scaleFactor = WindowManager.restoreScale();
        scaleFactor = Math.max(WindowManager.MIN_SCALE, Math.min(WindowManager.MAX_SCALE, scaleFactor));

        // 4. 创建透明场景（根据缩放比例调整尺寸）
        double sceneW = BASE_WIDTH * scaleFactor;
        double sceneH = BASE_HEIGHT * scaleFactor;
        Scene scene = new Scene(root, sceneW, sceneH, Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // 5. 应用缩放
        root.setScaleX(scaleFactor);
        root.setScaleY(scaleFactor);

        // 6. 配置主窗口：无边框 + 透明 + 置顶
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setTitle("桌面日历");

        // 7. 恢复窗口位置
        WindowManager.restorePosition(primaryStage);

        // 8. 窗口拖拽移动 + 边缘吸附
        scene.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        scene.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - xOffset);
            primaryStage.setY(event.getScreenY() - yOffset);
        });

        scene.setOnMouseReleased(event -> {
            WindowManager.snapToEdge(primaryStage);
        });

        // 9. Ctrl+滚轮缩放界面
        scene.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                e.consume();
                double delta = e.getDeltaY() > 0 ? WindowManager.SCALE_STEP : -WindowManager.SCALE_STEP;
                double newScale = Math.round((scaleFactor + delta) * 10.0) / 10.0;
                newScale = Math.max(WindowManager.MIN_SCALE, Math.min(WindowManager.MAX_SCALE, newScale));
                if (newScale != scaleFactor) {
                    scaleFactor = newScale;
                    root.setScaleX(scaleFactor);
                    root.setScaleY(scaleFactor);
                    // 调整场景尺寸以匹配缩放后的内容
                    scene.setRoot(root);
                    primaryStage.setWidth(BASE_WIDTH * scaleFactor);
                    primaryStage.setHeight(BASE_HEIGHT * scaleFactor);
                    // 保存缩放比例
                    WindowManager.saveScale(scaleFactor);
                }
            }
        });

        // 10. 初始化全局快捷键
        GlobalHotkeyService hotkeyService = new GlobalHotkeyService(primaryStage);
        hotkeyService.start();

        // 11. 初始化系统托盘
        TrayService trayService = new TrayService(primaryStage);
        trayService.init();

        // 12. 注入服务到视图（用于设置面板）
        calendarView.setServices(primaryStage, hotkeyService, trayService);

        // 13. 应用保存的透明度
        double savedOpacity = SettingsDialog.getSavedOpacity();
        primaryStage.setOpacity(savedOpacity);
        calendarView.updateOpacity(savedOpacity);

        primaryStage.show();

        // 窗口显示后保存初始尺寸（用于 resize 时的基准计算）
        resizeStartW = primaryStage.getWidth();
        resizeStartH = primaryStage.getHeight();

        // 14. 后台自动更新节假日数据（不阻塞UI）
        final CalendarView viewRef = calendarView;
        Thread updateThread = new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待2秒后开始拉取，避免启动时卡顿
                storageService.autoUpdateHolidays();
                // 更新成功后在UI线程刷新视图
                javafx.application.Platform.runLater(viewRef::refreshHolidayData);
            } catch (Exception e) {
                System.err.println("后台更新节假日异常: " + e.getMessage());
            }
        }, "HolidayUpdateThread");
        updateThread.setDaemon(true);
        updateThread.start();

        // 15. 关闭时保存位置 + 隐藏到托盘
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            WindowManager.savePosition(primaryStage);
            primaryStage.hide();
        });

        // 17. 应用退出钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            hotkeyService.stop();
            trayService.removeTrayIcon();
        }));
    }

    /**
     * 创建右下角拖拽缩放手柄
     * 鼠标左键拖拽时调整窗口大小并同步缩放内容
     */
    private Pane createResizeHandle(Stage stage, StackPane root) {
        Pane handle = new Pane();
        handle.setPrefSize(16, 16);
        handle.setMinSize(16, 16);
        handle.setMaxSize(16, 16);
        handle.setStyle("-fx-background-color: transparent; -fx-cursor: se_resize;");
        handle.setCursor(Cursor.SE_RESIZE);

        // 右下角三角形指示器
        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(16.0, 0.0, 16.0, 16.0, 0.0, 16.0);
        triangle.setFill(Color.web("#BBBBBB"));
        triangle.setOpacity(0.5);
        handle.getChildren().add(triangle);

        handle.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                resizing = true;
                resizeStartX = e.getScreenX();
                resizeStartY = e.getScreenY();
                resizeStartW = stage.getWidth();
                resizeStartH = stage.getHeight();
            }
        });

        handle.setOnMouseDragged(e -> {
            if (!resizing) return;
            double dx = e.getScreenX() - resizeStartX;
            double dy = e.getScreenY() - resizeStartY;
            double newW = Math.max(MIN_WINDOW_W, Math.min(MAX_WINDOW_W, resizeStartW + dx));
            double newH = Math.max(MIN_WINDOW_H, Math.min(MAX_WINDOW_H, resizeStartH + dy));

            // 按比例缩放：取宽高变化中较小的比例，保持长宽比
            double scaleW = newW / BASE_WIDTH;
            double scaleH = newH / BASE_HEIGHT;
            double newScale = Math.round(Math.min(scaleW, scaleH) * 10.0) / 10.0;
            newScale = Math.max(WindowManager.MIN_SCALE, Math.min(WindowManager.MAX_SCALE, newScale));

            double actualW = BASE_WIDTH * newScale;
            double actualH = BASE_HEIGHT * newScale;

            stage.setWidth(actualW);
            stage.setHeight(actualH);
            scaleFactor = newScale;
            root.setScaleX(scaleFactor);
            root.setScaleY(scaleFactor);
        });

        handle.setOnMouseReleased(e -> {
            if (resizing) {
                resizing = false;
                WindowManager.saveScale(scaleFactor);
                WindowManager.savePosition(stage);
            }
        });

        return handle;
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
