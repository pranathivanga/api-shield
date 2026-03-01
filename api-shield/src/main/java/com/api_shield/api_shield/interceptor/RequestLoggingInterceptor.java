package com.api_shield.api_shield.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final int LIMIT = 5;
    private static final int WINDOW_SECONDS = 60;
    private static final int IDEMPOTENCY_TTL_SECONDS = 300;

    private final RedisTemplate<String, String> redisTemplate;

    public RequestLoggingInterceptor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {


        // 🔹 Rate limiting
        String path = request.getRequestURI();
        String rateKey = "rate_limit:" + path;

        Long count = redisTemplate.opsForValue().increment(rateKey);

        if (count != null && count == 1) {
            redisTemplate.expire(rateKey, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count > LIMIT) {
            Long retryAfter = redisTemplate.getExpire(rateKey, TimeUnit.SECONDS);

            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            String body = String.format(
                    "{\"status\":429,\"error\":\"RATE_LIMIT_EXCEEDED\",\"retryAfterSeconds\":%d}",
                    retryAfter
            );

            response.getWriter().write(body);
            return false;
        }

        return true;
    }
    
}