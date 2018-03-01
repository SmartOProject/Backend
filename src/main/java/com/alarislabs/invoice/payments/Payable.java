package com.alarislabs.invoice.payments;

public interface Payable {

    String getPaymentMethod();

    String getPaymentDetails();

    boolean isVerified();

}
