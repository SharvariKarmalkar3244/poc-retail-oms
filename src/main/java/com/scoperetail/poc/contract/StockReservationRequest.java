package com.scoperetail.poc.contract;

import java.util.List;

public record StockReservationRequest(
        String orderId,
        List<StockReservationLine> items) {
}
