package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

class Channel {

    @Override
    public int hashCode() {
        int result = channelSystemId != null ? channelSystemId.hashCode() : 0;
        result = 31 * result + (channelPassword != null ? channelPassword.hashCode() : 0);
        return result;
    }

    private final String channelSystemId;
    private final String channelPassword;
    private static final Logger Log = LoggerFactory.getLogger(Channel.class);
    private static final String CHANNEL_REDIS_KEY = "sms_channel";
    private static final String SYSTEMID_REDIS_FIELD = "channel_systemid";
    private static final String PASSWORD_REDIS_KEY = "channel_password";

    private Channel(String channelSystemId, String channelPassword) {
        this.channelSystemId = channelSystemId;
        this.channelPassword = channelPassword;
    }

    static Channel getChannel(int channelId) {

        String key = CHANNEL_REDIS_KEY + ":" + Integer.toString(channelId);

        try {

            if (Conf.getMainRedis().exists(key)) {
                Map<String, String> map = Conf.getMainRedis().hgetAll(key);
                return new Channel(map.get(SYSTEMID_REDIS_FIELD), map.get(PASSWORD_REDIS_KEY));
            }

        } catch (Exception e) {
            Log.error("Cannot find Channel due to Redis access problems: " + e.getMessage());
        }

        return null;
    }

    String getChannelSystemId() {
        return channelSystemId;
    }

    String getChannelPassword() {
        return channelPassword;
    }


}
