package com.hmall.gateway.filter;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.config.JwtProperties;
import com.hmall.gateway.utils.AutoPathMatcher;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final AutoPathMatcher autoPathMatcher = new AutoPathMatcher();
    private final JwtTool jwtTool;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 判断是否需要进行登录请求
        ServerHttpRequest request = exchange.getRequest();
        if (isExclude(request.getPath().toString())) {
            // 如果不是需要登录的地址放行，比如：登录、搜索商品
            return chain.filter(exchange);
        }

        // 2. 如果需要登录；获取令牌；获取请求头属性authorization 的值 就是令牌
        String token = request.getHeaders().getFirst("Authorization");

        // 3. 校验令牌，获取用户信息
        try {
            Long userId = jwtTool.parseToken(token);

            // 4. 将用户信息传递到后端服务
            System.out.println("用户id：" + userId);
            // 将用户id设置到请求头，改写request对象，设置请求头到后端服务
            exchange.mutate().request(builder -> {
                    builder.header("user-info", userId.toString()).build();
            });


        } catch (Exception e) {
            // 校验不通过则返回，没有授权：401
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }


        // 校验通过放行
        return chain.filter(exchange);
    }

    private boolean isExclude(String path) {
        for (String excludePath : authProperties.getExcludePaths()) {
            if (autoPathMatcher.match(excludePath, path)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public int getOrder() {
        return 0;
    }

}
