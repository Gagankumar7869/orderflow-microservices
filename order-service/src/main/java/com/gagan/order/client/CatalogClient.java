package com.gagan.order.client;

import com.gagan.order.client.dto.CatalogProductResponse;
import com.gagan.order.client.dto.ReleaseStockRequest;
import com.gagan.order.client.dto.ReserveStockRequest;
import com.gagan.order.client.exception.CatalogBadRequestException;
import com.gagan.order.client.exception.CatalogProductNotFoundException;
import com.gagan.order.client.exception.CatalogStockConflictException;
import com.gagan.order.client.exception.CatalogUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class CatalogClient {

    private static final String RESERVE_PATH = "/api/v1/products/{productId}/reserve-stock";
    private static final String RELEASE_PATH = "/api/v1/products/{productId}/release-stock";

    private final RestClient catalogRestClient;

    public CatalogClient(RestClient catalogRestClient) {
        this.catalogRestClient = catalogRestClient;
    }

    public CatalogProductResponse reserveStock(Long productId, String orderReference, Integer quantity) {
        ReserveStockRequest body = new ReserveStockRequest(orderReference, quantity);
        try {
            return catalogRestClient.post()
                    .uri(RESERVE_PATH, productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(CatalogProductResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapHttpError("reserve-stock", productId, orderReference, ex);
        } catch (ResourceAccessException ex) {
            log.error("Network/timeout calling catalog {} for productId={}, orderRef={}",
                    "reserve-stock", productId, orderReference, ex);
            throw new CatalogUnavailableException(
                    "Catalog service timed out or was unreachable during reserve-stock", true, ex);
        }
    }

    public CatalogProductResponse releaseStock(Long productId, String orderReference) {
        ReleaseStockRequest body = new ReleaseStockRequest(orderReference);
        try {
            return catalogRestClient.post()
                    .uri(RELEASE_PATH, productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(CatalogProductResponse.class);
        } catch (RestClientResponseException ex) {
            throw mapHttpError("release-stock", productId, orderReference, ex);
        } catch (ResourceAccessException ex) {
            log.error("Network/timeout calling catalog {} for productId={}, orderRef={}",
                    "release-stock", productId, orderReference, ex);
            throw new CatalogUnavailableException(
                    "Catalog service timed out or was unreachable during release-stock", true, ex);
        }
    }

    private RuntimeException mapHttpError(
            String operation, Long productId, String orderReference, RestClientResponseException ex) {

        HttpStatusCode status = ex.getStatusCode();
        int code = status.value();
        String responseBody = ex.getResponseBodyAsString();

        log.warn("Catalog {} failed with HTTP {} for productId={}, orderRef={}, body={}",
                operation, code, productId, orderReference, responseBody);

        if (code == 400) {
            return new CatalogBadRequestException("Invalid catalog request for product " + productId);
        }
        if (code == 404) {
            return new CatalogProductNotFoundException("Product " + productId + " not found in catalog");
        }
        if (code == 409) {
            return new CatalogStockConflictException("Insufficient stock for product " + productId);
        }
        if (code == 401 || code == 403) {
            return new CatalogUnavailableException(
                    "Catalog service rejected service authentication/authorization (HTTP " + code + ")", false, ex);
        }
        return new CatalogUnavailableException(
                "Catalog service returned server error (HTTP " + code + ")", true, ex);
    }
}