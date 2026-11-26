package com.gagan.order.repository;

import com.gagan.order.entity.IdempotencyRecord;
import com.gagan.order.enums.IdempotencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<IdempotencyRecord> findByOrderReference(String orderReference);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE IdempotencyRecord r
               SET r.status = :newStatus,
                   r.orderId = :orderId,
                   r.errorCode = null,
                   r.updatedAt = :updatedAt
             WHERE r.id = :id
               AND r.status = :expectedStatus
            """)
    int markCompletedIfProcessing(
            @Param("id") Long id,
            @Param("expectedStatus") IdempotencyStatus expectedStatus,
            @Param("newStatus") IdempotencyStatus newStatus,
            @Param("orderId") Long orderId,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE IdempotencyRecord r
               SET r.status = :newStatus,
                   r.errorCode = :errorCode,
                   r.updatedAt = :updatedAt
             WHERE r.id = :id
               AND r.status = :expectedStatus
            """)
    int markFailedIfProcessing(
            @Param("id") Long id,
            @Param("expectedStatus") IdempotencyStatus expectedStatus,
            @Param("newStatus") IdempotencyStatus newStatus,
            @Param("errorCode") String errorCode,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}