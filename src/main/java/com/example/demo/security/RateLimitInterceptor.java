package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * IP 频率限制拦截器
 * <p>
 * 实现方式：内存（ConcurrentHashMap + ConcurrentLinkedDeque）
 * 按 IP 记录最近请求的时间戳队列，同一 IP 每分钟最多允许 maxRequests 次请求。
 * 超过限制返回 HTTP 429 Too Many Requests。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    /** 时间窗口：1 分钟（毫秒） */
    private static final long WINDOW_MS = 60_000L;

    /** 窗口内最大请求数 */
    private static final int MAX_REQUESTS = 5;

    /** IP -> 最近请求时间戳队列 */
    private final Map<String, Deque<Long>> requestLogs = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        Deque<Long> timestamps = requestLogs.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

        // 移除超过时间窗口的旧时间戳
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS) {
            log.warn("IP 频率限制触发: ip={}, path={}", clientIp, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"注册请求过于频繁，请稍后再试\"}");
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    /**
     * 获取客户端真实 IP，考虑代理头
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // X-Forwarded-For 可能包含多个 IP，取第一个
            ip = ip.split(",")[0].trim();
            if (!ip.isEmpty()) {
                return ip;
            }
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
