package com.hmall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// 自定义过滤器
// 不是直接 实现 GatewayFilter，而是继承 AbstractGatewayFilterFactory
// 名称 必须和 AbstractGatewayFilterFactory 的泛型一致 必须有有GatewayFilterFactory
@Component
public class PrintAnyGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println("执行了 PrintAnyGatewayFilterFactory 的 pre ...");
                return chain.filter(exchange);
            }
        };
    }
}