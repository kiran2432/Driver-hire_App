package com.driverhire.driver;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<DriverProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<DriverProfile> findDetailedById(Long id);

    /**
     * Available, enabled drivers, most experienced first.
     * Optional filters use neutral values instead of NULL parameters (portable across PostgreSQL and H2):
     * city "" = any city, flags false = no transmission requirement, minExperience 0 = no minimum.
     *
     * @param city lower-cased city or ""
     */
    @Query(value = """
            select d from DriverProfile d join fetch d.user u
            where d.available = true and u.enabled = true
              and (:city = '' or lower(d.city) = :city)
              and (:needManual = false or d.drivesManual = true)
              and (:needAutomatic = false or d.drivesAutomatic = true)
              and d.yearsOfExperience >= :minExperience
            order by d.yearsOfExperience desc, d.id asc
            """,
            countQuery = """
            select count(d) from DriverProfile d join d.user u
            where d.available = true and u.enabled = true
              and (:city = '' or lower(d.city) = :city)
              and (:needManual = false or d.drivesManual = true)
              and (:needAutomatic = false or d.drivesAutomatic = true)
              and d.yearsOfExperience >= :minExperience
            """)
    Page<DriverProfile> search(@Param("city") String city,
                               @Param("needManual") boolean needManual,
                               @Param("needAutomatic") boolean needAutomatic,
                               @Param("minExperience") int minExperience,
                               Pageable pageable);
}
