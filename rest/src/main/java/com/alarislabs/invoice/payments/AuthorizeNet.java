package com.alarislabs.invoice.payments;

import com.alarislabs.invoice.common.Utils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class AuthorizeNet implements Payable {

    private static final Logger Log = LoggerFactory.getLogger(AuthorizeNet.class);
    private static final String CUST_ID_PN = "x_cust_id";
    private static final String TRANS_ID_PN = "x_trans_id";
    private static final String AMOUNT_PN = "x_amount";
    private static final String MD5_HASH_PN = "x_MD5_Hash";
    private static final String RETURN_LINK_PN = "return_link";

    private final JsonObject paymentDetails;

    public AuthorizeNet(HttpServletRequest request) {

        this.paymentDetails = new JsonObject();
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String paramName = (String) en.nextElement();
            this.paymentDetails.addProperty(paramName, request.getParameter(paramName));
        }
    }

    @Override
    public boolean isVerified() {

        //Payment verified on DB side, here only basic checks

        if (Utils.getAsString(paymentDetails, CUST_ID_PN) == null) {
            Log.warn(CUST_ID_PN + " is not set");
            return false;
        }

        if (Utils.getAsString(paymentDetails, TRANS_ID_PN) == null) {
            Log.warn(TRANS_ID_PN + " is not set");
            return false;
        }

        if (Utils.getAsString(paymentDetails, AMOUNT_PN) == null) {
            Log.warn(AMOUNT_PN + " is not set");
            return false;
        }

        if (Utils.getAsString(paymentDetails, MD5_HASH_PN) == null) {
            Log.warn(MD5_HASH_PN + " is not set");
            return false;
        }

        if (Utils.getAsString(paymentDetails, RETURN_LINK_PN) == null) {
            Log.warn(RETURN_LINK_PN + " is not set");
            return false;
        }

        return true;
    }

    @Override
    public String getPaymentMethod() {
        return "authorize";
    }

    @Override
    public String getPaymentDetails() { return paymentDetails.toString(); }

}
