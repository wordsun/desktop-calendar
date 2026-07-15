package com.desktop.calendar.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON工具类
 * 封装Gson操作，提供文件读写JSON的便捷方法
 */
public class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd")
            .create();

    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /**
     * JSON字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * JSON字符串转泛型对象
     */
    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    /**
     * 从文件读取JSON对象
     */
    public static <T> T readFromFile(Path filePath, Class<T> clazz) throws IOException {
        if (!Files.exists(filePath)) {
            return null;
        }
        String json = Files.readString(filePath, StandardCharsets.UTF_8);
        return GSON.fromJson(json, clazz);
    }

    /**
     * 从文件读取JSON泛型对象
     */
    public static <T> T readFromFile(Path filePath, Type type) throws IOException {
        if (!Files.exists(filePath)) {
            return null;
        }
        String json = Files.readString(filePath, StandardCharsets.UTF_8);
        return GSON.fromJson(json, type);
    }

    /**
     * 写入JSON到文件
     */
    public static void writeToFile(Path filePath, Object obj) throws IOException {
        Files.createDirectories(filePath.getParent());
        String json = GSON.toJson(obj);
        Files.writeString(filePath, json, StandardCharsets.UTF_8);
    }

    /**
     * 获取TypeToken的Type
     */
    public static Type getType(TypeToken<?> typeToken) {
        return typeToken.getType();
    }
}
