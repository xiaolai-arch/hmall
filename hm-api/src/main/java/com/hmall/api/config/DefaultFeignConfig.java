package com.hmall.api.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    // 注册feign日杂记录级别：none->basic->headers->full
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.FULL;
    }
}
