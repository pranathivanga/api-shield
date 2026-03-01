package com.api_shield.api_shield.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final int LIMIT = 5;
    private static final int WINDOW_SECONDS = 60;

    private final RedisTemplate<String, String> redisTemplate;

    public RequestLoggingInterceptor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String path = request.getRequestURI();
        String key = "rate_limit:" + path;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count > LIMIT) {
            response.setStatus(429);
            response.getWriter().write("Too many requests. Try again later.");
            return false; // BLOCK request
        }

        return true; // ALLOW request
    }
}