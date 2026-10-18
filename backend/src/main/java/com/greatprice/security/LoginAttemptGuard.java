package com.greatprice.security;

import com.greatprice.common.api.ErrorCode;
import com.greatprice.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** 按登录名限制连续失败次数，覆盖不存在的用户名。账号锁定仍以数据库为准。 */
@Component
public class LoginAttemptGuard {

    static final int MAX_ATTEMPTS = 8;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public void check(String username) {
        Attempt attempt = attempts.get(key(username));
        if (attempt != null && attempt.blocked(Instant.now())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录失败次数过多，请 15 分钟后再试");
        }
    }

    public void fail(String username) {
        attempts.compute(key(username), (name, current) -> {
            Instant now = Instant.now();
            Attempt attempt = current == null || current.expired(now) ? new Attempt() : current;
            attempt.failures++;
            attempt.windowStart = attempt.windowStart == null ? now : attempt.windowStart;
            if (attempt.failures >= MAX_ATTEMPTS) {
                attempt.blockedUntil = now.plus(WINDOW);
            }
            return attempt;
        });
    }

    public void success(String username) {
        attempts.remove(key(username));
    }

    public void reset() {
        attempts.clear();
    }

    private static String key(String username) {
        return username == null ? "" : username.strip();
    }

    private static final class Attempt {
        private int failures;
        private Instant windowStart;
        private Instant blockedUntil;

        private boolean expired(Instant now) {
            return windowStart != null && windowStart.plus(WINDOW).isBefore(now) && !blocked(now);
        }

        private boolean blocked(Instant now) {
            return blockedUntil != null && blockedUntil.isAfter(now);
        }
    }
}
