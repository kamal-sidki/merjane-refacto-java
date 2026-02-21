package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@UnitTest
public class MyUnitTests {

    private static final String TYPE_NORMAL = "NORMAL";
    private static final String TYPE_SEASONAL = "SEASONAL";
    private static final String TYPE_EXPIRABLE = "EXPIRABLE";

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setup() {
        Mockito.when(productRepository.save(Mockito.any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void should_send_delay_notification_and_save_product_when_notifyDelay_called() {
        // GIVEN
        Product product = new Product(null, 15, 0, TYPE_NORMAL, "RJ45 Cable", null, null, null);

        // WHEN
        productService.notifyDelay(product.getLeadTime(), product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository).save(product);
        Mockito.verify(notificationService).sendDelayNotification(15, product.getName());
    }

    @Test
    void should_set_available_to_zero_and_notify_out_of_stock_when_seasonal_restock_after_season_end() {
        // GIVEN
        LocalDate now = LocalDate.now();
        Product product = new Product(
                null,
                10,
                5,
                TYPE_SEASONAL,
                "Watermelon",
                null,
                now.minusDays(5),
                now.plusDays(3)
        );

        // WHEN
        productService.handleSeasonalProduct(product);

        // THEN
        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService).sendOutOfStockNotification(product.getName());
        Mockito.verify(productRepository).save(product);
    }

    @Test
    void should_notify_out_of_stock_and_save_when_seasonal_season_not_started_yet() {
        // GIVEN
        LocalDate now = LocalDate.now();
        Product product = new Product(
                null,
                5,
                2,
                TYPE_SEASONAL,
                "Strawberries",
                null,
                now.plusDays(2),
                now.plusDays(20)
        );

        // WHEN
        productService.handleSeasonalProduct(product);

        // THEN
        Mockito.verify(notificationService).sendOutOfStockNotification(product.getName());
        Mockito.verify(productRepository).save(product);
    }

    @Test
    void should_notify_delay_when_seasonal_and_restock_within_season() {
        // GIVEN
        LocalDate now = LocalDate.now();
        Product product = new Product(
                null,
                2,
                0,
                TYPE_SEASONAL,
                "Peaches",
                null,
                now.minusDays(1),
                now.plusDays(10)
        );

        // WHEN
        productService.handleSeasonalProduct(product);

        // THEN
        Mockito.verify(notificationService).sendDelayNotification(2, product.getName());
        Mockito.verify(productRepository).save(product);
    }

    @Test
    void should_decrement_available_and_save_when_expirable_not_expired_and_available() {
        // GIVEN
        LocalDate now = LocalDate.now();
        Product product = new Product(
                null,
                0,
                2,
                TYPE_EXPIRABLE,
                "Yogurt",
                now.plusDays(5),
                null,
                null
        );

        // WHEN
        productService.handleExpiredProduct(product);

        // THEN
        assertEquals(1, product.getAvailable());
        Mockito.verify(productRepository).save(product);
        Mockito.verify(notificationService, Mockito.never())
                .sendExpirationNotification(Mockito.anyString(), Mockito.any());
    }

    @Test
    void should_set_available_to_zero_and_notify_expiration_when_expirable_is_expired_or_not_available() {
        // GIVEN
        LocalDate now = LocalDate.now();
        Product product = new Product(
                null,
                0,
                3,
                TYPE_EXPIRABLE,
                "Milk",
                now.minusDays(1),
                null,
                null
        );

        // WHEN
        productService.handleExpiredProduct(product);

        // THEN
        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService)
                .sendExpirationNotification(product.getName(), product.getExpiryDate());
        Mockito.verify(productRepository).save(product);
    }
}