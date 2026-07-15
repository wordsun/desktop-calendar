package com.desktop.calendar.view;

import com.desktop.calendar.model.LunarDate;
import com.desktop.calendar.util.DateUtil;
import com.desktop.calendar.util.StyleUtil;
import com.desktop.calendar.util.ThemeManager;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * 日期单元格
 * 渲染单个日期格子：公历日期(大)、农历/节气/节日(小)、假日/调休标记
 */
public class DayCell {

    private final VBox container;

    public DayCell(LocalDate date, LunarDate lunarDate, boolean isCurrentMonth,
                   boolean hasEvent, boolean isHoliday, boolean isWorkday) {
        container = new VBox(2);
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(48, 62);
        container.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");

        boolean isToday = DateUtil.isToday(date);
        boolean isWeekend = DateUtil.isWeekend(date);

        // 公历日期
        Text dayText = new Text(String.valueOf(date.getDayOfMonth()));
        dayText.setFont(Font.font("Microsoft YaHei", isToday ? FontWeight.BOLD : FontWeight.NORMAL,
                StyleUtil.FONT_SIZE_NORMAL));

        if (isToday) {
            // 今日高亮：主题色圆形背景
            String themeColor = ThemeManager.getCurrentColorHex();
            Circle highlight = new Circle(19);
            highlight.setFill(Color.web(themeColor));
            dayText.setFill(Color.WHITE);
            StackPane dayPane = new StackPane(highlight, dayText);
            dayPane.setAlignment(Pos.CENTER);
            container.getChildren().add(dayPane);
        } else {
            if (!isCurrentMonth) {
                dayText.setFill(Color.web("#CCCCCC"));
            } else if (isHoliday && !isWorkday) {
                // 法定假日/传统节日/节气：标红
                dayText.setFill(Color.web("#E74C3C"));
            } else if (isWorkday && isWeekend) {
                // 调休工作日（周末但要上班）：橙色
                dayText.setFill(Color.web("#E67E22"));
            } else if (isWeekend) {
                // 普通周末：红色
                dayText.setFill(Color.web(StyleUtil.WEEKEND_COLOR));
            } else {
                dayText.setFill(Color.web("#333333"));
            }
            container.getChildren().add(dayText);
        }

        // 农历/节日/节气信息
        if (lunarDate != null) {
            Text lunarText = new Text(lunarDate.getShortDisplay());
            lunarText.setFont(Font.font("Microsoft YaHei", StyleUtil.FONT_SIZE_SMALL - 1));

            if (lunarDate.getFestival() != null) {
                // 农历节日：红色加粗
                lunarText.setFill(Color.web("#E74C3C"));
                lunarText.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, StyleUtil.FONT_SIZE_SMALL - 1));
            } else if (lunarDate.getSolarTerm() != null) {
                // 节气：草绿色
                lunarText.setFill(Color.web(StyleUtil.SOLAR_TERM_COLOR));
            } else if (isHoliday && !isWorkday) {
                // 法定假日：红色
                lunarText.setFill(Color.web("#E74C3C"));
            } else {
                lunarText.setFill(Color.web(StyleUtil.LUNAR_COLOR));
            }

            if (!isCurrentMonth) {
                lunarText.setFill(Color.web("#DDDDDD"));
            }

            container.getChildren().add(lunarText);
        }

        // 底部标记行
        if (isWorkday && isWeekend) {
            // 调休工作日：显示 "班"
            Text mark = new Text("班");
            mark.setFont(Font.font("Microsoft YaHei", 8));
            mark.setFill(Color.web("#E67E22"));
            container.getChildren().add(mark);
        } else if (isHoliday && !isWorkday && isCurrentMonth) {
            // 假日：显示 "休"
            Text mark = new Text("休");
            mark.setFont(Font.font("Microsoft YaHei", 8));
            mark.setFill(Color.web("#E74C3C"));
            container.getChildren().add(mark);
        } else if (hasEvent) {
            // 有日程事件：红点
            Circle dot = new Circle(2.5);
            dot.setFill(Color.web("#E74C3C"));
            StackPane dotPane = new StackPane(dot);
            dotPane.setAlignment(Pos.CENTER);
            container.getChildren().add(dotPane);
        }

        // 悬停效果
        final String normalStyle = "-fx-background-radius: 6; -fx-cursor: hand;";
        final String hoverStyle = "-fx-background-color: " + ThemeManager.getCurrentTheme().withAlpha(0.15) + "; " +
                "-fx-background-radius: 6; -fx-cursor: hand;";
        container.setOnMouseEntered(e -> {
            if (!isToday) container.setStyle(hoverStyle);
        });
        container.setOnMouseExited(e -> container.setStyle(normalStyle));
    }

    public VBox getView() {
        return container;
    }
}
