package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MccmncResolver {

    private static final Logger Log = LoggerFactory.getLogger(MccmncResolver.class);

    public static String getMccmnc(String dialCode) throws Exception {

        //Check e164 pattern
        if (Conf.getE164Pattern() != null && !Conf.getE164Pattern().matcher(dialCode).matches()) {
            throw new Exception(String.format("Destination address %s does not match e.164 pattern %s", dialCode, Conf.getE164Pattern().toString()));
        }

        try {

            for (int i = dialCode.length(); i > 0; i--) {

                String mccmnc = Conf.getMainRedis().get("sms_dc_ref:" + dialCode.substring(0, i));
                if (mccmnc != null) {
                    Log.trace("Resolved mccmnc: " + mccmnc);
                    return mccmnc;
                }

            }

        } catch (Exception e) {
            Log.error("Error resolving dial code", e);
        }

        return "0";

    }
}
