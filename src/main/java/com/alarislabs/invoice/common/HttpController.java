package com.alarislabs.invoice.common;

import com.alarislabs.invoice.payments.AuthorizeNet;
import com.alarislabs.invoice.payments.PayPal;
import com.alarislabs.invoice.payments.Payonline;
import com.alarislabs.invoice.smsmodule.*;
import com.alarislabs.invoice.swagger.SwaggerDocGen;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

/**
 * В данном классе присутствуют методы, реализующие все REST ресурсы
 * Перечень возвращаемых HTTP статусов
 * <ul>
 *     <li/>400 BAD_REQUEST - некорректный Json на входе или другое неправильные запросы
 *     <li/>401 UNAUTHORIZED - ошибка авторизации
 *     <li/>402 PAYMENT_REQUIRED - не достаточно средств для отправки СМС, отсутствует пакет или нет цены в тарифе
 *     <li/>403 FORBIDDEN - доступ запрещен из-за IP адреса или несоответствие авторизационных данных с тем, что отправлено
 *     <li/>406 NOT_ACCEPTABLE - в запросе отправки СМС отсутствует текст
 *     <li/>412 PRECONDITION_FAILED - авторизационные данные пользователя просрочены
 *     <li/>417 EXPECTATION_FAILED - введена направильная captcha
 *     <li/>418 I_AM_A_TEAPOT - попытка отправить СМС на пустой DNIS
 *     <li/>423 LOCKED - номер находится в четном списке
 *     <li/>426 UPGRADE_REQUIRED - просрочен token
 *     <li/>500 INTERNAL_SERVER_ERROR - внутренняя ошибка сервера
 * </ul>
 */
@RestController
@PropertySource("classpath:version.properties")
public class HttpController {

    private static final Logger Log = LoggerFactory.getLogger(HttpController.class);
    private String scmBuildNumber;

    @Value("${version}")
    @SuppressWarnings("unused")
    private String rpmBuildNumber;

    private HttpController() {

        //Get jar scmBuildNumber
        java.util.Properties prop = new java.util.Properties();

        try {
            URL url = getClass().getClassLoader().getResource("META-INF/MANIFEST.MF");
            if (url != null) prop.load(url.openStream());
            scmBuildNumber = prop.getProperty("SCM-Revision");
        } catch (NullPointerException|IOException e) {
            scmBuildNumber = "N/A";
        }

        Log.info("---------------------------------------------");
        Log.info("--                                         --");
        Log.info("--    R E S T M A N       S T A R T E D    --");
        Log.info("--                                         --");
        Log.info("---------------------------------------------");

    }

    private String getVersion() {
        return (Utils.hasValue(rpmBuildNumber) ? rpmBuildNumber : scmBuildNumber);
    }

    @PostConstruct
    @SuppressWarnings("unused")
    private void postConstruct() {
        Log.info("Current version: " + getVersion());
    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/upload", method = RequestMethod.POST, produces = "application/json")
    public
    @ResponseBody
    ResponseEntity<byte[]> handleFileUpload(@RequestParam("file") MultipartFile file) {

        JsonObject responseJson = new JsonObject();
        HttpStatus responseStatus;

        if (!file.isEmpty()) {
            try {

                byte[] randomDir = new byte[32];
                (new SecureRandom()).nextBytes(randomDir);
                responseJson.addProperty("file_id", Utils.bytesToHex(randomDir));

                //Create new directory
                String directory = Conf.getFileExchangeDir() + "/" + responseJson.get("file_id").getAsString();
                if (!(new File(directory)).mkdirs()) Log.info("Directory " + directory + " was not created");

                //Get file extension
                String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")).toLowerCase();

                //Save file to disk
                file.transferTo(new File(directory + "/original" + extension));
                Log.trace("File: " + directory + "/original" + extension + " uploaded");

                responseStatus = HttpStatus.OK;

            } catch (Exception e) {
                Log.error("Failed to upload file", e);
                return Response.newErrInstance("Failed to upload file " + e.getMessage(), HttpStatus.BAD_REQUEST).toEntity();
            }
        } else {
            return Response.newErrInstance("Failed to upload file because the file was empty", HttpStatus.BAD_REQUEST).toEntity();
        }

        return Response.newInstance(responseJson, responseStatus).toEntity();
    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/captcha", method = RequestMethod.GET)
    public ResponseEntity<byte[]> handleCaptcha(HttpServletRequest request) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.set("Content-Type", "image/png");
        return new ResponseEntity<>(CaptchaHelper.getCaptchaAsByteArray(Response.getRemoteAddr(request)), headers, HttpStatus.OK);
    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/download/**", method = RequestMethod.GET)
    public ResponseEntity<byte[]> handleFileDownload(HttpServletRequest request) {

        //Parse URI
        final String uriPrefix = "/rest/download/";
        String uri = request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        Log.trace("Got file download request: " + uri);

        //Init variables
        Path path = Paths.get("/var/www/html/invoice/files/" + uri.substring(uri.indexOf(uriPrefix) + uriPrefix.length()));

        //Check file
        if (!Files.isReadable(path)) return Response.newErrInstance("Cannot read file: " + path.toString(), HttpStatus.BAD_REQUEST).toEntity();

        //Init headers
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Path fileName = path.getFileName();

        if (fileName != null) {
            try {
                headers.set("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName.toString(), StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                headers.set("Content-Disposition", "attachment; filename=file." + fileName);
            }
            headers.set("Content-Type", Utils.getFileMimeType(fileName.toString()));
        }

        //Return file content
        try {
            return new ResponseEntity<>(Files.readAllBytes(path), headers, HttpStatus.OK);
        } catch (IOException e) {
            return Response.newInstance("Cannot read data from file: " + path.toString(), HttpStatus.BAD_REQUEST).toEntity();
        }


    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/resolve_dnis", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<byte[]> handleResolveDnis(
            @RequestParam String dnis,
            HttpServletRequest request) {

        Trace.traceRequest(request, null);
        JsonObject responseJson = new JsonObject();

        //Extract authorization data
        Auth auth = new Auth(request.getHeader("authorization"), Response.getRemoteAddr(request));

        if (!auth.getAuthType().equals(AuthType.Bearer)) {
            return Response.newErrInstance("Bearer authorization required", HttpStatus.UNAUTHORIZED).toEntity();
        }

        try {
            responseJson.addProperty("mccmnc", MccmncResolver.getMccmnc(dnis));
        } catch (Exception e) {
            return Response.newErrInstance("Error resolving dnis: " + e.getMessage(), HttpStatus.BAD_REQUEST).toEntity();
        }

        return Response.newInstance(responseJson, HttpStatus.OK).toEntity();

    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/bulk_send_sms", produces = "application/json")
    public ResponseEntity<byte[]> handleBulkSendSms(
            @RequestBody String smsArray,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false, defaultValue = "0") int acc_id,
            @RequestParam(required = false, defaultValue = "0") int campaign_id,
            @RequestParam(required = false) String message_split_mode,
            @RequestParam(required = false, defaultValue = "0") String show_details,
            HttpServletRequest request) {

        Trace.traceRequest(request, null);

        JsonArray smsJsonArray;
        try {
            smsJsonArray = Conf.getJsonParser().parse(smsArray).getAsJsonArray();
        } catch (Exception e) {
            return Response.newErrInstance("Invalid JSON Array of SMS: " + smsArray, HttpStatus.BAD_REQUEST).toEntity();
        }

        return SmsSender.sendBulkSms(smsJsonArray, username, password, acc_id, campaign_id, message_split_mode, request, show_details.equals("1")).toEntity();

    }


    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/send_sms", produces = "application/json")
    public ResponseEntity<byte[]> handleSendSms(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String message,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestParam(required = false, defaultValue = "0") int acc_id,
            @RequestParam(required = false, defaultValue = "0") int campaign_id,
            @RequestParam(required = false) String message_split_mode,
            HttpServletRequest request) {

        Trace.traceRequest(request, null);
        return SmsSender.sendSms(from, to, message, username, password, acc_id, campaign_id, message_split_mode, request).toEntity();
    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/info", produces = "application/json")
    public ResponseEntity<byte[]> handleInfo(HttpServletRequest request) {

        JsonObject json = new JsonObject();

        json.addProperty("version", getVersion());
        json.addProperty("time", Conf.now().format(Utils.sqlFormatter));
        json.addProperty("utc_time", LocalDateTime.now(ZoneId.of("UTC")).format(Utils.sqlFormatter));
        json.addProperty("server_tz", Conf.getSystemZoneId().getId());
        json.addProperty("remote_address", Response.getRemoteAddr(request));

        return Response.newInstance(json, HttpStatus.OK).toEntity();

    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/swagger.json", produces = "application/json")
    public ResponseEntity<byte[]> handleSwaggerDoc() {

        SwaggerDocGen doc;
        try {
            doc = new SwaggerDocGen();
            doc.setVersion(getVersion());
        } catch (Exception e) {
            Log.error("Error creating SwaggerDocGen object", e);
            return Response.newErrInstance("Error creating SwaggerDocGen object", HttpStatus.INTERNAL_SERVER_ERROR).toEntity();
        }

        return Response.newInstance(doc.getSwaggerJson(), HttpStatus.OK).toEntity();

    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/ipn", produces = "text/html")
    public ResponseEntity<byte[]> handleIpn(@RequestBody String requestBody, HttpServletRequest request) {

        Trace.traceRequest(request, null);
        return Conf.getOracleConn().postPayment(new PayPal(requestBody, request), true).toEntity();

    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/authorize_payment", produces = "text/html")
    public ResponseEntity<byte[]> handleAuthorize(HttpServletRequest request) {

        Trace.traceRequest(request, null);
        return Conf.getOracleConn().postPayment(new AuthorizeNet(request), true).toEntity();

    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/payonline_hash", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<byte[]> handlePayonlineHash(HttpServletRequest request) {

        Trace.traceRequest(request, null);
        return Payonline.getHash(request).toEntity();
    }


    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/payonline_payment", produces = "text/html")
    public ResponseEntity<byte[]> handlePayonlinePayment(HttpServletRequest request) {

        Trace.traceRequest(request, null);
        return Conf.getOracleConn().postPayment(new Payonline(request), true).toEntity();

    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/sms_segment_count", produces = "application/json")
    public ResponseEntity<byte[]> handleSmsSegmentsCountRequest(@RequestParam String text, HttpServletRequest request) {

        JsonObject responseJson = new JsonObject();
        Log.trace("Text supplied: " + text);
        responseJson.addProperty("segment_count", AuxUtils.getPartAmount(text));
        return Response.newInstance(responseJson, HttpStatus.OK).toEntity();

    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/rest/**", produces = "application/json")
    public ResponseEntity<byte[]> handleApiRequest(@RequestBody(required = false) String params, HttpServletRequest request) {

        try {

            JsonObject requestJson;

            //Try to parse params
            if (params != null) {
                try {
                    requestJson = Conf.getJsonParser().parse(params).getAsJsonObject();
                } catch (Exception e) {
                    return Response.newErrInstance("Invalid JSON: " + params, HttpStatus.BAD_REQUEST).toEntity();
                }
            } else {
                requestJson = new JsonObject();
            }

            //Trace request if enabled
            Trace.traceRequest(request, requestJson.toString());

            //Get rows
            List<String> rowsArray = new ArrayList<>();
            if (requestJson.has("rows") && requestJson.get("rows").isJsonArray()) {

                for(JsonElement element : requestJson.getAsJsonArray("rows")) {
                    rowsArray.add(element.isJsonPrimitive() ? element.getAsString() : element.toString());
                }
                requestJson.remove("rows");
            }

            //Get parameters from request
            Enumeration enumeration = request.getParameterNames();
            while (enumeration.hasMoreElements()) {
                String parameterName = (String) enumeration.nextElement();
                requestJson.addProperty(parameterName, request.getParameter(parameterName));
            }

            //Add implicit parameters
            String uri = request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
            requestJson.addProperty("uri", uri);
            requestJson.addProperty("method", request.getMethod());

            String remoteAddr = Response.getRemoteAddr(request);

            //Check captcha if exists
            if (requestJson.has("captcha")
                    && !CaptchaHelper.isCorrect(Utils.getAsString(requestJson, "captcha"), remoteAddr)
                    && !Conf.isTrustedIp(remoteAddr)) {
                return Response.newErrInstance("Wrong captcha value", HttpStatus.EXPECTATION_FAILED).toEntity();
            }


            requestJson.addProperty("remote_addr", remoteAddr);
            requestJson.addProperty("host", request.getHeader("Host"));
            requestJson.addProperty("origin", request.getHeader("Origin"));

            //Extract authorization data
            Auth auth = new Auth(request.getHeader("authorization"), remoteAddr);

            //Check authorization
            if (auth.getAuthType().equals(AuthType.Unauthorized)) {
                return Response.newErrInstance(auth.getAuthData(), HttpStatus.UNAUTHORIZED).toEntity();
            } else if (auth.getAuthType().equals(AuthType.Expired)) {
                return Response.newErrInstance(auth.getAuthData(), HttpStatus.UPGRADE_REQUIRED).toEntity();
            }

            //Set auth parameters
            requestJson.addProperty("auth_type", auth.getAuthType().toString());
            requestJson.addProperty("auth_data", auth.getAuthData());

            //Return response
            boolean addTotalCountHeader = request.getHeader(Response.QUERY_TOTAL_COUNT_HEADER) != null;
            Response response = Conf.getOracleConn().getResponseJson(
                    requestJson,
                    true,
                    rowsArray,
                    addTotalCountHeader
            );

            //Trace response if enabled
            Trace.traceResponse(response);

            return response.toEntity();

        } catch (Exception e) {
            Log.info("BAD request", e);
            return Response.newErrInstance(e.getMessage(), HttpStatus.BAD_REQUEST).toEntity();
        }
    }


    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/sms_array_cost", produces = "application/json")
    public ResponseEntity<byte[]> handleSendSms(
            @RequestParam int acc_id,
            @RequestParam String text,
            @RequestBody String content,
            HttpServletRequest request) {

        Trace.traceRequest(request, content);

        JsonObject response = new JsonObject();
        response.addProperty("cost", Sms.getSmsArrayCost(acc_id, text, content));
        return Response.newInstance(response, HttpStatus.OK).toEntity();
    }


    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/reload_configuration", produces = "text/html")
    public ResponseEntity<byte[]> handleReloadConfiguration(@RequestParam(required = false) String newConfigPath, HttpServletRequest request) {

        String remoteAddr = Response.getRemoteAddr(request);

        if (!Conf.isTrustedIp(remoteAddr)) {
            return Response.newErrInstance("Host " + remoteAddr + " requires authorization", HttpStatus.FORBIDDEN).toEntity();
        }
        else {

            if (newConfigPath != null) {
                Conf.setConfigPath(newConfigPath);
            }


            if (Conf.reload()) return Response.newInstance("Configuration successfully reloaded", HttpStatus.OK).toEntity();
            else {
                return Response.newInstance("Cannot reload application configuration. Exiting...", HttpStatus.OK).toEntity();
            }
        }
    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/db_test", produces = "text/html")
    public ResponseEntity<byte[]> handleDbTest() {

        if (Conf.getOracleConn().isValidConnection()) {
            return Response.newInstance("OK", HttpStatus.OK).toEntity();
        }
        else {
            return Response.newErrInstance("DB connection is lost...", HttpStatus.INTERNAL_SERVER_ERROR).toEntity();
        }

    }

    @Async
    @SuppressWarnings("unused")
    @RequestMapping(value = "/self_test", produces = "text/html")
    public ResponseEntity<byte[]> handleSelfTest() {

        return Response.newInstance("OK", HttpStatus.OK).toEntity();

    }

}