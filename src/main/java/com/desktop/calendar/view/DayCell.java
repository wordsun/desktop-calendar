package com.desktop.calendar.view;

import com.desktop.calendar.model.HolidayConfig;
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
 * 根据 DayType 枚举直接映射颜色，避免多重 if-else
 */
public class DayCell {

    /** 调休工作日绿色 */
    private static final String ADJUSTED_WORKDAY_COLOR = "#27AE60";

    private final VBox container;

    public DayCell(LocalDate date, LunarDate lunarDate, boolean isCurrentMonth,
                   boolean hasEvent, HolidayConfig.DayType dayType) {
        container = new VBox(2);
        container.setAlignment(Pos.CENTER);
        container.setPrefSize(48, 62);
        container.setStyle("-fx-background-radius: 6; -fx-cursor: hand;");

        boolean isToday = DateUtil.isToday(date);

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
            } else {
                // 根据 DayType 枚举直接映射颜色
                dayText.setFill(switch (dayType) {
                    case HOLIDAY -> Color.web("#E74C3C");          // 法定假日：红色
                    case ADJUSTED_WORKDAY -> Color.web(ADJUSTED_WORKDAY_COLOR); // 调休补班：绿色
                    case NORMAL_WEEKEND -> Color.web(StyleUtil.WEEKEND_COLOR);  // 周末：红色
                    case NORMAL_WORKDAY -> Color.web("#333333");   // 工作日：深灰
                });
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
            } else if (dayType == HolidayConfig.DayType.HOLIDAY) {
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
        if (dayType == HolidayConfig.DayType.ADJUSTED_WORKDAY) {
            // 调休工作日：显示绿色 "班"
            Text mark = new Text("班");
            mark.setFont(Font.font("Microsoft YaHei", 8));
            mark.setFill(Color.web(ADJUSTED_WORKDAY_COLOR));
            container.getChildren().add(mark);
        } else if (dayType == HolidayConfig.DayType.HOLIDAY && isCurrentMonth) {
            // 法定假日：显示红色 "休"
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
