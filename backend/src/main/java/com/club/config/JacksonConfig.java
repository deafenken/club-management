package com.club.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * 全局 Jackson 定制：
 *  1) LocalDateTime 反序列化同时接受 "yyyy-MM-dd HH:mm:ss"（前端 el-date-picker 默认）
 *     和 ISO "yyyy-MM-ddTHH:mm:ss" 两种格式；序列化统一输出空格格式。
 *  2) 允许 Boolean -> Integer 的强制转换（前端复选框 true/false 写入 0/1 字段），
 *     避免 isFee / needVenue / agreedDamageClause / isValuable 等字段 400。
 * 使用 Customizer 以保留 Spring Boot 默认配置（如 FAIL_ON_UNKNOWN_PROPERTIES=false）。
 */
@Configuration
public class JacksonConfig {

    /** 反序列化：yyyy-MM-dd 后可接 'T' 或空格，秒可选 */
    private static final DateTimeFormatter FLEXIBLE_DATETIME = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .appendPattern("HH:mm")
            .optionalStart().appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd()
            .toFormatter();

    /** 序列化：统一输出空格格式 */
    private static final DateTimeFormatter OUT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer clubJacksonCustomizer() {
        return builder -> {
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(FLEXIBLE_DATETIME));
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(OUT_DATETIME));
            builder.postConfigurer(mapper ->
                mapper.coercionConfigFor(LogicalType.Integer)
                      .setCoercion(CoercionInputShape.Boolean, CoercionAction.TryConvert));
        };
    }
}
