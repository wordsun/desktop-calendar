package com.desktop.calendar.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * 国际化工具类
 * 支持中文(zh)和英文(en)切换，持久化到 Preferences
 */
public class I18n {

    private static final Preferences PREFS = Preferences.userNodeForPackage(I18n.class);
    private static final String KEY_LANG = "language";

    private static Locale currentLocale;

    static {
        String saved = PREFS.get(KEY_LANG, "zh");
        currentLocale = "en".equals(saved) ? Locale.ENGLISH : Locale.CHINESE;
    }

    /** 获取当前 Locale */
    public static Locale getLocale() {
        return currentLocale;
    }

    /** 获取当前语言代码 */
    public static String getLang() {
        return "en".equals(currentLocale.getLanguage()) ? "en" : "zh";
    }

    /** 设置语言并持久化 */
    public static void setLang(String lang) {
        currentLocale = "en".equals(lang) ? Locale.ENGLISH : Locale.CHINESE;
        PREFS.put(KEY_LANG, lang);
    }

    /** 获取国际化文本 */
    public static String get(String key) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", currentLocale);
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }
}
