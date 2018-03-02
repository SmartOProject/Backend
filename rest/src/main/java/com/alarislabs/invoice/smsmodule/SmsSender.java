package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.HttpException;
import com.alarislabs.invoice.common.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SmsSender {

    private static final Logger Log = LoggerFactory.getLogger(SmsSender.class);


    public static Response sendBulkSms(JsonArray smsJsonArray,
                                       String username,
                                       String password,
                                       int acc_id,
                                       int campaign_id,
                                       String message_split_mode,
                                       HttpServletRequest request,
                                       boolean showDetails) {

        int finalAccId;

        try {
            finalAccId = SmsAuth.getAuthenticAccId(request, acc_id, username, password);
        } catch (HttpException e) {
            return Response.newErrInstance(e.getMessage(), e.getHttpStatus());
        }

        ExecutorService executor = Executors.newFixedThreadPool(Conf.getSmsThreadCount());
        ArrayList<Future<SmsResponse>> responses = new ArrayList<>();

        //Add all sms
        LocalDateTime bulkStart = Conf.now();
        int smsCount = 0;
        int speedLimit = Conf.getSmsSpeedLimit();
        for (JsonElement e : smsJsonArray) {

            responses.add(executor.submit(new Sms(
                    finalAccId,
                    e.getAsJsonObject().get("from").getAsString(),
                    e.getAsJsonObject().get("to").getAsString(),
                    e.getAsJsonObject().get("message").getAsString(),
                    campaign_id,
                    message_split_mode)
            ));

            smsCount++;

            if (smsCount >= speedLimit) {

                long remainTime = 1000L - bulkStart.until(Conf.now(), ChronoUnit.MILLIS);

                //Limit reached, time remained: sleep
                if (remainTime > 0) {
                    try {
                        Log.info("Process paused for " + remainTime + " milliseconds");
                        Thread.sleep(remainTime);
                    } catch (InterruptedException intExp) {
                        Log.info("Sleep was interrupted: " + intExp.getMessage());
                    }
                }

                //Reset sms and time counters
                smsCount = 0;
                bulkStart = Conf.now();
            }


        }

        JsonArray details = new JsonArray();

        //Check if all is done
        int sentCount = 0, rejectedCount = 0;
        for (Future<SmsResponse> response : responses)
        {
            try {
                details.addAll(response.get().getDetails());
            } catch (Exception e) {
                Log.error("Getting item failed: " + e.getMessage(), e);
            }
        }

        executor.shutdown();

        Iterator<JsonElement> iterator = details.iterator();
        while(iterator.hasNext()) {
            if (iterator.next().getAsJsonObject().get("http_status").getAsInt() == HttpStatus.OK.value())
                sentCount++;
            else
                rejectedCount++;
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("sentCount", sentCount);
        jsonObject.addProperty("rejectedCount", rejectedCount);
        jsonObject.addProperty("totalCount", details.size());
        if (showDetails)
            jsonObject.add("details", details);

        return Response.newInstance(jsonObject, HttpStatus.OK);

    }

    public static Response sendSms(String from,
                                   String to,
                                   String message,
                                   String username,
                                   String password,
                                   int acc_id,
                                   int campaign_id,
                                   String message_split_mode,
                                   HttpServletRequest request) {

        try {

            return new Sms(
                    SmsAuth.getAuthenticAccId(request, acc_id, username, password),
                    from,
                    to,
                    message,
                    campaign_id,
                    message_split_mode).call().getResponse();

        } catch (HttpException e) {
            return Response.newErrInstance(e.getMessage(), e.getHttpStatus());
        } catch (Exception e) {
            Log.error("Unhandled runtime exception in handleSendSms()", e);
            return Response.newErrInstance(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
