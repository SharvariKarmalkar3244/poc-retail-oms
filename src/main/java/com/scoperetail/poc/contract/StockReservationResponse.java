package com.scoperetail.poc.contract;

import java.util.List;

public record StockReservationResponse(
        ReservationStatus status,
        String message,
        String orderId,
        List<InventoryLineResult> items) {
}
