package com.desktop.calendar.view;

import com.desktop.calendar.service.AutoStartService;
import com.desktop.calendar.service.GlobalHotkeyService;
import com.desktop.calendar.service.RemarkService;
import com.desktop.calendar.service.TrayService;
import com.desktop.calendar.util.I18n;
import com.desktop.calendar.util.ThemeManager;
import com.desktop.calendar.util.ThemeManager.ThemeColor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * 设置面板
 * 支持：全局快捷键、开机自启、界面透明度、语言切换、主题色切换
 */
public class SettingsDialog {

    private static final Preferences PREFS = Preferences.userNodeForPackage(SettingsDialog.class);
    private static final String KEY_OPACITY = "window_opacity";

    private final Stage dialog;
    private final Stage mainStage;
    private final GlobalHotkeyService hotkeyService;
    private final TrayService trayService;
    private final Runnable onSettingsChanged;
    private final Runnable onResetSize;
    private final Runnable onClearTodos;

    // 拖拽偏移量
    private double dragOffsetX;
    private double dragOffsetY;

    private TextField hotkeyField;
    private CheckBox autoStartCheck;
    private Slider opacitySlider;
    private Label opacityValueLabel;
    private ToggleGroup langGroup;
    private RadioButton langZhRadio;
    private RadioButton langEnRadio;
    private ThemeColor selectedTheme;
    private TextField remarkPathField;
    private ComboBox<String> fontFamilyCombo;
    private ComboBox<Integer> fontSizeCombo;

    private int capturedModifiers;
    private int capturedVk;

    public SettingsDialog(Stage owner, GlobalHotkeyService hotkeyService,
                          TrayService trayService, Runnable onSettingsChanged,
                          Runnable onResetSize, Runnable onClearTodos) {
        this.mainStage = owner;
        this.hotkeyService = hotkeyService;
        this.trayService = trayService;
        this.onSettingsChanged = onSettingsChanged;
        this.onResetSize = onResetSize;
        this.onClearTodos = onClearTodos;

        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(I18n.get("settings.title"));
        dialog.setResizable(false);

        selectedTheme = ThemeManager.getCurrentTheme();

        VBox root = buildUI();
        Scene scene = new Scene(root, 330, 570, Color.TRANSPARENT);
        dialog.setScene(scene);
    }

    private VBox buildUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        String themeHex = ThemeManager.getCurrentColorHex();
        root.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); " +
                "-fx-background-radius: 12; -fx-border-radius: 12; " +
                "-fx-border-color: #ddd; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");

        // 拖拽支持：鼠标拖动整个设置面板
        root.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - dialog.getX();
            dragOffsetY = e.getScreenY() - dialog.getY();
        });
        root.setOnMouseDragged(e -> {
            dialog.setX(e.getScreenX() - dragOffsetX);
            dialog.setY(e.getScreenY() - dragOffsetY);
        });

        // 标题
        Label title = new Label(I18n.get("settings.title"));
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 16));
        title.setTextFill(Color.web("#333333"));

        // 快捷键设置
        Label hotkeyLabel = new Label(I18n.get("settings.hotkey"));
        hotkeyLabel.setFont(Font.font("Microsoft YaHei", 12));
        hotkeyLabel.setTextFill(Color.web("#555555"));

        hotkeyField = new TextField(hotkeyService.getHotkeyText());
        hotkeyField.setEditable(false);
        hotkeyField.setPromptText(I18n.get("settings.hotkey.prompt"));
        hotkeyField.setStyle("-fx-font-size: 13; -fx-padding: 6 8;");

        capturedModifiers = hotkeyService.getCurrentModifiers();
        capturedVk = hotkeyService.getCurrentVk();
        hotkeyField.setOnKeyPressed(this::onKeyPressed);

        // 开机自启
        autoStartCheck = new CheckBox(I18n.get("settings.autostart"));
        autoStartCheck.setFont(Font.font("Microsoft YaHei", 12));
        autoStartCheck.setSelected(AutoStartService.isAutoStart());
        autoStartCheck.setTextFill(Color.web("#333333"));

        // 界面透明度
        Label opacityLabel = new Label(I18n.get("settings.opacity"));
        opacityLabel.setFont(Font.font("Microsoft YaHei", 12));
        opacityLabel.setTextFill(Color.web("#555555"));

        double currentOpacity = PREFS.getDouble(KEY_OPACITY, 0.5);
        opacitySlider = new Slider(0.3, 1.0, currentOpacity);
        opacitySlider.setShowTickMarks(true);
        opacitySlider.setShowTickLabels(false);
        opacitySlider.setMajorTickUnit(0.1);
        opacitySlider.setBlockIncrement(0.05);
        opacitySlider.setPrefWidth(180);

        String pctText = Math.round(currentOpacity * 100) + "%";
        opacityValueLabel = new Label(pctText);
        opacityValueLabel.setFont(Font.font("Microsoft YaHei", 11));
        opacityValueLabel.setTextFill(Color.web(themeHex));
        opacityValueLabel.setPrefWidth(40);

        // 实时预览透明度
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double val = newVal.doubleValue();
            opacityValueLabel.setText(Math.round(val * 100) + "%");
            if (mainStage != null) {
                mainStage.setOpacity(val);
            }
        });

        HBox opacityRow = new HBox(8);
        opacityRow.setAlignment(Pos.CENTER_LEFT);
        opacityRow.getChildren().addAll(opacitySlider, opacityValueLabel);

        // 语言切换
        Label langLabel = new Label(I18n.get("settings.language"));
        langLabel.setFont(Font.font("Microsoft YaHei", 12));
        langLabel.setTextFill(Color.web("#555555"));

        langGroup = new ToggleGroup();
        langZhRadio = new RadioButton(I18n.get("settings.lang.zh"));
        langZhRadio.setToggleGroup(langGroup);
        langZhRadio.setFont(Font.font("Microsoft YaHei", 12));
        langZhRadio.setTextFill(Color.web("#333333"));

        langEnRadio = new RadioButton(I18n.get("settings.lang.en"));
        langEnRadio.setToggleGroup(langGroup);
        langEnRadio.setFont(Font.font("Microsoft YaHei", 12));
        langEnRadio.setTextFill(Color.web("#333333"));

        if ("zh".equals(I18n.getLang())) {
            langZhRadio.setSelected(true);
        } else {
            langEnRadio.setSelected(true);
        }

        HBox langRow = new HBox(16);
        langRow.setAlignment(Pos.CENTER_LEFT);
        langRow.getChildren().addAll(langZhRadio, langEnRadio);

        // 主题色选择
        Label themeLabel = new Label(I18n.get("settings.theme"));
        themeLabel.setFont(Font.font("Microsoft YaHei", 12));
        themeLabel.setTextFill(Color.web("#555555"));

        HBox themeRow = createThemeSelector();

        // 备注目录设置
        Label remarkLabel = new Label(I18n.get("settings.remark"));
        remarkLabel.setFont(Font.font("Microsoft YaHei", 12));
        remarkLabel.setTextFill(Color.web("#555555"));

        remarkPathField = new TextField();
        String savedPath = RemarkService.getSavedRemarkDir();
        remarkPathField.setText(savedPath.isEmpty() ? RemarkService.getRemarkDir().toString() : savedPath);
        remarkPathField.setPromptText(I18n.get("settings.remark.prompt"));
        remarkPathField.setStyle("-fx-font-size: 12; -fx-padding: 6 8; -fx-background-radius: 6;");
        HBox.setHgrow(remarkPathField, Priority.ALWAYS);

        Button browseBtn = new Button("...");
        browseBtn.setStyle("-fx-background-color: " + themeHex + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 4 10;");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(I18n.get("settings.remark.browse"));
            String current = remarkPathField.getText().trim();
            if (!current.isEmpty()) {
                File dir = new File(current);
                if (dir.exists() && dir.isDirectory()) {
                    chooser.setInitialDirectory(dir);
                }
            }
            File selected = chooser.showDialog(dialog);
            if (selected != null) {
                remarkPathField.setText(selected.getAbsolutePath());
            }
        });

        HBox remarkRow = new HBox(6);
        remarkRow.setAlignment(Pos.CENTER_LEFT);
        remarkRow.getChildren().addAll(remarkPathField, browseBtn);

        // 待办字体设置
        Label fontLabel = new Label(I18n.get("settings.todoFont"));
        fontLabel.setFont(Font.font("Microsoft YaHei", 12));
        fontLabel.setTextFill(Color.web("#555555"));

        fontFamilyCombo = new ComboBox<>();
        fontFamilyCombo.getItems().addAll(
                "Microsoft YaHei", "SimSun", "SimHei", "KaiTi", "FangSong",
                "Arial", "Consolas", "Courier New"
        );
        fontFamilyCombo.setValue(TodoListDialog.getSavedFontFamily());
        fontFamilyCombo.setStyle("-fx-font-size: 12; -fx-background-radius: 6;");
        fontFamilyCombo.setPrefWidth(160);

        fontSizeCombo = new ComboBox<>();
        for (int s = 10; s <= 24; s++) fontSizeCombo.getItems().add(s);
        fontSizeCombo.setValue(TodoListDialog.getSavedFontSize());
        fontSizeCombo.setStyle("-fx-font-size: 12; -fx-background-radius: 6;");
        fontSizeCombo.setPrefWidth(70);

        Label fontPreview = new Label(I18n.get("settings.todoFontSize"));
        fontPreview.setFont(Font.font("Microsoft YaHei", 11));
        fontPreview.setTextFill(Color.web("#999999"));

        HBox fontRow = new HBox(8);
        fontRow.setAlignment(Pos.CENTER_LEFT);
        fontRow.getChildren().addAll(fontFamilyCombo, fontSizeCombo, fontPreview);

        // 恢复默认大小按钮
        Button resetSizeBtn = new Button(I18n.get("settings.resetSize"));
        resetSizeBtn.setStyle("-fx-background-color: #eee; -fx-text-fill: #555; " +
                "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 14;");
        resetSizeBtn.setOnAction(e -> {
            if (onResetSize != null) {
                onResetSize.run();
            }
        });

        HBox resetRow = new HBox(resetSizeBtn);
        resetRow.setAlignment(Pos.CENTER_LEFT);

        // 一键清空待办
        Label clearLabel = new Label(I18n.get("settings.clearTodos"));
        clearLabel.setFont(Font.font("Microsoft YaHei", 12));
        clearLabel.setTextFill(Color.web("#555555"));

        Button clearTodosBtn = new Button(I18n.get("settings.clearTodos.button"));
        clearTodosBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 14;");
        clearTodosBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(I18n.get("settings.clearTodos.confirm.title"));
            confirm.setHeaderText(null);
            confirm.setContentText(I18n.get("settings.clearTodos.confirm.text"));
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                if (onClearTodos != null) {
                    onClearTodos.run();
                }
            }
        });

        HBox clearRow = new HBox(clearTodosBtn);
        clearRow.setAlignment(Pos.CENTER_LEFT);

        // 按钮行
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button(I18n.get("settings.save"));
        saveBtn.setStyle("-fx-background-color: " + themeHex + "; -fx-text-fill: white; " +
                "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 16;");
        saveBtn.setOnAction(e -> onSave());

        Button cancelBtn = new Button(I18n.get("settings.cancel"));
        cancelBtn.setStyle("-fx-background-color: #eee; -fx-text-fill: #555; " +
                "-fx-background-radius: 6; -fx-font-size: 12; -fx-cursor: hand; -fx-padding: 6 16;");
        cancelBtn.setOnAction(e -> {
            // 取消时恢复透明度
            if (mainStage != null) {
                mainStage.setOpacity(PREFS.getDouble(KEY_OPACITY, 0.5));
            }
            dialog.close();
        });

        buttonRow.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(title, hotkeyLabel, hotkeyField, autoStartCheck,
                opacityLabel, opacityRow, langLabel, langRow,
                themeLabel, themeRow,
                remarkLabel, remarkRow,
                fontLabel, fontRow, resetRow, clearRow, buttonRow);
        return root;
    }

    /**
     * 创建主题色选择器（5个彩色圆圈）
     */
    private HBox createThemeSelector() {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);

        ThemeColor current = ThemeManager.getCurrentTheme();

        for (ThemeColor tc : ThemeColor.values()) {
            Circle circle = new Circle(14);
            circle.setFill(Color.web(tc.getHex()));
            circle.setStyle("-fx-cursor: hand;");

            // 选中状态：白色边框
            if (tc == current) {
                circle.setStroke(Color.WHITE);
                circle.setStrokeWidth(3);
                circle.setEffect(new javafx.scene.effect.DropShadow(5, Color.web(tc.getHex())));
            }

            circle.setOnMouseClicked(e -> {
                selectedTheme = tc;
                // 更新所有圆圈的选中状态
                for (javafx.scene.Node node : box.getChildren()) {
                    Circle c = (Circle) node;
                    c.setStroke(Color.WHITE);
                    c.setStrokeWidth(0);
                    c.setEffect(null);
                }
                circle.setStroke(Color.WHITE);
                circle.setStrokeWidth(3);
                circle.setEffect(new javafx.scene.effect.DropShadow(5, Color.web(tc.getHex())));
            });

            box.getChildren().add(circle);
        }

        return box;
    }

    private void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.CONTROL || code == KeyCode.SHIFT ||
            code == KeyCode.ALT || code == KeyCode.META) {
            return;
        }

        int modifiers = 0;
        if (event.isControlDown()) modifiers |= 0x0002;
        if (event.isShiftDown()) modifiers |= 0x0004;
        if (event.isAltDown()) modifiers |= 0x0001;
        if (event.isMetaDown()) modifiers |= 0x0008;

        if (modifiers == 0) {
            hotkeyField.setPromptText(I18n.get("settings.hotkey.hint"));
            return;
        }

        capturedModifiers = modifiers;
        capturedVk = code.getCode();

        StringBuilder sb = new StringBuilder();
        if (event.isControlDown()) sb.append("Ctrl+");
        if (event.isShiftDown()) sb.append("Shift+");
        if (event.isAltDown()) sb.append("Alt+");
        if (event.isMetaDown()) sb.append("Win+");
        sb.append(code.getName());
        hotkeyField.setText(sb.toString());
        hotkeyField.setPromptText(I18n.get("settings.hotkey.prompt"));
    }

    private void onSave() {
        // 保存快捷键
        if (capturedModifiers > 0 && capturedVk > 0) {
            hotkeyService.setHotkey(capturedModifiers, capturedVk);
        }

        // 保存开机自启
        AutoStartService.setAutoStart(autoStartCheck.isSelected());
        trayService.refreshAutoStartState();

        // 保存透明度
        double opacity = opacitySlider.getValue();
        PREFS.putDouble(KEY_OPACITY, opacity);
        if (mainStage != null) {
            mainStage.setOpacity(opacity);
        }

        // 保存语言
        String newLang = langEnRadio.isSelected() ? "en" : "zh";
        I18n.setLang(newLang);

        // 保存主题色
        ThemeManager.setTheme(selectedTheme);

        // 保存备注目录路径
        String remarkPath = remarkPathField.getText().trim();
        RemarkService.setRemarkDir(remarkPath);

        // 保存待办字体设置
        if (fontFamilyCombo.getValue() != null) {
            TodoListDialog.setFontFamily(fontFamilyCombo.getValue());
        }
        if (fontSizeCombo.getValue() != null) {
            TodoListDialog.setFontSize(fontSizeCombo.getValue());
        }

        // 通知视图刷新
        if (onSettingsChanged != null) {
            onSettingsChanged.run();
        }

        dialog.close();
    }

    /**
     * 读取保存的透明度值
     */
    public static double getSavedOpacity() {
        return PREFS.getDouble(KEY_OPACITY, 0.5);
    }

    public void show() {
        dialog.showAndWait();
    }
}
