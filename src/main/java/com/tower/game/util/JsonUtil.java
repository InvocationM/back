package com.tower.game.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;

/**
 * JSON 工具类，封装 Jackson ObjectMapper，提供对象与 JSON 字符串互转等常用方法。
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = createDefaultMapper();

    private JsonUtil() {
    }

    private static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * 获取内部 ObjectMapper 实例，便于需要自定义配置时使用。
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * 对象转 JSON 字符串。
     *
     * @param obj 任意对象，可为 null
     * @return JSON 字符串，obj 为 null 时返回 "null"
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("对象转 JSON 失败: " + obj.getClass().getName(), e);
        }
    }

    /**
     * 对象转格式化的 JSON 字符串（带缩进，便于调试）。
     */
    public static String toJsonStringPretty(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("对象转 JSON 失败: " + obj.getClass().getName(), e);
        }
    }

    /**
     * JSON 字符串转指定类型对象。
     *
     * @param json  JSON 字符串，null 或空白会抛异常
     * @param clazz 目标类型
     * @return 解析后的对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("json 不能为空");
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 解析失败，目标类型: " + clazz.getName(), e);
        }
    }

    /**
     * JSON 字符串转 JsonNode（树形结构，适合动态读取字段）。
     *
     * @param json JSON 字符串，null 或空白会抛异常
     * @return JsonNode
     */
    public static JsonNode parseObject(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("json 不能为空");
        }
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 解析失败", e);
        }
    }

    /**
     * 使用 TypeReference 解析复杂泛型类型，例如 List&lt;User&gt;、Map&lt;String, User&gt;。
     *
     * @param json         JSON 字符串
     * @param typeReference 如 new TypeReference&lt;List&lt;User&gt;&gt;() {}
     * @return 解析后的对象
     */
    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("json 不能为空");
        }
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 解析失败", e);
        }
    }

    /**
     * 从输入流读取并解析为指定类型。
     */
    public static <T> T parseObject(InputStream in, Class<T> clazz) {
        if (in == null) {
            throw new IllegalArgumentException("InputStream 不能为空");
        }
        try {
            return MAPPER.readValue(in, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("从流解析 JSON 失败，目标类型: " + clazz.getName(), e);
        }
    }

    /**
     * 安全解析：解析失败时返回 null，不抛异常。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @return 解析结果，失败或 json 为空时返回 null
     */
    public static <T> T parseObjectOrNull(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
