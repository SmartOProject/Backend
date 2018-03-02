package com.alarislabs.invoice.smsmodule;

class Product {

    private int productId;

    private int parentProductId;
    private int effectiveProductId;

    Product (int productId, int parentProductId, int effectiveProductId) {
        this.productId = productId;
        this.parentProductId = parentProductId;
        this.effectiveProductId = effectiveProductId;
    }

    int getProductId() {
        return productId;
    }

    int getParentProductId() {
        return parentProductId;
    }

    int getEffectiveProductId() {
        return effectiveProductId;
    }


}
