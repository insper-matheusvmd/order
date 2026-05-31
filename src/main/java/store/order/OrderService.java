package store.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import feign.FeignException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final ExchangeClient exchangeClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient, ExchangeClient exchangeClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.exchangeClient = exchangeClient;
    }

    @Transactional
    public OrderCreatedOut create(String accountId, CreateOrderIn in) {
        OrderModel order = new OrderModel();
        order.setAccountId(accountId);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (CreateOrderItemIn itemIn : in.items()) {
            ProductSnapshotOut product = fetchProduct(itemIn.idProduct().trim());
            if (product.stock() < itemIn.quantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + product.id());
            }

            BigDecimal unitPrice = scale(product.price());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemIn.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

            OrderItemModel item = new OrderItemModel();
            item.setProductId(product.id());
            item.setQuantity(itemIn.quantity());
            item.setUnitPriceUsd(unitPrice);
            item.setTotalUsd(lineTotal);
            order.addItem(item);

            total = total.add(lineTotal).setScale(2, RoundingMode.HALF_UP);
        }

        order.setTotalUsd(total);
        OrderModel saved = orderRepository.saveAndFlush(order);
        return toCreatedOut(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryOut> findAll(String accountId) {
        return orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
            .stream()
            .map(this::toSummaryOut)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailsOut findById(String accountId, String id, String currency) {
        OrderModel order = orderRepository.findByIdAndAccountId(id, accountId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (currency == null || currency.equalsIgnoreCase("USD")) {
            return toDetailsOut(order, "USD", BigDecimal.ONE);
        }

        BigDecimal rate = fetchExchangeRate("USD", currency.toUpperCase(), accountId);
        return toDetailsOut(order, currency.toUpperCase(), rate);
    }

    private ProductSnapshotOut fetchProduct(String idProduct) {
        try {
            return productClient.findById(idProduct);
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found: " + idProduct);
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to validate product: " + idProduct);
        }
    }

    private BigDecimal fetchExchangeRate(String from, String to, String accountId) {
        try {
            ExchangeRateOut rate = exchangeClient.getRate(from, to, accountId);
            return BigDecimal.valueOf(rate.sell());
        } catch (FeignException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency pair not found: " + from + "/" + to);
        } catch (FeignException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to fetch exchange rate");
        }
    }

    private OrderCreatedOut toCreatedOut(OrderModel model) {
        return new OrderCreatedOut(
            model.getId(),
            model.getCreatedAt(),
            model.getStatus(),
            model.getItems().stream().map(this::toItemOut).toList(),
            scale(model.getTotalUsd())
        );
    }

    private OrderSummaryOut toSummaryOut(OrderModel model) {
        return new OrderSummaryOut(
            model.getId(),
            model.getCreatedAt(),
            model.getStatus(),
            scale(model.getTotalUsd())
        );
    }

    private OrderDetailsOut toDetailsOut(OrderModel model, String currency, BigDecimal rate) {
        return new OrderDetailsOut(
            model.getId(),
            model.getCreatedAt(),
            model.getStatus(),
            currency,
            model.getItems().stream().map(item -> toItemOut(item, rate)).toList(),
            scale(model.getTotalUsd().multiply(rate))
        );
    }

    private OrderItemOut toItemOut(OrderItemModel item) {
        return new OrderItemOut(
            item.getId(),
            new ProductRefOut(item.getProductId()),
            item.getQuantity(),
            scale(item.getTotalUsd())
        );
    }

    private OrderItemOut toItemOut(OrderItemModel item, BigDecimal rate) {
        return new OrderItemOut(
            item.getId(),
            new ProductRefOut(item.getProductId()),
            item.getQuantity(),
            scale(item.getTotalUsd().multiply(rate))
        );
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

}
