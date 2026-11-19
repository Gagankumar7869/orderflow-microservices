package com.gagan.order.client;

import com.gagan.order.client.dto.CatalogProductResponse;
import com.gagan.order.client.exception.CatalogBadRequestException;
import com.gagan.order.client.exception.CatalogProductNotFoundException;
import com.gagan.order.client.exception.CatalogStockConflictException;
import com.gagan.order.client.exception.CatalogUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CatalogClientTest {

    private MockRestServiceServer mockServer;
    private CatalogClient catalogClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8082");
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        this.catalogClient = new CatalogClient(builder.build());
    }

    @Test
    void reserveStock_shouldReturnProduct_on200() {
        String responseJson = """
                {
                  "productId": 10,
                  "productName": "Mechanical Keyboard",
                  "price": 4999.00,
                  "stockQuantity": 8,
                  "isActive": true,
                  "sku": "KB-001"
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/api/v1/products/10/reserve-stock"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderReference\":\"ORD-REF-1\",\"quantity\":2}"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        CatalogProductResponse response = catalogClient.reserveStock(10L, "ORD-REF-1", 2);

        assertThat(response.productId()).isEqualTo(10L);
        assertThat(response.productName()).isEqualTo("Mechanical Keyboard");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("4999.00"));
        assertThat(response.stockQuantity()).isEqualTo(8);
        mockServer.verify();
    }

    @Test
    void releaseStock_shouldReturnProduct_on200() {
        String responseJson = """
                {
                  "productId": 10,
                  "productName": "Mechanical Keyboard",
                  "price": 4999.00,
                  "stockQuantity": 10,
                  "isActive": true
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/api/v1/products/10/release-stock"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderReference\":\"ORD-REF-1\"}"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        CatalogProductResponse response = catalogClient.releaseStock(10L, "ORD-REF-1");

        assertThat(response.stockQuantity()).isEqualTo(10);
        mockServer.verify();
    }

    @Test
    void reserveStock_shouldThrowBadRequest_on400() {
        mockServer.expect(requestTo("http://localhost:8082/api/v1/products/10/reserve-stock"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> catalogClient.reserveStock(10L, "ORD-REF-1", 0))
                .isInstanceOf(CatalogBadRequestException.class);
        mockServer.verify();
    }

    @Test
    void reserveStock_shouldThrowNotFound_on404() {
        mockServer.expect(requestTo("http://localhost:8082/api/v1/products/999/reserve-stock"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> catalogClient.reserveStock(999L, "ORD-REF-1", 1))
                .isInstanceOf(CatalogProductNotFoundException.class);
        mockServer.verify();
    }

    @Test
    void reserveStock_shouldThrowStockConflict_on409() {
        mockServer.expect(requestTo("http://localhost:8082/api/v1/products/10/reserve-stock"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> catalogClient.reserveStock(10L, "ORD-REF-1", 50))
                .isInstanceOf(CatalogStockConflictException.class);
        mockServer.verify();
    }

    @Test
    void reserveStock_shouldThrowUnavailableNotIndeterminate_on401() {
        mockServer.expect(requestTo("http://localhost:8082/api/v1/products/10/reserve-stock"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> catalogClient.reserveStock(10L, "ORD-REF-1", 1))
                .isInstanceOf(CatalogUnavailableException.class)
                .satisfies(ex -> assertThat(((CatalogUnavailableException) ex).isIndeterminate()).isFalse());
        mockServer.verify();
    }

    @Test
    void reserveStock_shouldThrowUnavailableIndeterminate_on500() {
        mockServer.expect(requestTo("http://localhost:8082/api/v1/products/10/reserve-stock"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> catalogClient.reserveStock(10L, "ORD-REF-1", 1))
                .isInstanceOf(CatalogUnavailableException.class)
                .satisfies(ex -> assertThat(((CatalogUnavailableException) ex).isIndeterminate()).isTrue());
        mockServer.verify();
    }

    @Test
    void reserveStock_shouldThrowUnavailableIndeterminate_onTimeout() {
        mockServer.expect(requestTo("http://localhost:8082/api/v1/products/10/reserve-stock"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThatThrownBy(() -> catalogClient.reserveStock(10L, "ORD-REF-1", 1))
                .isInstanceOf(CatalogUnavailableException.class)
                .satisfies(ex -> assertThat(((CatalogUnavailableException) ex).isIndeterminate()).isTrue());
        mockServer.verify();
    }
}