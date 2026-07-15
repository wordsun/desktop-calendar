package com.desktop.calendar.service;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * 系统托盘服务
 * 负责创建系统托盘图标、右键菜单
 * 支持：显示/隐藏切换、开机自启切换、退出
 * 双击托盘图标恢复窗口
 */
public class TrayService {

    private final Stage primaryStage;
    private TrayIcon trayIcon;
    private MenuItem showHideItem;
    private CheckboxMenuItem autoStartItem;

    public TrayService(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * 初始化系统托盘
     */
    public void init() {
        if (!SystemTray.isSupported()) {
            System.err.println("系统不支持托盘功能");
            return;
        }

        Platform.setImplicitExit(false);

        try {
            // 创建托盘图标
            java.net.URL iconUrl = getClass().getResource("/icons/calendar.png");
            Image image;
            if (iconUrl != null) {
                image = Toolkit.getDefaultToolkit().createImage(iconUrl);
            } else {
                // 动态生成图标
                image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = ((java.awt.image.BufferedImage) image).createGraphics();
                g.setColor(java.awt.Color.decode("#4A90D9"));
                g.fillOval(0, 0, 16, 16);
                g.setColor(java.awt.Color.WHITE);
                g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 11));
                g.drawString("C", 3, 12);
                g.dispose();
            }

            trayIcon = new TrayIcon(image, "Desktop Calendar");
            trayIcon.setImageAutoSize(true);

            // 双击托盘图标恢复窗口
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                if (!primaryStage.isShowing()) {
                    primaryStage.show();
                    primaryStage.toFront();
                }
                updateShowHideText();
            }));

            // 创建右键菜单
            PopupMenu popup = new PopupMenu();

            // Show/Hide toggle
            showHideItem = new MenuItem("Hide Calendar");
            showHideItem.addActionListener(e -> Platform.runLater(() -> {
                if (primaryStage.isShowing()) {
                    WindowManager.savePosition(primaryStage);
                    primaryStage.hide();
                } else {
                    primaryStage.show();
                    primaryStage.toFront();
                }
                updateShowHideText();
            }));

            // Auto start
            autoStartItem = new CheckboxMenuItem("Auto Start");
            autoStartItem.setState(AutoStartService.isAutoStart());
            autoStartItem.addItemListener(e -> {
                boolean enable = (e.getStateChange() == ItemEvent.SELECTED);
                AutoStartService.setAutoStart(enable);
            });

            // Exit
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                try {
                    removeTrayIcon();
                } catch (Exception ignored) {}
                Platform.exit();
                System.exit(0);
            });

            popup.add(showHideItem);
            popup.add(autoStartItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);
            SystemTray.getSystemTray().add(trayIcon);

        } catch (Exception e) {
            System.err.println("初始化系统托盘失败: " + e.getMessage());
        }
    }

    /**
     * 更新"显示/隐藏"菜单文本
     */
    private void updateShowHideText() {
        if (showHideItem != null) {
            showHideItem.setLabel(primaryStage.isShowing() ? "Hide Calendar" : "Show Calendar");
        }
    }

    /**
     * 刷新开机自启复选框状态
     */
    public void refreshAutoStartState() {
        if (autoStartItem != null) {
            autoStartItem.setState(AutoStartService.isAutoStart());
        }
    }

    /**
     * 移除托盘图标
     */
    public void removeTrayIcon() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }
}
