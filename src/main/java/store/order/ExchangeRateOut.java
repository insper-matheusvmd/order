package store.order;

public record ExchangeRateOut(
    double sell,
    double buy,
    String date
) {
}
