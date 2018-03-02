package com.smarto.rest.common;
import nl.captcha.Captcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

class CaptchaHelper {

    private static final Logger Log = LoggerFactory.getLogger(CaptchaHelper.class);
    private static final String CAPTCHA_REDIS_KEY = "captcha:";

    static byte[] getCaptchaAsByteArray(String remoteHost) {

        Captcha captcha = new Captcha.Builder(130, 50)
            .addText()
            .addNoise()
            .build();

        Log.info("Captcha answer: " + captcha.getAnswer());

        byte[] imageInByte;
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            ImageIO.write(captcha.getImage(), "png", outStream);
            outStream.flush();
            imageInByte = outStream.toByteArray();
        } catch (IOException e) {
            Log.error("IO exception writing image: " + e.getMessage());
            return null;
        }

        try {
            Conf.getMainRedis().set(CAPTCHA_REDIS_KEY + captcha.getAnswer() + ":" + remoteHost, "", 600);
        } catch (Exception e) {
            Log.error("Cannot write captcha key to Redis: " + e.getMessage());
            return null;
        }

        return imageInByte;
    }

    static boolean isCorrect(String answer, String remoteHost) {
        try {
            String key = CAPTCHA_REDIS_KEY + answer + ":" + remoteHost;
            boolean exists = Conf.getMainRedis().exists(key);
            if (exists) Conf.getMainRedis().del(key);
            return exists;
        } catch (Exception e) {
            Log.error("Cannot check captcha key in Redis: " + e.getMessage());
            return false;
        }
    }


}
