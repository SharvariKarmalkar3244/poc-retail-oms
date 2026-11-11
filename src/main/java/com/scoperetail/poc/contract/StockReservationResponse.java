package com.scoperetail.poc.contract;

public record StockReservationResponse(ReservationStatus status,
                                       String message,
                                       String sku,
                                       int requestedQuantity,
                                       int availableQuantity) {
}
