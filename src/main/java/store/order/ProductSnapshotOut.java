package store.order;

import java.math.BigDecimal;

public record ProductSnapshotOut(
    String id,
    String name,
    String description,
    BigDecimal price,
    int stock,
    String unit
) {
}
