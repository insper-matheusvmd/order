package store.order;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/orders")
public class OrderResource {

    private final OrderService service;

    public OrderResource(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderCreatedOut> create(
        @RequestHeader("id-account") String accountId,
        @Valid @RequestBody CreateOrderIn in
    ) {
        return ResponseEntity.status(201).body(service.create(accountId, in));
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryOut>> findAll(
        @RequestHeader("id-account") String accountId
    ) {
        return ResponseEntity.ok(service.findAll(accountId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailsOut> findById(
        @RequestHeader("id-account") String accountId,
        @PathVariable String id,
        @RequestParam(required = false) String currency
    ) {
        return ResponseEntity.ok(service.findById(accountId, id, currency));
    }

}
