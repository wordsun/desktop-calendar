package com.desktop.calendar.view;

import com.desktop.calendar.controller.CalendarController;
import com.desktop.calendar.model.CalendarEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 待办事项编辑弹窗
 * 双击日期时弹出，用于添加/查看该日期的待办事项
 */
public class EventDialog {

    private final Stage dialog;
    private final CalendarController controller;
    private final LocalDate date;
    private final Runnable onSaved;

    public EventDialog(Stage owner, CalendarController controller, LocalDate date, Runnable onSaved) {
        this.controller = controller;
        this.date = date;
        this.onSaved = onSaved;

        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle("添加待办");
        dialog.setResizable(false);

        VBox root = buildUI();
        Scene scene = new Scene(root, 280, 260, Color.TRANSPARENT);
        dialog.setScene(scene);
    }

    private VBox buildUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); " +
                "-fx-background-radius: 12; -fx-border-radius: 12; " +
                "-fx-border-color: #ddd; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");

        // 日期标题
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        Label titleLabel = new Label("📝 " + dateStr);
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#333333"));

        // 事项名称
        Label nameLabel = new Label("待办事项：");
        nameLabel.setFont(Font.font("Microsoft YaHei", 12));
        nameLabel.setTextFill(Color.web("#555555"));

        TextField titleField = new TextField();
        titleField.setPromptText("输入待办事项...");
        titleField.setStyle("-fx-font-size: 13; -fx-padding: 6 8; -fx-background-radius: 6;");

        // 备注
        Label descLabel = new Label("备注（可选）：");
        descLabel.setFont(Font.font("Microsoft YaHei", 12));
        descLabel.setTextFill(Color.web("#555555"));

        TextArea descArea = new TextArea();
        descArea.setPromptText("添加备注...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-font-size: 12; -fx-padding: 6; -fx-background-radius: 6;");

        // 按钮行
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("保存");
        saveBtn.setStyle("-fx-background-color: #4A90D9; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 16;");
        saveBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                titleField.setStyle("-fx-font-size: 13; -fx-padding: 6 8; -fx-border-color: #E74C3C; -fx-border-radius: 6;");
                titleField.setPromptText("请输入待办事项名称！");
                return;
            }
            CalendarEvent event = CalendarEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .title(title)
                    .description(descArea.getText().trim())
                    .date(date)
                    .allDay(true)
                    .build();
            controller.addEvent(event);
            if (onSaved != null) onSaved.run();
            dialog.close();
        });

        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle("-fx-background-color: #eee; -fx-text-fill: #555; " +
                "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 16;");
        cancelBtn.setOnAction(e -> dialog.close());

        buttonRow.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(titleLabel, nameLabel, titleField, descLabel, descArea, buttonRow);
        return root;
    }

    public void show() {
        dialog.showAndWait();
    }
}
