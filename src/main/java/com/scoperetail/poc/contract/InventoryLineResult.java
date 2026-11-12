package com.scoperetail.poc.contract;

public record InventoryLineResult(
        String sku,
        int quantity,
        int availableQuantity
) {
}
