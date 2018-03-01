package com.alarislabs.invoice.payments;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.Response;
import com.alarislabs.invoice.common.Utils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Enumeration;

public class Payonline implements Payable {

    private static MessageDigest md5;
    private static final Logger Log = LoggerFactory.getLogger(Payonline.class);
    private final JsonObject paymentDetails;

    //Mandatory parameters
    private static final String MERCHANT_ID_PN = "MerchantId";
    private static final String DATE_TIME_PN = "DateTime";
    private static final String TRANSACTION_ID_PN = "TransactionID";
    private static final String ORDER_ID_PN = "OrderId";
    private static final String AMOUNT_PN = "Amount";
    private static final String CURRENCY_PN = "Currency";
    private static final String SECURITY_KEY_PN = "SecurityKey";
    private static final String PRIVATE_SECURITY_KEY_PN = "PrivateSecurityKey";
    private static final String ORDER_DESCRIPTION_PN = "OrderDescription";



    static {

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            md5 = null;
            Log.error("Error getting MessageDigest MD5: " + e.getMessage());
        }

    }

    public Payonline(HttpServletRequest request) {

        this.paymentDetails = new JsonObject();
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String paramName = (String) en.nextElement();
            this.paymentDetails.addProperty(paramName, request.getParameter(paramName));
        }
    }

    @Override
    public boolean isVerified() {

        LocalDateTime payDate;
        String transactionId;
        String securityKey;
        String orderId;
        float amount;
        String currency;

        try {

            payDate = LocalDateTime.parse(paymentDetails.get(DATE_TIME_PN).getAsJsonPrimitive().getAsString(), Utils.isoFormatter);
            if (payDate == null) throw new Exception(DATE_TIME_PN + " is not set");

            transactionId = Utils.getAsString(paymentDetails, TRANSACTION_ID_PN);
            if (transactionId == null) throw new Exception(TRANSACTION_ID_PN + " is not set");

            securityKey = Utils.getAsString(paymentDetails, SECURITY_KEY_PN);
            if (securityKey == null) throw new Exception(SECURITY_KEY_PN + " is not set");

            orderId = Utils.getAsString(paymentDetails, ORDER_ID_PN);
            if (orderId == null) throw new Exception(ORDER_ID_PN + " is not set");

            amount = Utils.getAsFloat(paymentDetails, AMOUNT_PN);
            if (amount == 0.0f) throw new Exception(AMOUNT_PN + " is not set");

            currency = Utils.getAsString(paymentDetails, CURRENCY_PN);
            if (currency == null) throw new Exception(CURRENCY_PN + " is not set");

        } catch (Exception e) {
            Log.warn("Bad request: " + e.getMessage());
            return false;
        }

        String hashKey = String.format(DATE_TIME_PN + "=%s&"
                + TRANSACTION_ID_PN + "=%s&"
                        + ORDER_ID_PN + "=%s&"
                        + AMOUNT_PN + "=%.2f&"
                        + CURRENCY_PN + "=%s&"
                        + PRIVATE_SECURITY_KEY_PN + "=%s",
                payDate.format(Utils.isoFormatter),
                transactionId,
                orderId,
                amount,
                currency,
                Conf.getPayonlinePrivateSecurityKey()
        );

        return securityKey.equals(Utils.bytesToHex(md5.digest(hashKey.getBytes(StandardCharsets.UTF_8))));

    }

    @Override
    public String getPaymentMethod() {
        return "payonline";
    }

    @Override
    public String getPaymentDetails() { return paymentDetails.toString(); }

    public static Response getHash(HttpServletRequest request) {

        JsonObject inParams = new JsonObject();
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String paramName = (String) en.nextElement();
            inParams.addProperty(paramName, request.getParameter(paramName));
        }


        JsonObject outParams = new JsonObject();
        outParams.addProperty(MERCHANT_ID_PN, Conf.getPayonlineMerchantPnId());
        outParams.addProperty(SECURITY_KEY_PN, Utils.bytesToHex(md5.digest(String.format(
                MERCHANT_ID_PN + "=%s&"
                        + ORDER_ID_PN + "=%s&"
                        + AMOUNT_PN + "=%.2f&"
                        + CURRENCY_PN + "=%s&"
                        + ORDER_DESCRIPTION_PN + "=%s&"
                        + PRIVATE_SECURITY_KEY_PN + "=%s",
                Conf.getPayonlineMerchantPnId(),
                Utils.getAsString(inParams, ORDER_ID_PN),
                Utils.getAsFloat(inParams, AMOUNT_PN),
                Utils.getAsString(inParams, CURRENCY_PN),
                Utils.getAsString(inParams, ORDER_DESCRIPTION_PN),
                Conf.getPayonlinePrivateSecurityKey()
        ).getBytes(StandardCharsets.UTF_8))));


        return Response.newInstance(outParams, HttpStatus.OK);
    }

}
