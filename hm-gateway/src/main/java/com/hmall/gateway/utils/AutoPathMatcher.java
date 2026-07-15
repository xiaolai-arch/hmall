package com.hmall.gateway.utils;

import org.springframework.util.AntPathMatcher;

/**
 * Ant 风格路径匹配器，用于判断请求路径是否匹配配置的路径模式。
 * 支持 ?（匹配单个字符）、*（匹配单层路径）、**（匹配多层路径）。
 */
public class AutoPathMatcher {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 判断路径是否匹配指定的模式。
     *
     * @param pattern Ant 风格路径模式，如 /api/**、/user/{id}
     * @param path    实际请求路径，如 /api/items/1
     * @return true 表示匹配
     */
    public boolean match(String pattern, String path) {
        return antPathMatcher.match(pattern, path);
    }
}