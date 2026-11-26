package com.gagan.order.idempotency;

import com.gagan.order.entity.IdempotencyRecord;
import com.gagan.order.enums.IdempotencyStatus;
import com.gagan.order.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyClaimService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyClaimService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord insertProcessingClaim(Long userId, String idempotencyKey, String requestHash) {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .orderReference("ORD-" + UUID.randomUUID())
                .status(IdempotencyStatus.PROCESSING)
                .build();

        return idempotencyRecordRepository.saveAndFlush(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<IdempotencyRecord> findExistingClaim(Long userId, String idempotencyKey) {
        return idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markCompleted(Long recordId, Long orderId) {
        int updated = idempotencyRecordRepository.markCompletedIfProcessing(
                recordId,
                IdempotencyStatus.PROCESSING,
                IdempotencyStatus.COMPLETED,
                orderId,
                LocalDateTime.now()
        );
        return updated == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markFailed(Long recordId, String errorCode) {
        int updated = idempotencyRecordRepository.markFailedIfProcessing(
                recordId,
                IdempotencyStatus.PROCESSING,
                IdempotencyStatus.FAILED,
                errorCode,
                LocalDateTime.now()
        );
        return updated == 1;
    }
}