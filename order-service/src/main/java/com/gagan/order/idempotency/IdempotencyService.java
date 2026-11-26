package com.gagan.order.idempotency;

import com.gagan.order.constant.OrderErrorCode;
import com.gagan.order.dto.CreateOrderRequest;
import com.gagan.order.entity.IdempotencyRecord;
import com.gagan.order.enums.IdempotencyStatus;
import com.gagan.order.exception.OrderServiceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IdempotencyService {

    private final RequestHasher requestHasher;
    private final IdempotencyClaimService claimService;

    public IdempotencyService(RequestHasher requestHasher, IdempotencyClaimService claimService) {
        this.requestHasher = requestHasher;
        this.claimService = claimService;
    }

    public IdempotencyResult claimOrRetrieve(Long userId, String idempotencyKey, CreateOrderRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new OrderServiceException(OrderErrorCode.MISSING_IDEMPOTENCY_KEY);
        }

        String normalizedKey = idempotencyKey.trim();
        String requestHash = requestHasher.hash(request);

        try {
            IdempotencyRecord created = claimService.insertProcessingClaim(userId, normalizedKey, requestHash);
            return IdempotencyResult.newClaim(created);
        } catch (DataIntegrityViolationException ex) {
            IdempotencyRecord existing = claimService.findExistingClaim(userId, normalizedKey)
                    .orElseThrow(() -> new OrderServiceException(
                            OrderErrorCode.INTERNAL_ERROR,
                            "Idempotency conflict occurred but existing record could not be loaded"
                    ));

            return evaluateExistingClaim(existing, requestHash);
        }
    }

    public boolean markCompleted(Long recordId, Long orderId) {
        return claimService.markCompleted(recordId, orderId);
    }

    public boolean markFailed(Long recordId, String errorCode) {
        return claimService.markFailed(recordId, errorCode);
    }

    private IdempotencyResult evaluateExistingClaim(IdempotencyRecord existing, String incomingHash) {
        if (!existing.getRequestHash().equals(incomingHash)) {
            throw new OrderServiceException(OrderErrorCode.IDEMPOTENCY_CONFLICT);
        }

        if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
            throw new OrderServiceException(OrderErrorCode.CHECKOUT_IN_PROGRESS);
        }

        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            return IdempotencyResult.existingCompleted(existing);
        }

        return IdempotencyResult.existingFailed(existing);
    }
}