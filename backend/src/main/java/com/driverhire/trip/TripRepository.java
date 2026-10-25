package com.driverhire.trip;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every query that feeds a response fetch-joins the customer and driver (to-one only), so mapping to DTOs
 * never triggers lazy loading or N+1 selects, and paging stays in the database.
 */
public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("""
            select t from Trip t join fetch t.customer
            left join fetch t.driver d left join fetch d.user
            where t.id = :id
            """)
    Optional<Trip> findDetailedById(@Param("id") Long id);

    @Query(value = """
            select t from Trip t join fetch t.customer
            left join fetch t.driver d left join fetch d.user
            where t.customer.id = :customerId
            order by t.createdAt desc, t.id desc
            """,
            countQuery = "select count(t) from Trip t where t.customer.id = :customerId")
    Page<Trip> findForCustomer(@Param("customerId") Long customerId, Pageable pageable);

    /** Open trips still in the future for a city, soonest first. {@code city} must be lower-cased. */
    @Query(value = """
            select t from Trip t join fetch t.customer
            where t.status = :status and lower(t.pickupCity) = :city and t.startAt > :now
            order by t.startAt asc, t.id asc
            """,
            countQuery = """
            select count(t) from Trip t
            where t.status = :status and lower(t.pickupCity) = :city and t.startAt > :now
            """)
    Page<Trip> findOpenByCity(@Param("status") TripStatus status,
                              @Param("city") String city,
                              @Param("now") Instant now,
                              Pageable pageable);

    @Query(value = """
            select t from Trip t join fetch t.customer
            join fetch t.driver d join fetch d.user
            where d.id = :driverId and t.status in :statuses
            order by t.startAt desc, t.id desc
            """,
            countQuery = "select count(t) from Trip t where t.driver.id = :driverId and t.status in :statuses")
    Page<Trip> findForDriver(@Param("driverId") Long driverProfileId,
                             @Param("statuses") Collection<TripStatus> statuses,
                             Pageable pageable);
}
