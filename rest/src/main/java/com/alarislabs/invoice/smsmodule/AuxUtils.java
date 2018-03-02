package com.alarislabs.invoice.smsmodule;

import com.alarislabs.invoice.common.Conf;
import com.alarislabs.invoice.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

public class AuxUtils {

    private static final String GSM_CHARACTERS_REGEX = "^[A-Za-z0-9 \\\\@ΔSP0¡¿p£_!$Φ\"¥Γ#èΛ¤éΩ%uùΠ&ìΨ'òΣ\\(ÇΘ\\rΞ*:Ø\\)+;ÄkäøÆ,<Ölö\\næ\\-=MÑmñÅß.>NÜnüåÉ/?O§oà|^€}{\\[~\\]]*$";
    private static final Pattern gmsCharsetPattern = Pattern.compile(GSM_CHARACTERS_REGEX);
    private static final Logger Log = LoggerFactory.getLogger(AuxUtils.class);


    public static GsmDataEncoding getDataEncoding(String text) {
        return (gmsCharsetPattern.matcher(text).matches() ? GsmDataEncoding.GSM7 : GsmDataEncoding.USC2);
    }

    public static int getPartAmount(String text) {

        if (!Utils.hasValue(text)) return 0;

        int messageLen = text.length();

        switch (getDataEncoding(text)) {

            case GSM7:
                if (messageLen > 160) {
                    return (messageLen - 1)/ 152 + 1;
                } else {
                    return 1;
                }
            case USC2:
                if (messageLen > 70) {
                    return (messageLen - 1) / 67 + 1;
                } else {
                    return 1;
                }

            default:
                //Unsupported encoding
                return 1;
        }

    }

    static boolean bnumberInBlackList(int carId, String dnis) {

        try {

            return Conf.getMainRedis().exists("sms_black_list:" + Integer.toString(carId) + ":" + dnis);

        } catch (Exception e) {
            Log.error("Cannot check bnumber in black list", e);
            return false;
        }

    }

    static void assureSwitchGotConfiguration(LocalDateTime confLastUpdateDate) {

        if (confLastUpdateDate != null) {

            long remainTime = 60000L - confLastUpdateDate.until(Conf.now(), ChronoUnit.MILLIS);

            //Limit reached, time remained: sleep
            if (remainTime > 0) {
                try {
                    //Sleep
                    Log.info("Waiting to be sure that switch got configuration for " + remainTime
                            + " milliseconds, account configuration last update date: " + confLastUpdateDate.toString());
                    Thread.sleep(remainTime);
                } catch (InterruptedException e) {
                    Log.info("Thread.sleep was interrupted with message: " + e.getMessage());
                }
            }
        }
    }
}
