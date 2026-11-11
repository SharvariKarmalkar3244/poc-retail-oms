package com.scoperetail.poc.contract;

public record StockReleaseResponse(
        ReservationStatus status,
        String message,
        String sku,
        int releasedQuantity
) {
}
