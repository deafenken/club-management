package com.club.llm;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mermaid专用工具类 - LLM输出的图表代码清洗与校验
 * 【答辩讲解点】解决LLM生成Mermaid图表渲染空白/报错的核心问题
 */
@Slf4j
public class MermaidUtil {

    private static final Pattern MERMAID_BLOCK = Pattern.compile(
            "```mermaid\\s*\\n([\\s\\S]*?)```", Pattern.MULTILINE);

    // 常见中文标点→英文标点映射（LLM经常生成中文标点导致渲染失败）
    private static final String[][] CHAR_MAP = {
            {"，", ","}, {"。", "."}, {"：", ":"}, {"；", ";"},
            {"“", "\""}, {"”", "\""}, {"（", "("}, {"）", ")"},
            {"【", "["}, {"】", "]"}, {"！", "!"}, {"？", "?"}
    };

    /** 从LLM混合输出中提取所有Mermaid代码块 */
    public static List<String> extractMermaidBlocks(String raw) {
        List<String> blocks = new ArrayList<>();
        if (StrUtil.isBlank(raw)) return blocks;
        Matcher m = MERMAID_BLOCK.matcher(raw);
        while (m.find()) {
            String code = m.group(1).trim();
            if (!code.isEmpty()) blocks.add(code);
        }
        if (blocks.isEmpty()) {
            for (String line : raw.split("\n")) {
                if (line.trim().startsWith("gantt") || line.trim().startsWith("erDiagram")) {
                    blocks.add(raw.trim()); break;
                }
            }
        }
        log.info("提取到{}个Mermaid代码块", blocks.size());
        return blocks;
    }

    /** 清洗Mermaid代码：全角→半角、去非法字符、补全闭合标签 */
    public static String cleanMermaidCode(String code) {
        if (StrUtil.isBlank(code)) return code;
        String cleaned = code;
        for (String[] pair : CHAR_MAP) {
            cleaned = cleaned.replace(pair[0], pair[1]);
        }
        cleaned = cleaned.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");
        StringBuilder sb = new StringBuilder();
        for (String line : cleaned.split("\n")) {
            sb.append(line.stripTrailing()).append("\n");
        }
        cleaned = sb.toString().trim();
        // 自动补全缺失的闭合花括号
        int open = 0, close = 0;
        for (char c : cleaned.toCharArray()) { if (c=='{') open++; if (c=='}') close++; }
        if (open > close) {
            for (int i = 0; i < open - close; i++) cleaned += "\n}";
            log.warn("Mermaid代码缺少{}个闭合花括号，已自动补全", open-close);
        }
        return cleaned;
    }

    /** Mermaid语法预校验：返回null=合法，返回非空字符串=错误描述 */
    public static String validateMermaid(String code) {
        if (StrUtil.isBlank(code)) return "代码为空";
        String lower = code.toLowerCase().trim();
        if (lower.startsWith("gantt")) {
            if (!lower.contains("title")) return "甘特图缺少title声明";
            if (!lower.contains("section")) return "甘特图缺少section分区";
        }
        if (lower.startsWith("erdiagram")) {
            if (!lower.contains("{") && !lower.contains("|")) return "ER图缺少实体定义";
        }
        return null;
    }

    /** 从LLM输出中提取纯Markdown文本（去掉Mermaid代码块） */
    public static String extractMarkdown(String raw) {
        if (StrUtil.isBlank(raw)) return raw;
        return MERMAID_BLOCK.matcher(raw).replaceAll("").trim();
    }
}
