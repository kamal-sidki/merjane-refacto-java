package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;

@Service
public class ProductService {

    private static final String TYPE_NORMAL = "NORMAL";
    private static final String TYPE_SEASONAL = "SEASONAL";
    private static final String TYPE_EXPIRABLE = "EXPIRABLE";

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ProductService(ProductRepository productRepository, NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    public void processProduct(Product product) {
        if (product == null || product.getType() == null) {
            return;
        }

        switch (product.getType()) {
            case TYPE_NORMAL -> handleNormalProduct(product);
            case TYPE_SEASONAL -> handleSeasonalProductProcess(product);
            case TYPE_EXPIRABLE -> handleExpiredProduct(product);
            default -> {

            }
        }
    }

    public void notifyDelay(int leadTime, Product product) {
        product.setLeadTime(leadTime);
        save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

    public void handleNormalProduct(Product product) {
        if (decrementIfAvailable(product)) {
            return;
        }

        Integer leadTime = product.getLeadTime();
        if (leadTime != null && leadTime > 0) {
            notifyDelay(leadTime, product);
        }
    }

    private void handleSeasonalProductProcess(Product product) {
        LocalDate now = LocalDate.now();

        if (isInSeason(now, product) && hasAvailableStock(product)) {
            decrementIfAvailable(product);
            return;
        }

        handleSeasonalProduct(product);
    }

    public void handleSeasonalProduct(Product product) {
        LocalDate now = LocalDate.now();

        Integer leadTime = product.getLeadTime();
        if (leadTime == null) {
            return;
        }

        if (restockAfterSeasonEnd(now, product, leadTime)) {
            notificationService.sendOutOfStockNotification(product.getName());
            product.setAvailable(0);
            save(product);
            return;
        }

        if (seasonNotStartedYet(now, product)) {
            notificationService.sendOutOfStockNotification(product.getName());
            save(product);
            return;
        }

        notifyDelay(leadTime, product);
    }

    public void handleExpiredProduct(Product product) {
        LocalDate now = LocalDate.now();

        if (hasAvailableStock(product) && notExpired(now, product)) {
            decrementIfAvailable(product);
            return;
        }

        notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
        product.setAvailable(0);
        save(product);
    }


    private void save(Product product) {
        productRepository.save(product);
    }

    private boolean hasAvailableStock(Product product) {
        return product.getAvailable() != null && product.getAvailable() > 0;
    }


    private boolean decrementIfAvailable(Product product) {
        if (!hasAvailableStock(product)) {
            return false;
        }
        product.setAvailable(product.getAvailable() - 1);
        save(product);
        return true;
    }

    private boolean isInSeason(LocalDate now, Product product) {
        return product.getSeasonStartDate() != null
                && product.getSeasonEndDate() != null
                && now.isAfter(product.getSeasonStartDate())
                && now.isBefore(product.getSeasonEndDate());
    }

    private boolean restockAfterSeasonEnd(LocalDate now, Product product, int leadTime) {
        return product.getSeasonEndDate() != null
                && now.plusDays(leadTime).isAfter(product.getSeasonEndDate());
    }

    private boolean seasonNotStartedYet(LocalDate now, Product product) {
        return product.getSeasonStartDate() != null && product.getSeasonStartDate().isAfter(now);
    }

    private boolean notExpired(LocalDate now, Product product) {
        return product.getExpiryDate() != null && product.getExpiryDate().isAfter(now);
    }
}