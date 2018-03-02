package com.smarto.rest.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

public class Response {

    public static final String ERROR_MESSAGE_KEY = "error_message";
    static final String TOTAL_COUNT_HEADER = "X-total-count";
    static final String FETCHED_COUNT_HEADER = "X-fetched-count";
    static final String HAS_MORE_ROWS_HEADER = "X-has-more-rows";
    static final String QUERY_TOTAL_COUNT_HEADER = "X-query-total-count";
    private HttpStatus status;
    private String body;
    private MultiValueMap<String, String> headers;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Response response = (Response) o;

        if (status != response.status) return false;
        return body != null ? body.equals(response.body) : response.body == null;
    }

    @Override
    public int hashCode() {
        int result = status != null ? status.hashCode() : 0;
        result = 31 * result + (body != null ? body.hashCode() : 0);
        return result;
    }

    public void addHeader(String headerName, String headerValue) {
        if (headers == null) headers = new LinkedMultiValueMap<>();
        headers.add(headerName, headerValue);
    }

    public static Response newInstance(String body, HttpStatus status) {
        Response resp = new Response();
        resp.body = body;
        resp.status = status;
        return resp;
    }

    public static Response newErrInstance(String errorMessage, HttpStatus status) {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty(ERROR_MESSAGE_KEY, errorMessage);
        return newInstance(responseJson, status);
    }

    public static Response newInstance(JsonElement body, HttpStatus status) {
        return newInstance(body.toString(), status);
    }


    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getBody() {
        return body;
    }

    ResponseEntity<byte[]> toEntity() {

        byte[] resp = body.getBytes(StandardCharsets.UTF_8);

        if (headers != null) {
            return new ResponseEntity<>(resp, headers, status);
        } else {
            return new ResponseEntity<>(resp, status);
        }
    }

    public static String getRemoteAddr(HttpServletRequest request) {
        return (request.getHeader("x-real-ip") != null ? request.getHeader("x-real-ip") : request.getRemoteAddr());
    }


    public void mergeResponse(Response resp) {

        if (this.status == HttpStatus.OK && resp.getStatus() != HttpStatus.OK)
            this.status = resp.getStatus();

        if (this.body == null) this.body = resp.getBody();
        else if (resp.getBody() != null && !this.equals(resp)) {

            //Combine two responses
            JsonElement sourceJson = Utils.parseJson(this.body);
            JsonElement targetJson = Utils.parseJson(resp.getBody());

            if (sourceJson.isJsonArray() && (targetJson.isJsonObject() || targetJson.isJsonPrimitive())) {
                sourceJson.getAsJsonArray().add(targetJson);
                this.body = Utils.getAsString(sourceJson);
            } else if (targetJson.isJsonArray() && (sourceJson.isJsonObject() || sourceJson.isJsonPrimitive())) {
                targetJson.getAsJsonArray().add(sourceJson);
                this.body = Utils.getAsString(targetJson);
            } else if ((sourceJson.isJsonObject() && targetJson.isJsonObject())
                            || (sourceJson.isJsonObject() && targetJson.isJsonPrimitive())
                            || (sourceJson.isJsonPrimitive() && targetJson.isJsonObject())) {
                JsonArray finalJsonArray = new JsonArray();
                finalJsonArray.add(sourceJson);
                finalJsonArray.add(targetJson);
                this.body = Utils.getAsString(finalJsonArray);
            } else if (sourceJson.isJsonArray() && targetJson.isJsonArray()) {
                sourceJson.getAsJsonArray().addAll(targetJson.getAsJsonArray());
                this.body = Utils.getAsString(sourceJson);
            } else if (sourceJson.isJsonPrimitive() && targetJson.isJsonPrimitive()) {
                this.body = sourceJson.getAsJsonPrimitive().getAsString() + targetJson.getAsJsonPrimitive().getAsString();
            }
        }


    }

}
