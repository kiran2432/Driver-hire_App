package com.driverhire.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Small fixed-window, per-IP limiter for the unauthenticated /api/auth/** endpoints
 * (slows down credential stuffing and signup abuse).
 * <p>
 * It is in-memory, so each instance counts separately. When running several replicas,
 * put a shared limiter in front (API gateway / ingress / WAF) or swap this for a Redis-backed one.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private static final class Counter {
        private final long windowStart;
        private int count;

        private Counter(long windowStart) {
            this.windowStart = windowStart;
            this.count = 1;
        }
    }

    private final int maxPerWindow;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(int maxPerMinute) {
        this.maxPerWindow = maxPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        String key = request.getRemoteAddr();

        Counter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                return new Counter(now);
            }
            existing.count++;
            return existing;
        });

        if (counters.size() > CLEANUP_THRESHOLD) {
            counters.values().removeIf(c -> now - c.windowStart >= WINDOW_MS);
        }

        if (counter.count > maxPerWindow) {
            long retryAfterSeconds = Math.max(1, (WINDOW_MS - (now - counter.windowStart)) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(
                    "{\"title\":\"Too Many Requests\",\"status\":429,"
                            + "\"detail\":\"Too many requests. Please try again shortly.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
