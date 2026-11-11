package com.scoperetail.poc.contract;

public record StockReservationRequest(String sku,
                                      int quantity) {
}
