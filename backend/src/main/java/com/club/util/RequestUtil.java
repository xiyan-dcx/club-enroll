package com.club.util;

import com.google.gson.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;

/** 统一 JSON 类型校验，避免缺少字段、错误类型或小数 ID 进入业务层。 */
public final class RequestUtil {
    private RequestUtil() {}

    public static JsonObject body(HttpServletRequest req) throws IOException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json"))
            throw new IllegalArgumentException("请求须使用 application/json");
        try {
            JsonElement json = JsonParser.parseReader(req.getReader());
            if (!json.isJsonObject()) throw new IllegalArgumentException("请求体必须是 JSON 对象");
            return json.getAsJsonObject();
        } catch (JsonParseException ex) {
            throw new IllegalArgumentException("JSON 格式错误");
        }
    }

    public static String string(JsonObject body, String key) {
        JsonElement value = body.get(key);
        if (value == null || value.isJsonNull()) return null;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException(key + " 必须是字符串");
        return value.getAsString();
    }

    public static long id(JsonObject body, String key) {
        JsonElement value = body.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException(key + " 必须是正整数");
        return positiveId(value.getAsString(), key);
    }

    public static long positiveId(String value, String key) {
        try {
            long id = new BigDecimal(value).longValueExact();
            if (id <= 0) throw new ArithmeticException();
            return id;
        } catch (ArithmeticException | NumberFormatException | NullPointerException ex) {
            throw new IllegalArgumentException(key + " 必须是正整数");
        }
    }

    public static int offset(int page, int size) {
        long offset = (long) (page - 1) * size;
        if (offset > Integer.MAX_VALUE) throw new IllegalArgumentException("页码超出范围");
        return (int) offset;
    }
}
