package com.scoperetail.poc.contract;

public interface ProductLookup {

    ProductInfo findBySku(String sku);
}
