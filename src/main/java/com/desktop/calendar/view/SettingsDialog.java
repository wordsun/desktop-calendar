package com.desktop.calendar.view;

import com.desktop.calendar.service.AutoStartService;
import com.desktop.calendar.service.GlobalHotkeyService;
import com.desktop.calendar.service.TrayService;
import com.desktop.calendar.util.I18n;
import com.desktop.calendar.util.ThemeManager;
import com.desktop.calendar.util.ThemeManager.ThemeColor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

    private TextField hotkeyField;
    private CheckBox autoStartCheck;
    private Slider opacitySlider;
    private Label opacityValueLabel;
    private ToggleGroup langGroup;
    private RadioButton langZhRadio;
    private RadioButton langEnRadio;
    private ThemeColor selectedTheme;

    private int capturedModifiers;
    private int capturedVk;

    public SettingsDialog(Stage owner, GlobalHotkeyService hotkeyService,
                          TrayService trayService, Runnable onSettingsChanged) {
        this.mainStage = owner;
        this.hotkeyService = hotkeyService;
        this.trayService = trayService;
        this.onSettingsChanged = onSettingsChanged;

        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(I18n.get("settings.title"));
        dialog.setResizable(false);

        selectedTheme = ThemeManager.getCurrentTheme();

        VBox root = buildUI();
        Scene scene = new Scene(root, 310, 420, Color.TRANSPARENT);
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
                themeLabel, themeRow, buttonRow);
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
