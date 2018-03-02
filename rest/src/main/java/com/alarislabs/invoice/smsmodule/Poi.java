package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.Utils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Map;

class Poi {

    private int poiId;
    private int channelId;
    private static final Logger Log = LoggerFactory.getLogger(Poi.class);

    private static final String POI_REDIS_KEY = "sms_poi";
    private static final String POI_START_DATE_REDIS_FIELD = "start_date";
    private static final String POI_END_DATE_REDIS_FIELD = "end_date";
    private static final String POI_CHANNEL_ID_REDIS_FIELD = "channel_id";


    private Poi(int poiId, int channelId) {
        this.poiId = poiId;
        this.channelId = channelId;
    }

    static Poi getPoi(int productId, String serviceType) {

        String key = POI_REDIS_KEY + ":" + Integer.toString(productId) + ":" + serviceType;

        try {

            for (Map.Entry<String, String> poiEntry : Conf.getMainRedis().hgetAll(key).entrySet()) {

                //Get entry
                JsonObject poiEntryJson = Conf.getJsonParser().parse(poiEntry.getValue()).getAsJsonObject();
                LocalDateTime startDate = Utils.getAsDateTime(poiEntryJson, POI_START_DATE_REDIS_FIELD);
                LocalDateTime endDate = Utils.getAsDateTime(poiEntryJson, POI_END_DATE_REDIS_FIELD);

                if (startDate != null
                        && endDate != null
                        && Conf.now().isAfter(startDate)
                        && Conf.now().isBefore(endDate)) {
                    return new Poi(Integer.parseInt(poiEntry.getKey()), Utils.getAsInt(poiEntryJson, POI_CHANNEL_ID_REDIS_FIELD));
                }

            }

        } catch (Exception e) {
            Log.error("Cannot get SMS Poi due to Redis error: " + e.getMessage());
        }


        return null;

    }

    int getPoiId() {
        return poiId;
    }

    int getChannelId() {
        return channelId;
    }

}
