package com.club.util;

import com.google.gson.*;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson 工具。
 * 1) 注册 LocalDateTime 的序列化 / 反序列化（Gson 默认不会处理 java.time 类型）。
 * 2) disableHtmlEscaping 避免 &lt; 被转成 &amp;lt; 让公告 HTML 渲染异常。
 * Gson 实例线程安全且重量级，全程一个静态实例。
 */
public class JsonUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src,t,ctx) ->
                            new JsonPrimitive(src.format(FMT)))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (j, t, ctx) ->
                            LocalDateTime.parse(j.getAsString(), FMT))
            .disableHtmlEscaping()
            .create();

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> T fromJson(Reader reader, Class<T> clazz) throws IOException {
        return GSON.fromJson(reader, clazz);
    }

    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    public static <T> T fromJson(Reader reader, Type type) {
        return GSON.fromJson(reader, type);
    }

    private JsonUtil() {}
}
