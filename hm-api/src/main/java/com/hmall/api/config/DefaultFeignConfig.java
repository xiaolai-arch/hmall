package com.hmall.api.config;

import com.hmall.api.client.fallback.CartClientFallback;
import com.hmall.api.client.fallback.ItemClientFallback;
import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    // 注册feign日杂记录级别：none->basic->headers->full
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.FULL;
    }

    // feign请求拦截器
    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 设置线程变量中的用户Id
                Long userId = UserContext.getUser();
                // 设置到feign请求头
                if (userId != null) {
                    template.header("user-info", userId.toString());
                }
            }
        };
    }
    @Bean
    public ItemClientFallback itemClientFallback() {
        return new ItemClientFallback();
    }
    @Bean
    public CartClientFallback cartClientFallback() {
        return new CartClientFallback();
    }
}
