package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Response;
import com.google.gson.JsonArray;

class SmsResponse {

    private Response response;
    private JsonArray details;

    SmsResponse(Response response, JsonArray details) {
        this.response = response;
        this.details = details;
    }


    Response getResponse() {
        return response;
    }

    JsonArray getDetails() {
        return details;
    }


}
