package com.hmall.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// 全局过滤器
@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        System.out.println("执行了MyGlobalFilter 过滤器 pre....");
        System.out.println(request);
        // 将请求对象继续向下传递，其他过滤器继续处理
        // return chain.filter(exchange);
        // 放行
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            System.out.println("执行了MyGlobalFilter 过滤器 post....");
        }));
    }

    @Override
    public int getOrder() {
        // 优先级, 越小越先执行
        return 0;
    }


}
