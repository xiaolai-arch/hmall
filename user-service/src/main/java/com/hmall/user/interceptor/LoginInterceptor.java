package com.hmall.user.interceptor;

import com.hmall.common.utils.UserContext;
import com.hmall.user.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtTool jwtTool;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的 token
        String token = request.getHeader("authorization");
        // 2.去掉 Bearer 前缀（兼容 Knife4j Swagger）
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // 3.校验token（失败则返回401）
        Long userId;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return false;
        }
        // 4.存入上下文
        UserContext.setUser(userId);
        // 5.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理用户
        UserContext.removeUser();
    }
}
