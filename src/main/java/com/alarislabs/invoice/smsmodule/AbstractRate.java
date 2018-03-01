package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.HttpException;
import org.springframework.http.HttpStatus;

abstract class AbstractRate {

    private final String countryDialCode;
    private final String mccmnc;
    private final String serviceType;
    private final int productId;
    private final double ratePerSegment;

    abstract void lockFunds(int lockedSms) throws Exception;
    abstract void unlockFunds(String messageId, int unlockedSms, int confirmedSms) throws Exception;

    AbstractRate(String countryDialCode, String mccmnc, String serviceType, int productId, double ratePerSegment) {
        this.countryDialCode = countryDialCode;
        this.mccmnc = mccmnc;
        this.serviceType = serviceType;
        this.productId = productId;
        this.ratePerSegment = ratePerSegment;
    }

    static AbstractRate getRate(int accountId, String dnis, int segmentsCount) throws HttpException {

        //Step 0, get mccmnc
        String mccmnc;
        try {
            mccmnc = MccmncResolver.getMccmnc(dnis);
        } catch (Exception e) {
            throw new HttpException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }


        //Step 1, look for subscription
        PackSubscription subscription = PackSubscription.findSubscription(accountId, mccmnc, dnis, segmentsCount);
        if (subscription != null) return subscription;

        //Step 2, look for general rate
        SmsRate smsRate = SmsRate.getRate(accountId, mccmnc, dnis);
        if (smsRate != null) {
            return smsRate;
        }
        else {
            throw new HttpException("Rate not found for accountId:" + Integer.toString(accountId) + "; mccmnc:" + mccmnc + "; dnis:" + dnis, HttpStatus.PAYMENT_REQUIRED);
        }
    }

    String getServiceType() { return serviceType; }
    double getRatePerSegment() { return ratePerSegment; }
    String getCountryDialCode() { return countryDialCode; }
    String getMccmnc() { return mccmnc; }
    int getProductId() { return productId; }
}
