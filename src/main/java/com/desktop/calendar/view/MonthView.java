package com.desktop.calendar.view;

import com.desktop.calendar.controller.CalendarController;
import com.desktop.calendar.model.HolidayConfig;
import com.desktop.calendar.model.LunarDate;
import com.desktop.calendar.service.GlobalHotkeyService;
import com.desktop.calendar.service.TrayService;
import com.desktop.calendar.service.WindowManager;
import com.desktop.calendar.util.DateUtil;
import com.desktop.calendar.util.I18n;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * 月视图网格
 * 以7列网格展示一个月的日期，包含前后月份的补齐日期
 * 支持：单击选择日期、双击添加待办、右键菜单
 */
public class MonthView {

    private final CalendarController controller;
    private final javafx.scene.layout.GridPane grid;
    private Stage ownerStage;
    private GlobalHotkeyService hotkeyService;
    private TrayService trayService;
    private Runnable refreshCallback;
    private Runnable resetSizeCallback;
    private ContextMenu currentMenu; // 当前打开的右键菜单（保证同时只有一个）

    public MonthView(CalendarController controller) {
        this.controller = controller;
        this.grid = new javafx.scene.layout.GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setAlignment(javafx.geometry.Pos.CENTER);
    }

    /**
     * 设置外部引用（用于双击弹窗和右键菜单）
     */
    public void setOwnerStage(Stage ownerStage, GlobalHotkeyService hotkeyService,
                               TrayService trayService, Runnable refreshCallback) {
        this.ownerStage = ownerStage;
        this.hotkeyService = hotkeyService;
        this.trayService = trayService;
        this.refreshCallback = refreshCallback;
    }

    /**
     * 刷新月视图，重新生成所有日期格子
     */
    public void refresh() {
        grid.getChildren().clear();

        YearMonth currentMonth = controller.getCurrentYearMonth();
        List<LocalDate> gridDates = DateUtil.getMonthGridDates(currentMonth);
        Set<LocalDate> eventDates = controller.getEventDatesInMonth(currentMonth);

        int col = 0;
        int row = 0;

        for (LocalDate date : gridDates) {
            boolean isCurrentMonth = date.getMonthValue() == currentMonth.getMonthValue();
            boolean hasEvent = eventDates.contains(date);
            HolidayConfig.DayType dayType = controller.getDayType(date);
            LunarDate lunarDate = controller.getLunarDate(date);

            DayCell cell = new DayCell(date, lunarDate, isCurrentMonth, hasEvent, dayType);

            // 鼠标事件：单击选择、双击添加待办、右键菜单
            cell.getView().setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    controller.onDateSelected(date);
                    if (e.getClickCount() == 2) {
                        // 双击打开待办查看/管理弹窗
                        openTodoListDialog(date);
                    }
                }
            });

            // 右键菜单
            cell.getView().setOnContextMenuRequested(e -> {
                showContextMenu(date, e.getScreenX(), e.getScreenY());
            });

            grid.add(cell.getView(), col, row);

            col++;
            if (col >= 7) {
                col = 0;
                row++;
            }
        }
    }

    /**
     * 双击日期打开待办查看/管理弹窗
     */
    private void openTodoListDialog(LocalDate date) {
        if (ownerStage == null) return;
        Platform.runLater(() -> {
            TodoListDialog dialog = new TodoListDialog(ownerStage, controller, date, refreshCallback);
            dialog.show();
        });
    }

    /**
     * 设置重置窗口大小回调
     */
    public void setResetSizeCallback(Runnable callback) {
        this.resetSizeCallback = callback;
    }

    /**
     * 右键弹出菜单
     */
    private void showContextMenu(LocalDate date, double x, double y) {
        if (ownerStage == null) return;

        // 关闭之前打开的菜单，保证同时只有一个
        if (currentMenu != null && currentMenu.isShowing()) {
            currentMenu.hide();
        }

        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-font: 13 'Microsoft YaHei';");

        // 添加待办
        String dateStr = "en".equals(I18n.getLang()) ?
                date.getMonthValue() + "/" + date.getDayOfMonth() :
                date.getMonthValue() + "月" + date.getDayOfMonth() + "日";
        MenuItem addTodo = new MenuItem(I18n.get("menu.addTodo") + " - " + dateStr);
        addTodo.setOnAction(e -> openTodoListDialog(date));

        // 隐藏到托盘
        MenuItem hideToTray = new MenuItem(I18n.get("menu.hideToTray"));
        hideToTray.setOnAction(e -> {
            ownerStage.hide();
        });

        // 关闭插件
        MenuItem close = new MenuItem(I18n.get("menu.close"));
        close.setStyle("-fx-text-fill: #E74C3C;");
        close.setOnAction(e -> {
            // 保存窗口位置后再退出
            if (ownerStage != null) WindowManager.savePosition(ownerStage);
            if (hotkeyService != null) hotkeyService.stop();
            if (trayService != null) trayService.removeTrayIcon();
            Platform.exit();
            System.exit(0);
        });

        // 重置窗口大小（兜底恢复机制）
        if (resetSizeCallback != null) {
            MenuItem resetSize = new MenuItem(I18n.get("settings.resetSize"));
            resetSize.setOnAction(e -> resetSizeCallback.run());
            menu.getItems().addAll(addTodo, new javafx.scene.control.SeparatorMenuItem(),
                    hideToTray, resetSize, close);
        } else {
            menu.getItems().addAll(addTodo, new javafx.scene.control.SeparatorMenuItem(), hideToTray, close);
        }
        currentMenu = menu;
        menu.show(grid, x, y);

        // 菜单关闭时清除引用
        menu.setOnHidden(e -> currentMenu = null);
    }

    public javafx.scene.layout.GridPane getView() {
        return grid;
    }
}
