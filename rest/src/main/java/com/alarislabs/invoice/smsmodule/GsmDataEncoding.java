package com.alarislabs.invoice.smsmodule;

enum GsmDataEncoding {

    GSM7, USC2;

    public int getDataEncodingValue() {
        return this == GSM7 ? 0 : this == USC2 ? 8 : -1;
    }

}