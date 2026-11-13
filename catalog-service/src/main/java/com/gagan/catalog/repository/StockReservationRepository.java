package com.gagan.catalog.repository;

import com.gagan.catalog.entity.ReservationStatus;
import com.gagan.catalog.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByOrderReferenceAndProductId(String orderReference, Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE StockReservation r SET r.status = :newStatus " +
           "WHERE r.orderReference = :orderReference AND r.productId = :productId AND r.status = :expectedStatus")
    int updateStatusAtomically(
            @Param("orderReference") String orderReference,
            @Param("productId") Long productId,
            @Param("expectedStatus") ReservationStatus expectedStatus,
            @Param("newStatus") ReservationStatus newStatus);
}