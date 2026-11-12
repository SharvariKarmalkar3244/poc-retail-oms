package com.scoperetail.poc.contract;

import java.util.List;

public record StockReleaseResponse(
        ReleaseStatus status,
        String message,
        String orderId,
        List<InventoryLineResult> items
) {
}
