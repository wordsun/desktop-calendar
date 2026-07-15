package com.desktop.calendar.view;

import com.desktop.calendar.controller.CalendarController;
import com.desktop.calendar.model.CalendarEvent;
import com.desktop.calendar.util.ThemeManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * 待办事项查看/管理弹窗
 * 双击日期时弹出，展示该日期所有待办，支持添加和删除
 */
public class TodoListDialog {

    private static final Preferences PREFS = Preferences.userNodeForPackage(TodoListDialog.class);
    private static final String KEY_FONT_FAMILY = "todo_font_family";
    private static final String KEY_FONT_SIZE = "todo_font_size";

    private final Stage dialog;
    private final CalendarController controller;
    private final LocalDate date;
    private final Runnable onChanged;
    private VBox listContainer;

    public TodoListDialog(Stage owner, CalendarController controller, LocalDate date, Runnable onChanged) {
        this.controller = controller;
        this.date = date;
        this.onChanged = onChanged;

        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(date + " 待办事项");
        dialog.setResizable(false);

        VBox root = buildUI();
        Scene scene = new Scene(root, 320, 330, Color.TRANSPARENT);
        dialog.setScene(scene);
    }

    private VBox buildUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(14));
        String themeHex = ThemeManager.getCurrentColorHex();
        root.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); " +
                "-fx-background-radius: 12; -fx-border-radius: 12; " +
                "-fx-border-color: #ddd; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");

        // 标题
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        Label titleLabel = new Label("📋 " + dateStr + " 待办事项");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 15));
        titleLabel.setTextFill(Color.web("#1a1a1a"));

        // 待办列表区域
        listContainer = new VBox(6);
        listContainer.setPadding(new Insets(0));

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; " +
                "-fx-padding: 0; -fx-border-color: #eee; -fx-border-radius: 6;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // 添加表单
        TextField titleField = new TextField();
        titleField.setPromptText("输入待办事项...");
        titleField.setStyle("-fx-font-size: 13; -fx-padding: 6 8; -fx-background-radius: 6;");

        TextArea descArea = new TextArea();
        descArea.setPromptText("备注（可选）...");
        descArea.setPrefRowCount(2);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-font-size: 12; -fx-padding: 6; -fx-background-radius: 6;");

        // 保存并关闭按钮（同时处理添加逻辑）
        Button saveCloseBtn = new Button("保存并关闭");
        saveCloseBtn.setStyle("-fx-background-color: " + themeHex + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 20;");
        saveCloseBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            if (!title.isEmpty()) {
                CalendarEvent event = CalendarEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .title(title)
                        .description(descArea.getText().trim())
                        .date(date)
                        .allDay(true)
                        .build();
                controller.addEvent(event);
                if (onChanged != null) onChanged.run();
            }
            dialog.close();
        });
        HBox buttonRow = new HBox(saveCloseBtn);
        buttonRow.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, scrollPane, titleField, descArea, buttonRow);

        // 初始加载待办列表
        refreshList();

        return root;
    }

    /**
     * 刷新待办列表
     */
    private void refreshList() {
        listContainer.getChildren().clear();
        List<CalendarEvent> events = controller.getEventsForDate(date);

        if (events.isEmpty()) {
            Label emptyLabel = new Label("  暂无待办事项");
            emptyLabel.setFont(Font.font("Microsoft YaHei", 12));
            // 强制着色策略：代码级 setTextFill 最高优先级 + 内联样式兜底
            emptyLabel.setTextFill(Color.BLACK);
            emptyLabel.setStyle("-fx-text-fill: black; -fx-font-family: 'Microsoft YaHei'; -fx-font-size: 12px;");
            listContainer.getChildren().add(emptyLabel);
            return;
        }

        for (CalendarEvent event : events) {
            HBox row = createEventRow(event);
            listContainer.getChildren().add(row);
        }
    }

    /**
     * 创建单条待办事项行
     */
    private HBox createEventRow(CalendarEvent event) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 8, 6, 8));
        row.setStyle("-fx-background-color: #f8f8f8; -fx-background-radius: 6;");

        // 待办内容
        VBox content = new VBox(2);
        Label titleLabel = new Label("☐ " + event.getTitle());
        String fontFamily = getSavedFontFamily();
        int fontSize = getSavedFontSize();
        titleLabel.setStyle(String.format(
            "-fx-font-family: '%s'; -fx-font-size: %dpx; -fx-text-fill: #1a1a1a;",
            fontFamily, fontSize));
        titleLabel.setTextFill(Color.web("#1a1a1a"));
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(260);

        content.getChildren().add(titleLabel);

        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            Label descLabel = new Label("    " + event.getDescription());
            descLabel.setStyle(String.format(
                "-fx-font-family: '%s'; -fx-font-size: %dpx; -fx-text-fill: #555555;",
                fontFamily, Math.max(10, fontSize - 2)));
            descLabel.setTextFill(Color.web("#555555"));
            descLabel.setWrapText(true);
            descLabel.setMaxWidth(260);
            content.getChildren().add(descLabel);
        }

        HBox.setHgrow(content, Priority.ALWAYS);

        // 删除按钮
        Button delBtn = new Button("✕");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ccc; " +
                "-fx-font-size: 14; -fx-cursor: hand; -fx-padding: 2 6;");
        delBtn.setOnMouseEntered(e -> delBtn.setStyle("-fx-background-color: #ffe0e0; " +
                "-fx-text-fill: #E74C3C; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 2 6; " +
                "-fx-background-radius: 4;"));
        delBtn.setOnMouseExited(e -> delBtn.setStyle("-fx-background-color: transparent; " +
                "-fx-text-fill: #ccc; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 2 6;"));
        delBtn.setOnAction(e -> {
            controller.removeEvent(event.getId());
            refreshList();
            if (onChanged != null) onChanged.run();
        });

        row.getChildren().addAll(content, delBtn);
        return row;
    }

    public void show() {
        dialog.showAndWait();
    }

    /** 读取保存的待办字体（含空值/无效值校验，fallback 到 System） */
    public static String getSavedFontFamily() {
        String family = PREFS.get(KEY_FONT_FAMILY, "Microsoft YaHei");
        if (family == null || family.trim().isEmpty()) {
            return "Microsoft YaHei";
        }
        // 尝试解析字体，如果无效则 fallback
        Font testFont = Font.font(family.trim(), 13);
        if (testFont == null || testFont.getFamily().contains("System")) {
            // 字体可能不可用，但保留用户选择，JavaFX 会自动 fallback
        }
        return family.trim();
    }

    /** 保存待办字体 */
    public static void setFontFamily(String family) {
        PREFS.put(KEY_FONT_FAMILY, family);
    }

    /** 读取保存的待办字号 */
    public static int getSavedFontSize() {
        return PREFS.getInt(KEY_FONT_SIZE, 13);
    }

    /** 保存待办字号 */
    public static void setFontSize(int size) {
        PREFS.putInt(KEY_FONT_SIZE, size);
    }
}
