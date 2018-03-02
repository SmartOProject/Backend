package com.alarislabs.invoice.common;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;


public class Utils {

    public final static DateTimeFormatter sqlFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    public final static DateTimeFormatter isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public final static DateTimeFormatter shortFormatter = DateTimeFormatter.ofPattern("yyMMddHHmm");
    public final static DateTimeFormatter formatterWithoutSeconds = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final Logger Log = LoggerFactory.getLogger(Utils.class);

    public static String getStringTail(String str, int length) {
        return str.substring(str.length() <= length ? 0 : str.length() - length);
    }

    public static String getAsString(JsonElement json, String key) {
        JsonElement e = json.getAsJsonObject().get(key);
        return e == null ? null : e.isJsonObject() ? e.toString(): e.isJsonNull() ? null : e.isJsonArray() ? e.getAsJsonArray().toString() : e.getAsJsonPrimitive().getAsString();
    }

    public static int getAsInt(JsonElement json, String key) {
        String val = getAsString(json, key);
        return (Utils.hasValue(val) ? Integer.parseInt(val): 0);
    }

    public static float getAsFloat(JsonElement json, String key) {
        String val = getAsString(json, key);
        return (Utils.hasValue(val) ? Float.parseFloat(val): 0.0f);
    }

    public static double getAsDouble(JsonElement json, String key) {
        String val = getAsString(json, key);
        return (Utils.hasValue(val) ? Double.parseDouble(val): 0.0d);
    }

    public static LocalDateTime getAsDateTime(JsonElement json, String key) {
        String val = getAsString(json, key);
        return (Utils.hasValue(val) ? LocalDateTime.parse(val, Utils.sqlFormatter): null);
    }

    public static String getAsString(JsonElement e)
    {
        return e.isJsonObject() ? e.toString(): e.isJsonNull() ? null :  e.isJsonArray() ? e.getAsJsonArray().toString() : e.getAsJsonPrimitive().getAsString();
    }

    public static JsonElement parseJson(String str) {
        try {
            return (str == null ? JsonNull.INSTANCE : Conf.getJsonParser().parse(str));
        } catch (JsonSyntaxException e) {
            return new JsonPrimitive(str);
        }
    }

    public static boolean hasValue(String str) {
        return str != null && str.length() > 0;
    }

    public static String substringFromStart(String str, int length) {
        return str.substring(0, Math.min(length, str.length()));
    }

    public static String bytesToHex(byte[] data) {
        if (data == null) {
            return null;
        } else {
            StringBuilder string = new StringBuilder();
            for (byte b : data) {
                String hexString = Integer.toHexString(0x00FF & b);
                string.append(hexString.length() == 1 ? "0" + hexString : hexString);
            }
            return string.toString();
        }
    }

    static String getFileMimeType(String fileName) {

        String mimeType;
        if (fileName.endsWith(".rtf")) mimeType = "application/rtf";
        else if (fileName.endsWith(".xml")) mimeType = "text/xml";
        else if (fileName.endsWith(".txt")) mimeType = "text/plain";
        else if (fileName.endsWith(".pdf")) mimeType = "application/pdf";
        else if (fileName.endsWith(".zip")) mimeType = "application/zip";
        else if (fileName.endsWith(".jpg")) mimeType = "image/jpeg";
        else if (fileName.endsWith(".xlsx")) mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        else if (fileName.endsWith(".doc")) mimeType = "application/msword";
        else if (fileName.endsWith(".xls")) mimeType = "application/vnd.ms-excel";
        else if (fileName.endsWith(".csv")) mimeType = "text/csv";
        else if (fileName.endsWith(".log")) mimeType = "text/plain";
        else mimeType = "application/force-download";

        return mimeType;

    }

    public static Response requestUrl(String urlString) {

        BufferedInputStream in = null;
        StringBuilder out = new StringBuilder();
        try
        {
            HttpURLConnection connection = (HttpURLConnection)(new URL(urlString)).openConnection();
            in = new BufferedInputStream(connection.getInputStream());
            byte data[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(data, 0, 1024)) != -1) out.append(new String(data, 0, bytesRead, StandardCharsets.UTF_8));

            connection.disconnect();
            in.close();

            return Response.newInstance(out.toString(), HttpStatus.valueOf(connection.getResponseCode()));

        } catch (Exception e) {

            return Response.newInstance(e.getMessage(), HttpStatus.BAD_REQUEST);

        }
        finally
        {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                Log.error("Failed to close input stream: " + e.getMessage());
            }
        }


    }

    static String readFile(String fileName) throws IOException {

        return readStream(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8));
    }

    public static String readStream(java.io.Reader inStream) throws IOException {

        BufferedReader in = new BufferedReader(inStream);
        StringBuilder out = new StringBuilder();
        char data[] = new char[1024];
        int bytesRead;
        while ((bytesRead = in.read(data, 0, 1024)) != -1) out.append(new String(data, 0, bytesRead));
        in.close();

        return out.toString();
    }

    public static void replace(StringBuilder builder, String from, String to) {

        try {
            int index = builder.indexOf(from);
            if (index != -1) builder.replace(index, index + from.length(), URLEncoder.encode(to, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            Log.error("Failed to encode string: " + to);
        }
    }

    public static <T> T nvl(T newData, T defData) {
        return newData != null && (!(newData instanceof String) || Utils.hasValue((String)newData)) ? newData : defData;
    }

    public static String base64Decode(String base64EncodedMessage) {
        if (base64EncodedMessage != null) {
            return new String(Base64.getDecoder().decode(base64EncodedMessage), StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

    public static String base64Encode(String str) {
        if (str != null) {
            return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
        } else {
            return null;
        }
    }

}
