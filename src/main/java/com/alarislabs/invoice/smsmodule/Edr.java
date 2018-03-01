package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.HttpException;
import com.alarislabs.invoice.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import com.google.gson.JsonObject;

public class Edr {

    private transient static final Logger Log = LoggerFactory.getLogger(Edr.class);

    private String messageId;
    private LocalDateTime edrDate;
    private HttpStatus reasonCode;
    private int accId;
    private String ani;
    private String dnis;
    private String message;
    private AbstractRate rate;
    private Channel channel;
    private int poiId;
    private int campaignId;
    private String extInfo;
    private int segmentNum;
    private int lockedSms;
    @SuppressWarnings("all")
    private LocalDateTime dlrArriveDate;
    private MessageSplitMode messageSplitMode;

    private Edr() {
    }

    static Edr getInstance(
            String messageId,
            LocalDateTime edrDate,
            HttpStatus reasonCode,
            int accId,
            String ani,
            String dnis,
            String message,
            AbstractRate rate,
            Channel channel,
            int poiId,
            int campaignId,
            String extInfo,
            int segmentNum,
            MessageSplitMode messageSplitMode) {

        Edr edr = new Edr();

        edr.messageId = messageId;
        edr.edrDate = edrDate;
        edr.reasonCode = reasonCode;
        edr.accId = accId;
        edr.ani = ani;
        edr.dnis = dnis;
        edr.message = message;
        edr.rate = rate;
        edr.channel = channel;
        edr.poiId = poiId;
        edr.campaignId = campaignId;
        edr.extInfo = extInfo;
        edr.segmentNum = segmentNum;
        edr.lockedSms = 0;
        edr.messageSplitMode = messageSplitMode;

        return edr;
    }


    public static Edr getInstance(Sms sms, HttpStatus reasonCode, String extInfo, String dnis) {
        return Edr.getInstance(
                UUID.randomUUID().toString(),
                Conf.now(),
                reasonCode,
                sms.getAccId(),
                sms.getAni(),
                dnis == null ? sms.getDnisList() : dnis,
                sms.getMessage(),
                null,
                null,
                0,
                sms.getCampaignId(),
                extInfo,
                1,
                sms.getMessageSplitMode()
        );
    }

    private static void addString(StringBuilder record, String value, int maxWidth, boolean base64encode) {
        record.append("\"");
        if (value != null)
            record.append(base64encode ? Utils.base64Encode(Utils.substringFromStart(value, maxWidth)) : Utils.substringFromStart(value, maxWidth));
        record.append("\"");
    }

    private static void addInt(StringBuilder record, int value) {
        record.append(value);
    }

    private static void addDouble(StringBuilder record, double value) {
        record.append(value);
    }

    private static void addDateTime(StringBuilder record, LocalDateTime value) {
        record.append("\"");
        if (value != null) record.append(value.format(Utils.sqlFormatter));
        record.append("\"");
    }

    @Override
    public String toString() {

        StringBuilder record = new StringBuilder();

        /*
          Base64 encoded fields: message_id, dnis, ani, message_text, ext_info, mccmnc, country_dial_code
        */

        addDateTime(record, edrDate); //edr_date date
        record.append(",");
        addString(record, reasonCode == HttpStatus.OK ? "SENT": "NOT ACCEPTED", 30, false); //edr_status varchar2(30)
        record.append(",");
        addInt(record, reasonCode == HttpStatus.OK ? 1: 0); //edr_is_successful number(1)
        record.append(",");
        addString(record, messageId, 65, true); //message_id varchar2(65)
        record.append(",");
        addString(record, dnis, 32, true); //dnis varchar2(32)
        record.append(",");
        addString(record, ani, 32, true); //ani varchar2(32)
        record.append(",");
        addString(record, message, 256, true); //message_text varchar2(256)
        record.append(",");
        addInt(record, accId); //sender_acc_id number
        record.append(",");
        addInt(record, poiId); //poi_id number
        record.append(",");
        addInt(record, rate != null && rate instanceof PackSubscription ? ((PackSubscription)rate).getPackId() : 0); //pack_id number
        record.append(",");
        addInt(record, campaignId); //campaign_id number
        record.append(",");
        addString(record, extInfo, 256, true); //ext_info varchar2(256)
        record.append(",");
        addInt(record, reasonCode.value()); //reason_code number
        record.append(",");
        addDateTime(record, dlrArriveDate); //dlr_arrival_date date
        record.append(",");
        addInt(record, rate != null && rate instanceof PackSubscription ? ((PackSubscription)rate).getSubscrId() : 0); //subscr_id number
        record.append(",");
        addInt(record, segmentNum); //segment_num number(5)
        record.append(",");
        addString(record, rate != null ? rate.getMccmnc() : "", 6, true); //mccmnc varchar2(6)
        record.append(",");
        addInt(record, getMessageSplitMode() == MessageSplitMode.payload ? AuxUtils.getPartAmount(getMessage()) : 1); //segment_count number(2)
        record.append(",");
        addString(record, rate != null ? rate.getCountryDialCode() : "", 16, true); //country_dial_code varchar2(16)
        record.append(",");
        addDouble(record, rate != null ? rate.getRatePerSegment() : 0); //rate numbe
        record.append(",");
        addInt(record, rate != null && rate instanceof SmsRate ? ((SmsRate)rate).getRateId() : 0); //rate_id number
        record.append(",");
        addInt(record, rate != null ? rate.getProductId() : 0); //product_id number
        record.append("\n");

        return record.toString();

    }

    JsonObject getShortRepresentation() {

        JsonObject obj = new JsonObject();
        obj.addProperty("message_id", messageId);
        obj.addProperty("dnis", dnis);
        obj.addProperty("segment_num", segmentNum);
        obj.addProperty("http_status", reasonCode.value());

        return obj;
    }

    boolean accountingRequestStart() {

        lockedSms = (messageSplitMode == MessageSplitMode.cut ? 1 : AuxUtils.getPartAmount(message));

        //Get rate
        try {
            rate = AbstractRate.getRate(accId, dnis, lockedSms);

            if (rate instanceof PackSubscription && Utils.hasValue(((PackSubscription) rate).getForcedText())) {
                //Overwrite text of sms with forced text
                message = ((PackSubscription) rate).getForcedText();
                messageSplitMode = MessageSplitMode.cut;
                lockedSms = 1;
            }

        } catch (HttpException e) {
            setReasonCode(e.getHttpStatus());
            extInfo = e.getMessage();
            lockedSms = 0;
            Log.error("Error finding rate: " + e.getMessage());
            return false;
        }

        //Get Poi
        Poi poi = Poi.getPoi(rate.getProductId(), rate.getServiceType());

        //Get channel
        if (poi != null) {
            poiId = poi.getPoiId();
            channel = Channel.getChannel(poi.getChannelId());
        }

        if (channel == null) {

            setReasonCode(HttpStatus.BAD_REQUEST);

            //Check user products for correct error message
            Boolean productsFound = false;

            try {
                for (Map.Entry<String, String> productEntry : Conf.getMainRedis().hgetAll(SmsRate.ACCOUNT_REDIS_KEY + ":" + Integer.toString(accId)).entrySet()) {
                    if (productEntry.getKey().matches("product:\\d*")) {
                        productsFound = true;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.error("Error accessing Redis: " + e.getMessage());
            }

            extInfo = productsFound ? "Appropriate SMS channel/POI not found for Account ID: " + accId + ". Please contact your administrator for details" : "Please select rate plan";
            lockedSms = 0;
            return false;
        }

        //Lock funds
        try {
            rate.lockFunds(lockedSms);
        } catch (Exception e) {

            if (rate instanceof PackSubscription) {
                //Package was selected, but lock failed, do recursive search
                return accountingRequestStart();
            } else {
                //Lock failed due to negative balance
                setReasonCode(HttpStatus.PAYMENT_REQUIRED);
                extInfo = e.getMessage();
                lockedSms = 0;
                Log.error("Error locking funds: " + e.getMessage());
                return false;
            }
        }

        return true;

    }

    void accountingRequestStop() {

        if (lockedSms > 0) {

            int unlockedSms = (this.getReasonCode() == HttpStatus.OK ? lockedSms : 0);
            try {
                rate.unlockFunds(messageId, lockedSms, unlockedSms);
                lockedSms = 0;
            } catch (Exception e) {
                Log.error("Error while releasing pack lock", e);
            }
        }
    }

    int urlHashCode() {
        int result = accId;
        result = 31 * result + (ani != null ? ani.hashCode() : 0);
        result = 31 * result + (rate != null && rate.getServiceType() != null ? rate.getServiceType().hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        result = 31 * result + poiId;
        result = 31 * result + (messageSplitMode != null ? messageSplitMode.hashCode() : 0);
        return result;
    }


    void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    void setReasonCode(HttpStatus reasonCode) {
        this.reasonCode = reasonCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    void setExtInfo(String extInfo) {
        this.extInfo = extInfo;
    }

    void setSegmentNum(int segmentNum) {
        this.segmentNum = segmentNum;
    }

    String getMessageId() { return messageId; }

    HttpStatus getReasonCode() {
        return reasonCode;
    }

    int getAccId() {
        return accId;
    }

    String getDnis() {
        return dnis;
    }

    String getAni() {
        return ani;
    }

    public String getMessage() {
        return message;
    }

    String getExtInfo() {
        return extInfo;
    }

    AbstractRate getRate() {
        return rate;
    }

    Channel getChannel() {
        return channel;
    }

    MessageSplitMode getMessageSplitMode() {
        return messageSplitMode;
    }

    LocalDateTime getEdrDate() {
        return edrDate;
    }

    int getPoiId() {
        return poiId;
    }

    int getCampaignId() {
        return campaignId;
    }

}
