package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.Utils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

class PackSubscription extends AbstractRate {

    //Redis keys
    private final static String PACK_USAGE_REDIS_KEY = "sms_pack_usage";
    private final static String PACK_CONTENT_REDIS_KEY = "sms_pack_content";
    private final static String PACK_SUBSCR_REDIS_KEY = "sms_pack_subscr";

    //Redis fields
    private final static String LOCKED_SMS_COUNT_REDIS_FIELD = "locked_sms_count";
    private final static String CURRENT_USAGE_REDIS_FIELD = "current_usage";
    private final static String PACK_ID_REDIS_FIELD = "pack_id";
    private final static String INCLUDED_SMS_COUNT_REDIS_FIELD = "included_sms_count";
    private final static String PRIORITY_REDIS_FIELD = "priority";
    private final static String SUBSCRIPTION_COST_REDIS_FIELD = "subscription_cost";
    private final static String PRODUCT_ID_REDIS_FIELD = "product_id";
    private final static String COUNTRY_DIAL_CODE_REDIS_FIELD = "country_dial_code";
    private final static String MCC_MNC_REDIS_FIELD = "mcc_mnc";
    private final static String FORCED_TEXT_REDIS_FIELD = "forced_text";

    private static final Logger Log = LoggerFactory.getLogger(PackSubscription.class);

    int getSubscrId() {
        return subscrId;
    }

    int getPackId() {
        return packId;
    }

    private final int subscrId;
    @SuppressWarnings("all")
    private final int packId;
    private final transient int priority;
    private final transient String forcedText;
    private final int includedSmsCount;

    private PackSubscription(String countryDialCode, String mccmnc, String serviceType, int productId, int includedSmsCount, double ratePerSegment, int subscrId, int packId, int priority, String forcedText) {
        super(countryDialCode, mccmnc, serviceType, productId, ratePerSegment);
        this.includedSmsCount = includedSmsCount;
        this.subscrId = subscrId;
        this.packId = packId;
        this.priority = priority;
        this.forcedText = forcedText;
    }

    String getForcedText() {
        return forcedText;
    }

    private int getMatchIndex(String requestedMccMnc, String dnis) {
        if (requestedMccMnc.startsWith(super.getMccmnc()) && (super.getCountryDialCode() == null || dnis.startsWith(super.getCountryDialCode())))
            return priority * 1000 + super.getMccmnc().length() * 100 + (super.getCountryDialCode() == null ? 0 : super.getCountryDialCode().length());
        else
            return 0;
    }

    private static synchronized String updateSubscrValues(int subscrId, int lockedSms, int confirmedSms, int includedSmsCount) throws Exception {

        long newLockedAmount = Conf.getMainRedis().hincrBy(PACK_USAGE_REDIS_KEY + ":" + subscrId, LOCKED_SMS_COUNT_REDIS_FIELD, lockedSms);
        long newUsageAmount = Conf.getMainRedis().hincrBy(PACK_USAGE_REDIS_KEY + ":" + subscrId, CURRENT_USAGE_REDIS_FIELD, confirmedSms);

        if (newLockedAmount + newUsageAmount > includedSmsCount && lockedSms + confirmedSms > 0) {
            //Something went wrong and no available funds, Undo change
            updateSubscrValues(subscrId, -lockedSms, -confirmedSms, includedSmsCount);
            throw new Exception("No available funds for subscrId " + Integer.toString(subscrId));
        }

        return "SubscrId " + Integer.toString(subscrId)
                + ": included " + Integer.toString(includedSmsCount)
                + ", locked " + Long.toString(newLockedAmount - lockedSms) + " -> " + Long.toString(newLockedAmount)
                + (confirmedSms > 0 ? ", used " + Long.toString(newUsageAmount - confirmedSms) + " -> " + Long.toString(newUsageAmount) : "");
    }

    @Override
    void lockFunds(int lockedSms) throws Exception {
        Log.trace(PackSubscription.updateSubscrValues(subscrId, lockedSms, 0, includedSmsCount));
    }

    @Override
    void unlockFunds(String messageId, int unlockedSms, int confirmedSms) throws Exception {
        Log.trace(PackSubscription.updateSubscrValues(subscrId, -unlockedSms, confirmedSms, includedSmsCount));
    }

    private static int getCurrentUsage(int subscrId) {

        Map<String, String> funds;
        try {
            funds = Conf.getMainRedis().hgetAll(PACK_USAGE_REDIS_KEY + ":" + subscrId);
        } catch (Exception e) {
            Log.error("Error getting pack usage from Redis");
            return -1;
        }

        return (funds.containsKey("current_usage") ? Integer.parseInt(funds.get(CURRENT_USAGE_REDIS_FIELD)) : 0)
                + (funds.containsKey("locked_sms_count") ? Integer.parseInt(funds.get(LOCKED_SMS_COUNT_REDIS_FIELD)) : 0);

    }

    static PackSubscription findSubscription(int accountId, String mccmnc, String dnis, int smsCount) {

        PackSubscription packSubscription = null;

        try {

            int maxMatchIndex = 0;

            //Loop for all packages
            for (Map.Entry<String, String> subscrEntry : Conf.getMainRedis().hgetAll(PACK_SUBSCR_REDIS_KEY + ":" + accountId).entrySet()) {

                JsonObject subscrJson = Conf.getJsonParser().parse(subscrEntry.getValue()).getAsJsonObject();
                int packId = Utils.getAsInt(subscrJson, PACK_ID_REDIS_FIELD);
                int subscrId = Integer.parseInt(subscrEntry.getKey());
                int includedSmsCount = Utils.getAsInt(subscrJson, INCLUDED_SMS_COUNT_REDIS_FIELD);

                //Have free funds
                if (includedSmsCount - PackSubscription.getCurrentUsage(subscrId) >= smsCount) {

                    //Loop for mcc in selected package
                    for (Map.Entry<String, String> contentEntry : Conf.getMainRedis().hgetAll(PACK_CONTENT_REDIS_KEY + ":" + packId).entrySet()) {

                        JsonObject contentJson = Conf.getJsonParser().parse(contentEntry.getValue()).getAsJsonObject();
                        PackSubscription tmpSubscr = new PackSubscription(
                                Utils.getAsString(contentJson, COUNTRY_DIAL_CODE_REDIS_FIELD), //countryDialCode,
                                Utils.getAsString(contentJson, MCC_MNC_REDIS_FIELD), //mccmnc,
                                Utils.getStringTail(String.valueOf(accountId), 6), //serviceType,
                                Utils.getAsInt(subscrJson, PRODUCT_ID_REDIS_FIELD), //productId,
                                includedSmsCount, //includedSmsCount
                                Utils.getAsDouble(subscrJson, SUBSCRIPTION_COST_REDIS_FIELD) / includedSmsCount, //ratePerSegment,
                                subscrId, //subscrId,
                                packId, //packId,
                                Utils.getAsInt(subscrJson, PRIORITY_REDIS_FIELD), //priority
                                Utils.getAsString(subscrJson, FORCED_TEXT_REDIS_FIELD) //forced text
                        );

                        if (tmpSubscr.getMatchIndex(mccmnc, dnis) > maxMatchIndex) {
                            maxMatchIndex = tmpSubscr.getMatchIndex(mccmnc, dnis);
                            packSubscription = tmpSubscr;
                        }

                    }

                }

            }

        } catch (Exception e) {
            Log.error("Cannot find pack due to error", e);
        }

        Log.trace("Subscription " + (packSubscription == null ? "not found" : "ID:" + packSubscription.subscrId + " selected")
                + " for " + mccmnc + "/" + dnis
                + " account ID: " + Integer.toString(accountId)
                + " number of SMS: " + Integer.toString(smsCount));

        return packSubscription;
    }

}
