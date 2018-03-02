package com.alarislabs.invoice.payments;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.Utils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class PayPal implements Payable {

    private static final Logger Log = LoggerFactory.getLogger(PayPal.class);
    private final String requestLink;
    private final JsonObject paymentDetails;

    public PayPal(String requestLink, HttpServletRequest request) {

        this.requestLink = requestLink;
        String requestCharset = (request.getCharacterEncoding() == null ? "ISO-8859-1" : request.getCharacterEncoding());

        //Extract all parameters to JsonObject
        this.paymentDetails = new JsonObject();
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String paramName = (String) en.nextElement();
            try {
                this.paymentDetails.addProperty(paramName, new String(request.getParameter(paramName).getBytes(requestCharset), StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException e) {
                Log.error("Unsupported encoding in value parameter");
            }
        }
    }

    @Override
    public boolean isVerified() {

        String verificationLink = "cmd=_notify-validate&" + requestLink;
        try {

            URL u = new URL(Conf.getPaypalUrl());
            URLConnection uc = u.openConnection();
            uc.setDoOutput(true);
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            PrintWriter pw = new PrintWriter(uc.getOutputStream());
            pw.println(verificationLink);
            pw.close();

            String payPalResponse = Utils.readStream(new InputStreamReader(uc.getInputStream(), StandardCharsets.UTF_8));

            if (payPalResponse.equals("VERIFIED")) {
                return true;
            } else {
                Log.error("IPN response: " + payPalResponse);
                return false;
            }


        } catch (IOException e) {
            Log.error("Cannot get response from PayPal IPN", e);
            return false;
        }

    }

    @Override
    public String getPaymentMethod() {
        return "paypal";
    }

    @Override
    public String getPaymentDetails() {
        return paymentDetails.toString();
    }

}
