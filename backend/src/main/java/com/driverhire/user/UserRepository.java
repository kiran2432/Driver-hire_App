package com.driverhire.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Emails are stored lower-cased; callers must normalise before querying. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
