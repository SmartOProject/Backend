package com.smarto.rest.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@EnableScheduling
@Component
public class Conf {

    private static final Logger Log = LoggerFactory.getLogger(Conf.class);
    private static final String DEBUG_MODE_PN = "PARAM-DEBUG_MODE";
    private static final String SYSTEM_NAME_PN = "FIN-SYSTEM-OWNER-NAME";
    private static final String PAYPAL_URL_PN = "CLP-PAYPAL_URL";
    private static final String TRUSTED_ADDR_LIST_PN = "SMS-RTL-TRUSTED_ADDR_LIST";
    private static final String SMS_URL_TEMPLATE_PN = "SMS-RTL-URL-TEMPLATE";
    private static final String DEF_SMS_SPLIT_MODE_PN = "CLP-DEF_SMS_SPLIT_MODE";
    private static final String SYS_TIMEZONE_NAME_PN = "PARAM-SYS-TIMEZONE-NAME";
    private static final String SMS_THREAD_COUNT_PN = "CLP-SMS_THREAD_COUNT";
    private static final String SMS_CAMPAING_MESSAGE_LIMIT_PN = "CLP-SMS_CAMPAING_MESSAGE_LIMIT";
    private static final String RESPONSE_RECORD_COUNT_LIMIT_PN = "PARAM-RESPONSE_RECORD_COUNT_LIMIT";
    private static final String DEFAULT_REDIS_HOST_PN = "PARAM-DEFAULT_REDIS_HOST";
    private static final String DEFAULT_REDIS_PORT_PN = "PARAM-DEFAULT_REDIS_PORT";
    private static final String DEFAULT_REDIS_PWD_PN = "PARAM-DEFAULT_REDIS_PWD";
    private static final String TRACE_REQUEST_PN = "PARAM-REST_API_TRACE_REQUEST";
    private static final String TRACE_RESPONSE_PN = "PARAM-REST_API_TRACE_RESPONSE";
    private static final String PAYONLINE_MERCHANT_PN = "CLP-PAYONLINE_MERCHANT_ID";
    private static final String PAYONLINE_PRIVATE_SECURITY_KEY_PN = "CLP-PAYONLINE_PRIVATE_SECURITY_KEY";
    private static final String E164_PATTERN = "CLP-E164_PATTERN_JAVA";
    private static final String RATES_INH_MODE = "SMSRATE-RATES_INH_MODE";

    private static String configPath;

    private static final JsonParser jsonParser = new JsonParser();

    //Database connections
    private static RedisAdapter mainRedis;
    private static OracleAdapter oracleConn;

    //System params
    private static Map<String, String> systemParams = new HashMap<>();

    //Global variables
    private static ZoneId systemZoneId = ZoneId.systemDefault();
    private static String fileExchangeDir;
    private static int responseRecordCountLimit;

    private static Pattern e164Pattern;

    //Billing options
    private static boolean globalLongestMatch;

    private static boolean lastReloadSucceeded = false;

    static boolean reload() {

        String connectionString, userName, password;

        //Load settings from configuration file
        try {

            if (!Utils.hasValue(configPath)) configPath = "/home/ec2-user/restman/config/restman.conf";

            Log.info("Using config file: " + configPath);

            JsonObject fileConfJson = Conf.getJsonParser().parse(Utils.readFile(configPath)).getAsJsonObject();

            connectionString = fileConfJson.get("ConnectionString").getAsString();
            userName = fileConfJson.get("Username").getAsString();
            password = fileConfJson.get("Password").getAsString();
            fileExchangeDir = fileConfJson.get("FileExchangeDirectory").getAsString();

        } catch (Exception e) {
            Log.error("Cannot load configuration file " + configPath + " or required parameters missing: ", e);
            return false;
        }

        //Connect or reconnect to DB
        if (oracleConn == null
                || !connectionString.equals(oracleConn.getConnectionString())
                || !userName.equals(oracleConn.getUserName())
                || !password.equals(oracleConn.getPassword())
                || !oracleConn.isValidConnection()) {

            if (oracleConn != null) oracleConn.disconnect();
            oracleConn = new OracleAdapter(connectionString, userName, password);
            oracleConn.initPool();
        }

        //Get DB parameters
        Map<String, String> newValues = oracleConn.getSystemParams();

        boolean doDefRedisReconnect = false;

        if (newValues != null) {

            //Log changed parameters
            Log.info("Reload system parameters");
            for(String pn : newValues.keySet()) {

                if (!systemParams.containsKey(pn) || !newValues.get(pn).equals(systemParams.get(pn))) {

                    //Update parameter value
                    systemParams.put(pn, newValues.get(pn));
                    Log.info(pn + ": " + systemParams.get(pn));

                    //Detect Redis settings change
                    if (pn.equals(DEFAULT_REDIS_HOST_PN) || pn.equals(DEFAULT_REDIS_PORT_PN) || pn.equals(DEFAULT_REDIS_PWD_PN))
                        doDefRedisReconnect = true;

                    //Set logging level
                    if (pn.equals(DEBUG_MODE_PN)) {
                        LoggerLevel.setLevel(systemParams.get(DEBUG_MODE_PN).equals("true") ? Level.TRACE : Level.INFO);
                    }

                    //Set timezone
                    if (pn.equals(SYS_TIMEZONE_NAME_PN)) {
                        systemZoneId = ZoneId.of(systemParams.get(SYS_TIMEZONE_NAME_PN));
                        Log.info("Current system timezone: " + systemZoneId.getId() + ", datetime: " + now().format(Utils.sqlFormatter));
                    }

                    if (pn.equals(RESPONSE_RECORD_COUNT_LIMIT_PN)) {
                        responseRecordCountLimit = 10000;
                    }

                    //Set request/response tracing
                    if (pn.equals(TRACE_REQUEST_PN)) {
                        Trace.setEnableTraceRequest(systemParams.get(TRACE_REQUEST_PN).equals("1"));
                    }

                    if (pn.equals(TRACE_RESPONSE_PN)) {
                        Trace.setEnableTraceResponse(systemParams.get(TRACE_RESPONSE_PN).equals("1"));
                    }

                    if (pn.equals(E164_PATTERN)) {
                        try {
                            e164Pattern = Pattern.compile(systemParams.get(E164_PATTERN), Pattern.CASE_INSENSITIVE);
                        } catch (Exception e) {
                            Log.error("Invalid E.164 number pattern (java format): " + e.getMessage());
                            e164Pattern = null;
                        }
                    }


                    if (pn.equals(RATES_INH_MODE)) {
                        globalLongestMatch = systemParams.get(RATES_INH_MODE).equals("1");
                    }

                }
            }


            //Connect to main Redis
            if (doDefRedisReconnect) {

                if (mainRedis != null) mainRedis.destroyPoolInstance();

                mainRedis = new RedisAdapter(
                        systemParams.get(DEFAULT_REDIS_HOST_PN),
                        Integer.parseInt(systemParams.get(DEFAULT_REDIS_PORT_PN)),
                        systemParams.get(DEFAULT_REDIS_PWD_PN)
                );
                Conf.getMainRedis().testConnection("Default Redis");
            }

            return true;
        } else {
            return false;
        }
    }

    public static JsonParser getJsonParser() { return jsonParser; }

    public static OracleAdapter getOracleConn() {
        return oracleConn;
    }

    public static RedisAdapter getMainRedis() {
        return mainRedis;
    }

    static String getFileExchangeDir() {
        return fileExchangeDir;
    }

    public static String getPaypalUrl() {
        return systemParams.get(PAYPAL_URL_PN);
    }

    public static boolean isTrustedIp(String remoteAddr) {
        return ("," + systemParams.get(TRUSTED_ADDR_LIST_PN) + ",").contains("," + remoteAddr  + ",");
    }

    public static String getSmsSwitchUrl() { return systemParams.get(SMS_URL_TEMPLATE_PN); }

    public static String getDefMessageSplitMode() {
        return Utils.nvl(systemParams.get(DEF_SMS_SPLIT_MODE_PN), "split");
    }

    public static String getSystemName() { return systemParams.get(SYSTEM_NAME_PN); }

    public static LocalDateTime now() {
        return LocalDateTime.now(systemZoneId);
    }

    static ZoneId getSystemZoneId() {
        return systemZoneId;
    }

    public static int getSmsThreadCount() {
        return Utils.nvl(Integer.parseInt(systemParams.get(SMS_THREAD_COUNT_PN)), 12);
    }

    public static int getSmsSpeedLimit() {
        return Utils.nvl(Integer.parseInt(systemParams.get(SMS_CAMPAING_MESSAGE_LIMIT_PN)), 10);
    }

    public static String getPayonlineMerchantPnId() {
        return systemParams.get(PAYONLINE_MERCHANT_PN);
    }

    public static String getPayonlinePrivateSecurityKey() {
        return systemParams.get(PAYONLINE_PRIVATE_SECURITY_KEY_PN);
    }

    public static Pattern getE164Pattern() { return e164Pattern; }

    static int getResponseRecordCountLimit() {
        return 10000;
    }

    static void setConfigPath(String configPath) {
        Conf.configPath = configPath;
    }

    public static boolean isGlobalLongestMatch() {
        return globalLongestMatch;
    }

    @Scheduled(fixedRate=30000)
    @SuppressWarnings("unused")
    public void check() {

        if (oracleConn == null || !oracleConn.isValidConnection() || mainRedis == null || /*switchRedis == null || */!lastReloadSucceeded) {

            lastReloadSucceeded = reload();

        } else {
            Log.trace("DB connection is Ok");
        }

    }

}
