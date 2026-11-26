package com.gagan.order.idempotency;

import com.gagan.order.entity.IdempotencyRecord;

public record IdempotencyResult(
        Outcome outcome,
        Long recordId,
        String orderReference,
        Long orderId,
        String errorCode
) {
    public enum Outcome {
        NEW_CLAIM,
        EXISTING_COMPLETED,
        EXISTING_FAILED
    }

    public static IdempotencyResult newClaim(IdempotencyRecord record) {
        return new IdempotencyResult(
                Outcome.NEW_CLAIM,
                record.getId(),
                record.getOrderReference(),
                null,
                null
        );
    }

    public static IdempotencyResult existingCompleted(IdempotencyRecord record) {
        return new IdempotencyResult(
                Outcome.EXISTING_COMPLETED,
                record.getId(),
                record.getOrderReference(),
                record.getOrderId(),
                null
        );
    }

    public static IdempotencyResult existingFailed(IdempotencyRecord record) {
        return new IdempotencyResult(
                Outcome.EXISTING_FAILED,
                record.getId(),
                record.getOrderReference(),
                record.getOrderId(),
                record.getErrorCode()
        );
    }
}