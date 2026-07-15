package com.desktop.calendar.view;

import com.desktop.calendar.controller.CalendarController;
import com.desktop.calendar.service.CalendarService;
import com.desktop.calendar.service.GlobalHotkeyService;
import com.desktop.calendar.service.LunarService;
import com.desktop.calendar.service.StorageService;
import com.desktop.calendar.service.TrayService;
import com.desktop.calendar.util.I18n;
import com.desktop.calendar.util.StyleUtil;
import com.desktop.calendar.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * 日历主视图
 * 组装顶部导航栏（月份切换 + 设置按钮）和月视图网格
 * 应用毛玻璃背景效果
 */
public class CalendarView {

    private VBox root;
    private final CalendarController controller;
    private final MonthView monthView;
    private Label monthLabel;
    private Button todayBtn;
    private GlobalHotkeyService hotkeyService;
    private TrayService trayService;
    private Stage ownerStage;

    public CalendarView(StorageService storageService) {
        CalendarService calendarService = new CalendarService(storageService);
        LunarService lunarService = new LunarService();
        this.controller = new CalendarController(calendarService, lunarService);

        root = new VBox(8);
        root.setPadding(new Insets(12));
        root.setStyle(StyleUtil.BG_GLASS + StyleUtil.DROP_SHADOW);
        root.setAlignment(Pos.TOP_CENTER);

        // 顶部导航栏
        HBox navBar = createNavBar();

        // 星期标题行
        HBox weekHeader = createWeekHeader();

        // 月视图网格
        monthView = new MonthView(controller);

        root.getChildren().addAll(navBar, weekHeader, monthView.getView());

        // 初始加载当月
        refreshMonth();
    }

    /**
     * 注入外部服务引用（在 Main 中调用）
     */
    public void setServices(Stage ownerStage, GlobalHotkeyService hotkeyService, TrayService trayService) {
        this.ownerStage = ownerStage;
        this.hotkeyService = hotkeyService;
        this.trayService = trayService;
        // 传递 Stage 和服务引用给 MonthView（用于双击弹窗、右键菜单和关闭清理）
        monthView.setOwnerStage(ownerStage, hotkeyService, trayService, this::refreshMonth);
    }

    /**
     * 更新背景透明度
     */
    public void updateOpacity(double opacity) {
        int alpha = (int) (opacity * 255);
        root.setStyle(String.format(
                "-fx-background-color: rgba(255, 255, 255, %.2f); " +
                "-fx-background-radius: 12; -fx-border-radius: 12;", opacity));
    }

    /**
     * 创建顶部导航栏（上/下月切换 + 月份标题 + 设置按钮）
     */
    private HBox createNavBar() {
        HBox navBar = new HBox(8);
        navBar.setAlignment(Pos.CENTER);
        navBar.setPadding(new Insets(4, 0, 4, 0));

        Button prevBtn = new Button("\u25C0");
        prevBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 17;");
        prevBtn.setOnAction(e -> { controller.previousMonth(); refreshMonth(); });

        monthLabel = new Label();
        monthLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, StyleUtil.FONT_SIZE_TITLE));
        monthLabel.setTextFill(Color.web("#333333"));

        Button nextBtn = new Button("\u25B6");
        nextBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 17;");
        nextBtn.setOnAction(e -> { controller.nextMonth(); refreshMonth(); });

        // 今日按钮
        todayBtn = new Button(I18n.get("btn.today"));
        String themeHex = ThemeManager.getCurrentColorHex();
        todayBtn.setStyle("-fx-background-color: " + themeHex + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 13; -fx-cursor: hand;");
        todayBtn.setOnAction(e -> { controller.goToToday(); refreshMonth(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 设置按钮
        Button settingsBtn = new Button("\u2699");
        settingsBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 19;");
        settingsBtn.setOnAction(e -> openSettings());

        navBar.getChildren().addAll(prevBtn, monthLabel, nextBtn, spacer, todayBtn, settingsBtn);
        return navBar;
    }

    /**
     * 打开设置面板
     */
    private void openSettings() {
        if (ownerStage != null && hotkeyService != null && trayService != null) {
            SettingsDialog dialog = new SettingsDialog(ownerStage, hotkeyService, trayService, this::applySettings);
            dialog.show();
        }
    }

    /**
     * 设置保存后刷新界面（主题色+语言）
     */
    public void applySettings() {
        applyTheme();
        applyLanguage();
        refreshMonth();
    }

    /**
     * 应用主题色到UI元素
     */
    public void applyTheme() {
        String themeHex = ThemeManager.getCurrentColorHex();
        todayBtn.setStyle("-fx-background-color: " + themeHex + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 13; -fx-cursor: hand;");
    }

    /**
     * 应用语言到UI元素
     */
    public void applyLanguage() {
        todayBtn.setText(I18n.get("btn.today"));
    }

    /**
     * 创建星期标题行
     */
    private HBox createWeekHeader() {
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(4, 0, 4, 0));

        String[] weeks = {"一", "二", "三", "四", "五", "六", "日"};
        for (int i = 0; i < weeks.length; i++) {
            Label label = new Label(weeks[i]);
            label.setFont(Font.font("Microsoft YaHei", StyleUtil.FONT_SIZE_SMALL));
            label.setAlignment(Pos.CENTER);
            label.setPrefWidth(48);

            if (i >= 5) {
                label.setTextFill(Color.web(StyleUtil.WEEKEND_COLOR));
            } else {
                label.setTextFill(Color.web("#666666"));
            }

            header.getChildren().add(label);
        }

        return header;
    }

    /**
     * 刷新月视图显示
     */
    public void refreshMonth() {
        monthLabel.setText(controller.getCurrentMonthText());
        monthView.refresh();
    }

    public VBox getView() {
        return root;
    }

    public CalendarController getController() {
        return controller;
    }

    /**
     * 刷新节假日数据（在线更新后调用）
     */
    public void refreshHolidayData() {
        controller.refreshHolidayConfig();
        refreshMonth();
    }
}
