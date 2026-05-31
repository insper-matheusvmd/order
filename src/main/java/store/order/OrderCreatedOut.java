package store.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedOut(
    String id,
    LocalDateTime date,
    OrderStatus status,
    List<OrderItemOut> items,
    BigDecimal total
) {
}
