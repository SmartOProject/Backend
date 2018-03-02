package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.Response;
import com.alarislabs.invoice.common.Utils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;

public class Sms implements Callable<SmsResponse> {

    private static final Logger Log = LoggerFactory.getLogger(Sms.class);
    private static final String CONF_LAST_UPDATE_DATE = "conf_last_update_date";

    private final int accId;
    private final String ani;
    private final String dnisList;
    private final String message;
    private final int campaignId;
    private final MessageSplitMode messageSplitMode;

    int getAccId() {
        return accId;
    }

    String getAni() {
        return ani;
    }

    String getDnisList() {
        return dnisList;
    }

    String getMessage() {
        return message;
    }

    int getCampaignId() {
        return campaignId;
    }

    MessageSplitMode getMessageSplitMode() {
        return messageSplitMode;
    }

    public Sms(int accId, String ani, String dnisList, String message, int campaignId, String message_split_mode) {
        this.accId = accId;
        this.ani = ani;
        this.dnisList = dnisList.replaceAll("\\s", "");
        this.message = message;
        this.campaignId = campaignId;

        //Get message split mode
        MessageSplitMode messageSplitMode;
        try {
            messageSplitMode = MessageSplitMode.valueOf(Utils.nvl(message_split_mode, Conf.getDefMessageSplitMode()));
        } catch (IllegalArgumentException e) {
            Log.error("Invalid message_split_mode parameter value: " + message_split_mode + " default mode used");
            messageSplitMode = MessageSplitMode.split;
        }
        this.messageSplitMode = messageSplitMode;
    }

    public static List<List<Edr>> splitEdrByUrlHashCode(List<Edr> edrArray) {

        List<List<Edr>> edrArraySets = new ArrayList<>();

        boolean added;

        for (Edr element : edrArray) {

            added = false;
            for (List<Edr> edrSet : edrArraySets) {

                if (edrSet.get(0).urlHashCode() == element.urlHashCode()) {
                    //Group found
                    edrSet.add(element);
                    added = true;
                    break;
                }

            }

            if (!added) {

                List<Edr> buf = new ArrayList<>();
                buf.add(element);
                edrArraySets.add(buf);
            }


        }

        return edrArraySets;

    }

    @Override
    public SmsResponse call() throws Exception {

        Log.info("Got SMS request. Ani: " + ani + "; dnis: " + dnisList);

        //Check SMS connection
        List<Edr> edrArray = new ArrayList<>();
        List<Edr> rejectedEdrArray = new ArrayList<>();
        List<String> dnisArray = Arrays.asList(dnisList.split(","));

        if (!Utils.hasValue(this.getMessage())) {

            rejectedEdrArray.add(Edr.getInstance(this, HttpStatus.NOT_ACCEPTABLE, "Missing SMS text", null));

        } else if (dnisArray.size() == 0) {

            rejectedEdrArray.add(Edr.getInstance(this, HttpStatus.BAD_REQUEST, "Empty DNIS supplied", null));

        } else {

            //Get car_id and conf_last_update_date
            Map<String, String> accountMap = Conf.getMainRedis().hgetAll("account:" + Integer.toString(accId));

            int carId;
            try {
                carId = Integer.parseInt(accountMap.get("car_id"));
            } catch (Exception e) {
                Log.error("Cannot get car_id from redis for account: " + Integer.toString(accId));
                carId = 0;
            }

            AuxUtils.assureSwitchGotConfiguration(accountMap.containsKey(CONF_LAST_UPDATE_DATE) ? LocalDateTime.parse(accountMap.get(CONF_LAST_UPDATE_DATE), Utils.sqlFormatter) : null);

            //Loop for dnis
            for (String dnis : dnisArray) {

                if (dnis != null) {

                    if (AuxUtils.bnumberInBlackList(carId, dnis)) {

                        rejectedEdrArray.add(Edr.getInstance(this, HttpStatus.LOCKED, "B-number " + dnis + " in black list", dnis));

                    } else {

                        Edr edr = Edr.getInstance(this, HttpStatus.PROCESSING, null, dnis.replaceAll("\\D", ""));

                        if (edr.accountingRequestStart()) edrArray.add(edr);
                        else rejectedEdrArray.add(edr);

                    }

                }
            }
        }

        //Write unsuccessful attempts
        if (rejectedEdrArray.size() > 0) {
            Log.info(rejectedEdrArray.size() + " SMS rejected");

            //Write rejected Edrs to files
            for(Edr edr : rejectedEdrArray) {
                Log.info("Writing rejected Edr: " + edr.getMessageId());
                EdrWriter.writeEdr(edr.toString());
            }
        }

        //Send
        Response finalResponse = null;
        JsonArray details = new JsonArray();
        if (edrArray.size() > 0) {

            //Split array and merge switch response
            for (List<Edr> resortedList : splitEdrByUrlHashCode(edrArray)) {

                //Move SMS array to switch
                Response switchResp = sendSmsArray(resortedList);

                //SMS array was resorted
                for(Edr item : resortedList) details.add(item.getShortRepresentation());


                //Merge switch response
                if (finalResponse == null) finalResponse = switchResp;
                else finalResponse.mergeResponse(switchResp);
            }

        } else if (rejectedEdrArray.size() > 0) {

            finalResponse = Response.newErrInstance(rejectedEdrArray.get(0).getExtInfo(), rejectedEdrArray.get(0).getReasonCode());

        } else {

            Log.info("Empty request, DNIS list empty?");
            finalResponse = Response.newErrInstance("Empty request, DNIS list empty?", HttpStatus.I_AM_A_TEAPOT);

        }

        for(Edr item : rejectedEdrArray) details.add(item.getShortRepresentation());

        return new SmsResponse(finalResponse, details);
    }

    private static String getUrl(List<Edr> edrArray) {

        StringBuilder requestUrl = new StringBuilder(Conf.getSmsSwitchUrl());

        requestUrl.append("&returnText=1");

        Utils.replace(requestUrl, "%ani%", edrArray.get(0).getAni());
        Utils.replace(requestUrl, "%from%", edrArray.get(0).getAni());
        Utils.replace(requestUrl, "%message%", edrArray.get(0).getMessage());
        Utils.replace(requestUrl, "%text%", edrArray.get(0).getMessage());
        Utils.replace(requestUrl, "%username%", edrArray.get(0).getChannel().getChannelSystemId());
        Utils.replace(requestUrl, "%password%", edrArray.get(0).getChannel().getChannelPassword());
        Utils.replace(requestUrl, "%serviceType%", edrArray.get(0).getRate().getServiceType());
        Utils.replace(requestUrl, "%messageMode%", edrArray.get(0).getMessageSplitMode().name());
        Utils.replace(requestUrl, "%channelId%", "-1");
        Utils.replace(requestUrl, "%dataCoding%", Integer.toString(AuxUtils.getDataEncoding(edrArray.get(0).getMessage()).getDataEncodingValue()));

        StringBuilder dnisArrayToSend = new StringBuilder();

        //Combine multiple numbers into one request
        for (Edr edr : edrArray) {
            dnisArrayToSend.append((dnisArrayToSend.length() > 0 ? "," : ""));
            dnisArrayToSend.append(edr.getDnis());
        }

        Utils.replace(requestUrl, "%dnis%", dnisArrayToSend.toString());
        Utils.replace(requestUrl, "%to%", dnisArrayToSend.toString());

        return requestUrl.toString();
    }

    private static void applySwitchResponse(List<Edr> edrArray, JsonElement switchResp) {

        if (switchResp.isJsonObject()) {

            String messageId = Utils.getAsString(switchResp, "message_id");
            String messageText = Utils.base64Decode(Utils.getAsString(switchResp, "message_text"));

            //Switch returned 1 objects
            edrArray.forEach(item -> {
                if (item.getReasonCode() == HttpStatus.PROCESSING) {
                    item.setMessageId(messageId);
                    item.setMessage(messageText);
                }
            });


        } else if (switchResp.isJsonArray()) {

            HashMap<String, Edr> dnisHash = new HashMap<>();

            //Server returned array of objects
            for (JsonElement element : switchResp.getAsJsonArray()) {

                //Get parameters from response object
                boolean matched = false;

                String messageId = Utils.getAsString(element, "message_id");
                String dnis = Utils.getAsString(element, "dnis");
                int segmentNum = element.getAsJsonObject().has("segment_num") ? Utils.getAsInt(element, "segment_num") : 1;
                String messageText = Utils.base64Decode(Utils.getAsString(element, "message_text"));

                //Try to find matching Edr
                for (Edr edr : edrArray) {

                    if (edr.getReasonCode() == HttpStatus.PROCESSING && edr.getDnis().equals(dnis)) {
                        edr.setReasonCode(HttpStatus.OK);
                        edr.setMessageId(messageId);
                        edr.setSegmentNum(segmentNum);
                        edr.setMessage(messageText);
                        matched = true;

                        //Set hash by dnis
                        dnisHash.put(dnis, edr);

                        break;
                    }
                }

                //Add extra Edr
                if (!matched) {

                    Edr baseEdr = dnisHash.containsKey(dnis) ? dnisHash.get(dnis) : edrArray.get(0);

                    edrArray.add(Edr.getInstance(
                            messageId,
                            baseEdr.getEdrDate(),
                            HttpStatus.OK,
                            baseEdr.getAccId(),
                            baseEdr.getAni(),
                            baseEdr.getDnis(),
                            messageText,
                            baseEdr.getRate(),
                            baseEdr.getChannel(),
                            baseEdr.getPoiId(),
                            baseEdr.getCampaignId(),
                            baseEdr.getExtInfo(),
                            segmentNum,
                            baseEdr.getMessageSplitMode()
                    ));

                    Log.trace("Extra edr with messageId:" + messageId + " created");
                }

            }

        }
    }


    //Send SMS array. All SMS must have same parameters except DNIS
    private static Response sendSmsArray(List<Edr> edrArray) {

        //Prepare http link
        String url = getUrl(edrArray);
        Log.trace("Sms switch request: " + url);

        //Send SMS to switch
        LocalDateTime requestStarted = Conf.now();
        Response resp = Utils.requestUrl(url);
        Log.info("Switch answered in " + requestStarted.until(Conf.now(), ChronoUnit.MILLIS) + " milliseconds: " + resp.getBody() + "; HTTP code: " + resp.getStatus().toString());

        //Check response status
        if (resp.getStatus() == HttpStatus.OK) {
            //Parse response from SMS Switch
            try {
                JsonElement switchResp = Conf.getJsonParser().parse(resp.getBody());
                applySwitchResponse(edrArray, switchResp);
            } catch (Exception e) {
                Log.error("Error parsing response for " + url + " from SMS switch: " + resp.getBody() + "; error: " + e.getMessage());
                resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        edrArray.forEach(item -> {

            //Set status from switch resp
            item.setReasonCode(resp.getStatus());

            //Set error details
            if (resp.getStatus() != HttpStatus.OK) item.setExtInfo(Utils.substringFromStart(resp.getBody(), 4000));

            //Release or transform lock
            item.accountingRequestStop();

            //Write Edr
            Log.info("Writing " + (resp.getStatus() ==  HttpStatus.OK ? "successful" : "not successful") + " Edr: " + item.getMessageId());
            EdrWriter.writeEdr(item.toString());
        });

        if (resp.getStatus() == HttpStatus.OK)
            return resp;
        else
            return Response.newErrInstance(resp.getBody(), resp.getStatus());


    }

    public static double getSmsArrayCost(int accId, String smsText, String smsDestinations) {

        String pairArray[] = smsDestinations.split(",");
        int partAmount = AuxUtils.getPartAmount(smsText);
        double totalPrice = 0;

        for (String pair : pairArray) {

            if (pair != null) {
                String[] elements = pair.split(":");
                try {

                    AbstractRate rate = AbstractRate.getRate(accId, elements[0], 1);

                    if (rate != null)
                        totalPrice += Integer.parseInt(elements[1]) * rate.getRatePerSegment() * partAmount;

                } catch (Exception e) {
                    Log.error("Error in getSmsArrayCost: " + e.getMessage());
                }
            }
        }

        return totalPrice;

    }


}