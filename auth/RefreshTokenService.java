package com.driverhire.auth;

import com.driverhire.common.ApiException;
import com.driverhire.config.AppProperties;
import com.driverhire.user.User;
import com.driverhire.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and rotates opaque refresh tokens.
 * <ul>
 *   <li>Every refresh invalidates the presented token and issues a new one (rotation).</li>
 *   <li>Presenting an already-used token means it was probably stolen, so all of that user's
 *       sessions are revoked (reuse detection).</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** A freshly issued raw token (only ever held in memory and in the client's cookie) and its owner. */
    public record Rotation(User user, String newToken) {
    }

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final AppProperties props;

    public RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, AppProperties props) {
        this.tokens = tokens;
        this.users = users;
        this.props = props;
    }

    public Duration ttl() {
        return props.jwt().refreshTtl();
    }

    /** Creates and stores a new token for the user and returns the raw value to hand to the client. */
    @Transactional
    public String issue(Long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(new RefreshToken(userId, hash(raw), Instant.now().plus(ttl())));
        return raw;
    }

    /**
     * Validates the presented token, revokes it and issues its replacement.
     * {@code noRollbackFor} matters: when reuse is detected the revocation of the user's sessions
     * must be committed even though a 401 is thrown.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken stored = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.unauthorized("Session expired, please sign in again"));

        if (stored.isRevoked()) {
            int revoked = tokens.revokeAllForUser(stored.getUserId());
            log.warn("Refresh token reuse detected for user {}; revoked {} active session(s)",
                    stored.getUserId(), revoked);
            throw ApiException.unauthorized("Session expired, please sign in again");
        }
        if (stored.isExpired(Instant.now())) {
            throw ApiException.unauthorized("Session expired, please sign in again");
        }

        User user = users.findById(stored.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.unauthorized("Session expired, please sign in again"));

        stored.revoke();
        return new Rotation(user, issue(user.getId()));
    }

    /** Revokes one token (logout). Unknown tokens are ignored. */
    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    /** Housekeeping: removes tokens that expired more than a day ago. */
    @Transactional
    public int purgeExpired() {
        return tokens.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
