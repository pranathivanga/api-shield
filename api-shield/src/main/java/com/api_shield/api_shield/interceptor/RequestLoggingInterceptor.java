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

    private static final int WINDOW_SECONDS = 60;
    private static final int MAX_VIOLATIONS = 3;
    private static final int BAN_SECONDS = 300;

    private final RedisTemplate<String, String> redisTemplate;

    public RequestLoggingInterceptor(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {

        // 🔹 Extract user info ONCE
        String userId = request.getHeader("X-User-Id");
        String userTier = request.getHeader("X-User-Tier");

        if (userId == null || userId.isBlank()) {
            userId = "anonymous";
        }

        if (userTier == null || userTier.isBlank()) {
            userTier = "FREE";
        }

        // 🔴 1. Check if user is banned
        String banKey = "banned:" + userId;
        Boolean isBanned = redisTemplate.hasKey(banKey);

        if (Boolean.TRUE.equals(isBanned)) {
            Long ttl = redisTemplate.getExpire(banKey, TimeUnit.SECONDS);

            response.setStatus(403);
            response.setContentType("application/json");

            String body = String.format(
                    "{\"status\":403,\"error\":\"USER_TEMPORARILY_BLOCKED\",\"retryAfterSeconds\":%d}",
                    ttl
            );

            response.getWriter().write(body);
            return false;
        }

        // 🔹 2. Rate limiting (user-based)
        int userLimit = getLimitForTier(userTier);

        String path = request.getRequestURI();
        String rateKey = "rate_limit:" + userId + ":" + path;

        Long count = redisTemplate.opsForValue().increment(rateKey);

        if (count != null && count == 1) {
            redisTemplate.expire(rateKey, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count > userLimit) {

            // 🔴 3. Track violations
            String violationKey = "violation:" + userId;
            Long violations = redisTemplate.opsForValue().increment(violationKey);

            if (violations != null && violations == 1) {
                redisTemplate.expire(violationKey, 600, TimeUnit.SECONDS);
            }

            // 🔴 4. Ban if too many violations
            if (violations != null && violations > MAX_VIOLATIONS) {

                redisTemplate.opsForValue()
                        .set(banKey, "true", BAN_SECONDS, TimeUnit.SECONDS);

                response.setStatus(403);
                response.setContentType("application/json");

                String body = String.format(
                        "{\"status\":403,\"error\":\"USER_TEMPORARILY_BLOCKED\",\"banDurationSeconds\":%d}",
                        BAN_SECONDS
                );

                response.getWriter().write(body);
                return false;
            }

            // 🔹 5. Normal rate limit response
            Long retryAfter = redisTemplate.getExpire(rateKey, TimeUnit.SECONDS);

            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            String body = String.format(
                    "{\"status\":429,\"error\":\"RATE_LIMIT_EXCEEDED\",\"tier\":\"%s\",\"limit\":%d,\"violations\":%d,\"retryAfterSeconds\":%d}",
                    userTier,
                    userLimit,
                    violations,
                    retryAfter
            );

            response.getWriter().write(body);
            return false;
        }

        return true;
    }

    private int getLimitForTier(String tier) {
        return switch (tier.toUpperCase()) {
            case "PREMIUM" -> 20;
            case "FREE" -> 5;
            default -> 5;
        };
    }
}