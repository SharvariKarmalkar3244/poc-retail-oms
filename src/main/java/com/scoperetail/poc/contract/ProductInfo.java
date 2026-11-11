package com.scoperetail.poc.contract;

import java.math.BigDecimal;

public record ProductInfo(
        String sku,
        String name,
        BigDecimal price
) {

}
