package com.scoperetail.poc.contract;

public interface StockReserver  {

    StockReservationResponse reserve(StockReservationRequest request);

    StockReleaseResponse release(String sku,
                 int quantity);
}
