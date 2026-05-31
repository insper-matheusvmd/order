package store.order;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "exchange", url = "${clients.exchange.url}")
public interface ExchangeClient {

    @GetMapping("/exchanges/{from}/{to}")
    ExchangeRateOut getRate(
        @PathVariable String from,
        @PathVariable String to,
        @RequestHeader("id-account") String idAccount
    );

}
