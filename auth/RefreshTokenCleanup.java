package com.driverhire.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Nightly purge of long-expired refresh tokens so the table does not grow forever. */
@Configuration
@EnableScheduling
public class RefreshTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenService tokens;

    public RefreshTokenCleanup(RefreshTokenService tokens) {
        this.tokens = tokens;
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void purge() {
        int removed = tokens.purgeExpired();
        if (removed > 0) {
            log.info("Purged {} expired refresh token(s)", removed);
        }
    }
}
