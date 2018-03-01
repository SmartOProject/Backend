package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.Utils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class SmsRate extends AbstractRate {

    //Keys
    private final static String SMS_RATE_REDIS_KEY = "sms_rate";
    final static String ACCOUNT_REDIS_KEY = "account";
    private final static String EDR_LOCK_REDIS_KEY = "account_lock";

    //Fields
    private final static String EDR_LOCK_ACC_ID_REDIS_FIELD = "acc_id";
    private final static String EDR_LOCK_COST_REDIS_FIELD = "cost";
    private final static String ACCOUNT_LOCK_AMOUNT_REDIS_FIELD = "locked_amount";
    private final static String ACCOUNT_BALANCE_REDIS_FIELD = "balance";
    private final static String ACCOUNT_IN_CREDIT_LIMIT_REDIS_FIELD = "in_credit_limit";
    private final static String EDR_LOCK_UNIX_TIME_FIELD = "unix_time";
    private final static String RATE_DIAL_CODE_REDIS_FIELD = "dial_code";
    private final static String RATE_START_DATE_REDIS_FIELD = "start_date";
    private final static String RATE_END_DATE_REDIS_FIELD = "end_date";
    private final static String RATE_VALUE_REDIS_FIELD = "rate";
    private final static Logger Log = LoggerFactory.getLogger(SmsRate.class);

    int getRateId() {
        return rateId;
    }

    private final int accId;
    @SuppressWarnings("all")
    private final int rateId;
    @SuppressWarnings("all")
    private final int effectiveProductId;
    private final transient LocalDateTime startDate;
    private final transient LocalDateTime endDate;

    private SmsRate(Product product, String mccmnc, String countryDialCode, double ratePerSegment, int accId, int rateId, LocalDateTime startDate, LocalDateTime endDate) {

        super(countryDialCode, mccmnc, Utils.getStringTail(String.valueOf(product.getParentProductId()), 6), product.getProductId(), ratePerSegment);
        this.accId = accId;
        this.rateId = rateId;
        this.effectiveProductId = product.getEffectiveProductId();
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private static double getAvailableFunds(int accId) throws Exception {

        Map<String, String> map = Conf.getMainRedis().hgetAll(ACCOUNT_REDIS_KEY + ":" + accId);

        return (map.containsKey(ACCOUNT_BALANCE_REDIS_FIELD) ? Double.parseDouble(map.get(ACCOUNT_BALANCE_REDIS_FIELD)) : 0)
                - (map.containsKey(ACCOUNT_LOCK_AMOUNT_REDIS_FIELD) ? Double.parseDouble(map.get(ACCOUNT_LOCK_AMOUNT_REDIS_FIELD)) : 0)
                + (map.containsKey(ACCOUNT_IN_CREDIT_LIMIT_REDIS_FIELD) ? Double.parseDouble(map.get(ACCOUNT_IN_CREDIT_LIMIT_REDIS_FIELD)) : 0);

    }

    private static synchronized String updateBalance(int accId, double lockedFunds, double confirmedFunds, String messageId) throws Exception {

        String miniReport = "Account ID " + accId;

        //Check availableFunds
        double availableFunds = getAvailableFunds(accId);
        if (lockedFunds > 0 && availableFunds - lockedFunds < 0) {
            throw new Exception("Insufficient funds for account ID " + accId + ", available funds including credit limit " + availableFunds + ", required " + lockedFunds);
        }

        if (confirmedFunds == 0 && lockedFunds != 0) {

            //Initial lock or not successful SMS, No CDR expected, just remove lock
            double newLockValue = Conf.getMainRedis().hincrByFloat(ACCOUNT_REDIS_KEY + ":" + accId, ACCOUNT_LOCK_AMOUNT_REDIS_FIELD, lockedFunds);
            miniReport = miniReport + ", locked amount " + (newLockValue - lockedFunds) + " -> " + newLockValue;

        } else if (confirmedFunds > 0) {

            //Add edr_lock:<message_id>, locked funds will be released from database
            Map<String,String> map = new HashMap<>();

            map.put(EDR_LOCK_ACC_ID_REDIS_FIELD, Integer.toString(accId));
            map.put(EDR_LOCK_COST_REDIS_FIELD, Double.toString(confirmedFunds));
            map.put(EDR_LOCK_UNIX_TIME_FIELD, Long.toString(System.currentTimeMillis()/1000));

            Conf.getMainRedis().hmset(EDR_LOCK_REDIS_KEY + ":" + messageId, map);
            miniReport = miniReport + ", edr lock " + messageId + " -> " + confirmedFunds;
        }

        return miniReport;

    }

    @Override
    void lockFunds(int lockedSms) throws Exception {
        Log.trace(updateBalance(accId, super.getRatePerSegment() * lockedSms, 0, null));
    }

    @Override
    void unlockFunds(String messageId, int unlockedSms, int confirmedSms) throws Exception {
        Log.trace(updateBalance(accId, - super.getRatePerSegment() * unlockedSms, super.getRatePerSegment() * confirmedSms, messageId));
    }

    private int getMatchIndex(String dnis) {

        if (!dnis.startsWith(super.getCountryDialCode()) || !Conf.now().isAfter(startDate) || !Conf.now().isBefore(endDate))
            return 0;

        return Conf.isGlobalLongestMatch()
                ? super.getMccmnc().length() * 1000 + super.getCountryDialCode().length() * 10 + (effectiveProductId == super.getProductId() ? 1 : 0)
                : (effectiveProductId == super.getProductId() ? 1000 : 0) + super.getMccmnc().length() * 100 + super.getCountryDialCode().length();
    }

    static SmsRate getRate(int accId, String mccmnc, String dnis) {

        //Prepare array of products
        try {

            ArrayList<Product> products = new ArrayList<>();
            String accountKey  = ACCOUNT_REDIS_KEY + ":" + Integer.toString(accId);

            for (Map.Entry<String, String> productEntry : Conf.getMainRedis().hgetAll(accountKey).entrySet()) {

                try {
                    //Add productId
                    if (productEntry.getKey().matches("product:\\d*")) {

                        int productId = Integer.parseInt(productEntry.getKey().split(":")[1]);
                        int parentProductId = Integer.parseInt(productEntry.getValue());

                        products.add(new Product(productId, parentProductId, productId));
                        if (parentProductId > 0) {
                            products.add(new Product(productId, parentProductId, parentProductId));
                        }
                    }
                } catch (Exception e) {
                    Log.error("Failed to get products from " + accountKey + " redis key");
                }

            }

            //Prepare list of mccmnc
            ArrayList<String> mccmncList = new ArrayList<>();
            mccmncList.add(mccmnc);
            if (mccmnc.length() == 6) mccmncList.add(mccmnc.substring(0,3)); //Add mcc if mccmnc has length 6


            SmsRate smsRate = null;
            int maxMatchIndex = 0;
            for(String mccmncElement : mccmncList) {
                for (Product product : products) {


                    String rateKey = SMS_RATE_REDIS_KEY + ":" + Integer.toString(product.getEffectiveProductId()) + ":" + mccmncElement;
                    for (Map.Entry<String, String> rateEntry : Conf.getMainRedis().hgetAll(rateKey).entrySet()) {

                        JsonObject rateEntryJson;
                        try {
                            rateEntryJson = Conf.getJsonParser().parse(rateEntry.getValue()).getAsJsonObject();
                        } catch ( com.google.gson.JsonSyntaxException syntaxEx) {
                            Log.error("Value for key " + rateKey + "." + rateEntry.getKey() + " corrupted. Please run prv_rr_utils.sync_to_redis_job(true) to fix it.");
                            continue;
                        }

                        SmsRate tempRate = new SmsRate(
                                product, //product
                                mccmncElement, //mccmnc
                                Utils.getAsString(rateEntryJson, RATE_DIAL_CODE_REDIS_FIELD), //countryDialCode
                                Utils.getAsDouble(rateEntryJson, RATE_VALUE_REDIS_FIELD), //ratePerSegment
                                accId, // accId
                                Integer.parseInt(rateEntry.getKey()), // rateId
                                Utils.getAsDateTime(rateEntryJson, RATE_START_DATE_REDIS_FIELD), // startDate
                                Utils.getAsDateTime(rateEntryJson, RATE_END_DATE_REDIS_FIELD) // endDate
                        );


                        int matchIndex = tempRate.getMatchIndex(dnis);
                        if (matchIndex > maxMatchIndex) {
                            maxMatchIndex = matchIndex;
                            smsRate = tempRate;
                        }
                    }
                }
            }
            Log.trace(smsRate == null ? "Rate not found" : "Found Rate ID: " + smsRate.rateId);
            return smsRate;

        } catch (Exception e) {
            Log.error("Cannot get SMS ratePerSegment due to Redis error: " + e.getMessage());
            return null;
        }

    }


}
