package com.desktop.calendar;

import com.desktop.calendar.service.*;
import com.desktop.calendar.view.CalendarView;
import com.desktop.calendar.view.SettingsDialog;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
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
    private double winX = 0;
    private double winY = 0;
    private boolean resizing = false;
    private long lastResizeUpdateNs = 0;
    private static final double BASE_WIDTH = 384;
    private static final double BASE_HEIGHT = 480;
    // 安全边距：防止极值下内容区挤压拖拽热区
    private static final double SAFE_MARGIN = 20;
    // 最小/最大尺寸 = 初始尺寸 × 0.8 / × 1.2 + 安全边距
    private static final double MIN_WINDOW_W = Math.round(BASE_WIDTH * 0.8) + SAFE_MARGIN;
    private static final double MIN_WINDOW_H = Math.round(BASE_HEIGHT * 0.8) + SAFE_MARGIN;
    private static final double MAX_WINDOW_W = Math.round(BASE_WIDTH * 1.2);
    private static final double MAX_WINDOW_H = Math.round(BASE_HEIGHT * 1.2);
    // 拖拽手柄引用（跨方法访问）
    private Pane resizeHandle;

    @Override
    public void start(Stage primaryStage) {
        // 1. 初始化存储服务
        StorageService storageService = new StorageService();
        storageService.init();

        // 2. 创建主视图
        CalendarView calendarView = new CalendarView(storageService);

        // 内容层：承载日历视图，受缩放影响
        StackPane contentRoot = new StackPane();
        contentRoot.getChildren().add(calendarView.getView());
        contentRoot.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        contentRoot.setMaxSize(BASE_WIDTH, BASE_HEIGHT);

        // 场景根容器：不受缩放影响，负责绝对定位
        Pane sceneRoot = new Pane();
        sceneRoot.getChildren().add(contentRoot);

        // 内容层居中
        contentRoot.layoutXProperty().bind(sceneRoot.widthProperty().subtract(BASE_WIDTH).divide(2));
        contentRoot.layoutYProperty().bind(sceneRoot.heightProperty().subtract(BASE_HEIGHT).divide(2));

        // 右下角拖拽缩放手柄：直接放 sceneRoot（不受缩放），始终锚定右下角
        resizeHandle = createResizeHandle(primaryStage, contentRoot);
        resizeHandle.setLayoutX(BASE_WIDTH - 24);
        resizeHandle.setLayoutY(BASE_HEIGHT - 24);
        resizeHandle.layoutXProperty().bind(sceneRoot.widthProperty().subtract(24));
        resizeHandle.layoutYProperty().bind(sceneRoot.heightProperty().subtract(24));
        sceneRoot.getChildren().add(resizeHandle);

        // 3. 恢复缩放比例
        scaleFactor = WindowManager.restoreScale();
        scaleFactor = Math.max(WindowManager.MIN_SCALE, Math.min(WindowManager.MAX_SCALE, scaleFactor));

        // 4. 创建透明场景（根据缩放比例调整尺寸）
        double sceneW = BASE_WIDTH * scaleFactor;
        double sceneH = BASE_HEIGHT * scaleFactor;
        Scene scene = new Scene(sceneRoot, sceneW, sceneH, Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // 5. 应用缩放（仅内容层）
        contentRoot.setScaleX(scaleFactor);
        contentRoot.setScaleY(scaleFactor);

        // 6. 配置主窗口：无边框 + 透明 + 置顶
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setTitle("桌面日历");

        // 7. 恢复窗口位置
        WindowManager.restorePosition(primaryStage);

        // 7b. Scene 级 resize 事件过滤器（鼠标移出手柄区域仍能继续拖拽）
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (resizing) {
                doResize(e.getScreenX(), e.getScreenY(), primaryStage, contentRoot);
                e.consume();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (resizing) {
                finishResize(primaryStage, contentRoot);
                e.consume();
            }
        });

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
                    contentRoot.setScaleX(scaleFactor);
                    contentRoot.setScaleY(scaleFactor);
                    primaryStage.setWidth(BASE_WIDTH * scaleFactor);
                    primaryStage.setHeight(BASE_HEIGHT * scaleFactor);
                    // 保存缩放比例
                    WindowManager.saveScale(scaleFactor);
                    resizeHandle.toFront();
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

        // 12b. 设置重置窗口大小回调（恢复原始尺寸）
        calendarView.setResetSizeCallback(() -> {
            scaleFactor = 1.0;
            contentRoot.setScaleX(1.0);
            contentRoot.setScaleY(1.0);
            primaryStage.setWidth(BASE_WIDTH);
            primaryStage.setHeight(BASE_HEIGHT);
            WindowManager.saveScale(1.0);
            WindowManager.savePosition(primaryStage);
            resizeHandle.toFront();
        });

        // 13. 应用保存的透明度
        double savedOpacity = SettingsDialog.getSavedOpacity();
        primaryStage.setOpacity(savedOpacity);
        calendarView.updateOpacity(savedOpacity);

        primaryStage.show();

        // 隐藏任务栏图标：设置 WS_EX_TOOLWINDOW 扩展样式
        hideFromTaskbar();

        // Z-Order 保障：确保拖拽手柄始终在最顶层，不被内容区遮挡
        resizeHandle.toFront();
        resizeHandle.setMouseTransparent(false);

        // 设置窗口尺寸硬边界（OS 级约束，防止超出 0.8x-1.2x 范围）
        primaryStage.setMinWidth(MIN_WINDOW_W);
        primaryStage.setMinHeight(MIN_WINDOW_H);
        primaryStage.setMaxWidth(MAX_WINDOW_W);
        primaryStage.setMaxHeight(MAX_WINDOW_H);

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
     *
     * 核心优化：
     * 1. 尺寸约束：min = 初始×0.8, max = 初始×1.2（stage.setMin/MaxWidth 硬限制）
     * 2. 时间节流（~16ms=60fps）保证跟手同时避免过度重绘
     * 3. Scene 级事件过滤器确保鼠标移出手柄区域仍能继续拖拽
     * 4. 拖拽中不取整 scale；mouseReleased 时取整到 0.1 并持久化
     */
    private Pane createResizeHandle(Stage stage, StackPane contentRoot) {
        Pane handle = new Pane();
        handle.setPrefSize(24, 24);
        handle.setMinSize(24, 24);
        handle.setMaxSize(24, 24);
        handle.setStyle("-fx-background-color: transparent; -fx-cursor: se_resize;");
        handle.setCursor(Cursor.SE_RESIZE);

        // 右下角三角形指示器（扩大至 24px 区域）
        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(24.0, 0.0, 24.0, 24.0, 0.0, 24.0);
        triangle.setFill(Color.web("#888888"));
        triangle.setOpacity(0.7);
        handle.getChildren().add(triangle);

        handle.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                beginResize(e.getScreenX(), e.getScreenY(), stage);
                e.consume();
            }
        });

        handle.setOnMouseDragged(e -> {
            if (resizing) {
                doResize(e.getScreenX(), e.getScreenY(), stage, contentRoot);
                e.consume();
            }
        });

        handle.setOnMouseReleased(e -> {
            if (resizing) {
                finishResize(stage, contentRoot);
                e.consume();
            }
        });

        return handle;
    }

    /** 开始拖拽缩放：记录初始状态 */
    private void beginResize(double screenX, double screenY, Stage stage) {
        resizing = true;
        resizeStartX = screenX;
        resizeStartY = screenY;
        resizeStartW = stage.getWidth();
        resizeStartH = stage.getHeight();
        winX = stage.getX();
        winY = stage.getY();
        lastResizeUpdateNs = 0;
    }

    /** 执行拖拽缩放（每 ~16ms 调用一次，手柄紧贴鼠标） */
    private void doResize(double screenX, double screenY, Stage stage, StackPane contentRoot) {
        if (!resizing) return;

        // 时间节流：两次更新至少间隔 ~16ms (60fps)
        long now = System.nanoTime();
        if (lastResizeUpdateNs > 0 && (now - lastResizeUpdateNs) < 16_000_000L) {
            return;
        }
        lastResizeUpdateNs = now;

        double dx = screenX - resizeStartX;
        double dy = screenY - resizeStartY;
        double newW = Math.max(MIN_WINDOW_W, Math.min(MAX_WINDOW_W, resizeStartW + dx));
        double newH = Math.max(MIN_WINDOW_H, Math.min(MAX_WINDOW_H, resizeStartH + dy));

        // 按比例缩放：取宽高中较小的比例，保持长宽比
        double scaleW = newW / BASE_WIDTH;
        double scaleH = newH / BASE_HEIGHT;
        double newScale = Math.min(scaleW, scaleH);
        newScale = Math.max(WindowManager.MIN_SCALE, Math.min(WindowManager.MAX_SCALE, newScale));

        // 连续更新（无步进节流）
        scaleFactor = newScale;
        double actualW = Math.round(BASE_WIDTH * scaleFactor);
        double actualH = Math.round(BASE_HEIGHT * scaleFactor);
        stage.setWidth(actualW);
        stage.setHeight(actualH);
        stage.setX(winX);
        stage.setY(winY);
        contentRoot.setScaleX(scaleFactor);
        contentRoot.setScaleY(scaleFactor);
        // 确保手柄始终在最顶层
        resizeHandle.toFront();
    }

    /** 完成拖拽缩放：对 scale 取整到 0.1 步进再持久化 */
    private void finishResize(Stage stage, StackPane contentRoot) {
        if (!resizing) return;
        resizing = false;
        lastResizeUpdateNs = 0;
        // 落盘时对齐到 0.1 步进，保证 Ctrl+滚轮 缩放时值一致
        scaleFactor = Math.round(scaleFactor * 10.0) / 10.0;
        scaleFactor = Math.max(WindowManager.MIN_SCALE, Math.min(WindowManager.MAX_SCALE, scaleFactor));
        double actualW = Math.round(BASE_WIDTH * scaleFactor);
        double actualH = Math.round(BASE_HEIGHT * scaleFactor);
        stage.setWidth(actualW);
        stage.setHeight(actualH);
        contentRoot.setScaleX(scaleFactor);
        contentRoot.setScaleY(scaleFactor);
        WindowManager.saveScale(scaleFactor);
        WindowManager.savePosition(stage);
        resizeHandle.toFront();
    }

    /**
     * 隐藏任务栏图标：通过 Win32 API 设置 WS_EX_TOOLWINDOW 扩展样式
     * 使窗口仅显示在系统托盘，不占据任务栏空间
     */
    private static void hideFromTaskbar() {
        // 通过窗口标题查找 JavaFX 原生窗口句柄
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, "桌面日历");
        if (hwnd != null) {
            int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
            // WS_EX_TOOLWINDOW = 0x80, 使窗口不显示在任务栏
            User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle | 0x80);
        }
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
