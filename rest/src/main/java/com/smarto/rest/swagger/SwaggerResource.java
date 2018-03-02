package com.smarto.rest.swagger;

import com.smarto.rest.common.Conf;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class SwaggerResource {

    private String method;
    private String path;
    private String tag;
    private String summary;
    private boolean returnArray;
    private boolean authRequired;
    private List<SwaggerParams> requestParams = new ArrayList<>();

    private List<SwaggerParams> requestBodyParams = new ArrayList<>();
    private List<SwaggerParams> responseBodyParams = new ArrayList<>();

    private static final Logger Log = LoggerFactory.getLogger(SwaggerResource.class);
    private static final int maxRows = 10000;

    private boolean paramExists(List<SwaggerParams> params, String paramName) {

        if (params != null)
            for (SwaggerParams element : params) {
                if (element.getExternalName().equals(paramName)) return true;
            }
        return false;

    }

    SwaggerResource(String method, String path, String tag, String summary, boolean authRequired, boolean returnArray) {

        this.method = method;
        this.path = path;
        this.tag = tag;
        this.summary = summary;
        this.authRequired = authRequired;
        this.returnArray = returnArray;

        List<HashMap<String, String>> records = null;

        try {

            records = Conf.getOracleConn().getQueryResultAsSetOfRows(
                    "begin ? := smarto.prv_rest_api.get_api_resource_param_list('" +  this.method + ":" + this.path + "'); end; ",
                    maxRows
            );

        } catch (Exception e) {
            Log.error("Cannot load parameters", e);
        }

        if (records != null) {
            //Init params
            for (HashMap<String, String> record : records) {

                SwaggerParams swaggerParams = new SwaggerParams(record.get("param_name"),
                        record.get("param_in"),
                        record.get("param_required").equals("1"),
                        record.get("param_descr"),
                        record.get("param_type"),
                        false
                );

                //Check object already in hash set

                switch (record.get("param_direction")) {
                    case "0":
                        if (!paramExists(requestParams, swaggerParams.getExternalName()))
                            requestParams.add(swaggerParams);

                        if (swaggerParams.inBody())
                            requestBodyParams.add(swaggerParams);

                        break;

                    case "1":
                        if (swaggerParams.inBody())
                            responseBodyParams.add(swaggerParams);

                        break;
                }

            }
        }
    }

    String getMethod() {
        return method;
    }

    String getPath() {
        return path;
    }

    String getSchemaId(String suffix) {
        return path.split("/")[0] + "_" + method.toLowerCase() + "_" + suffix;
    }

    private JsonObject getSchema(String suffix) {
        JsonObject j = new JsonObject();
        j.addProperty("$ref", "#/definitions/" + getSchemaId(suffix));
        return j;
    }

    private JsonArray getTags() {
        JsonArray array = new JsonArray();
        array.add(tag); //Add array of tags in future
        return array;
    }

    private JsonArray getParameters() {
        JsonArray array = new JsonArray();

        if (requestParams != null)
            for(SwaggerParams element : requestParams) {
                array.add(element.getParameter(getSchema("in")));
            }

        return array;
    }

    private JsonObject getResponses() {
        JsonObject j = new JsonObject();

        JsonObject r200 = new JsonObject(), r400 = new JsonObject(), r401 = new JsonObject();

        r200.addProperty("description", "Successful execution");

        if (returnArray)
            r200.addProperty("type", "array");

        if (responseBodyParams.size() > 0)
            r200.add("schema", getSchema("out"));

        r400.addProperty("description", "Invalid input");
        r401.addProperty("description", "Authorization failed");

        j.add("200", r200);
        j.add("400", r400);
        j.add("401", r401);

        return j;
    }

    JsonObject getResource() {
        JsonObject j = new JsonObject();

        j.addProperty("summary", summary);
        j.add("tags", getTags());
        j.add("parameters", getParameters());
        j.add("security", getSecurity());
        j.add("responses", getResponses());

        return j;
    }

    private JsonArray getSecurity() {

        JsonArray a = new JsonArray();

        if (authRequired) {
            JsonObject j = new JsonObject();

            if (path.equals("auth"))
                j.add(SwaggerDocGen.BASIC_AUTH, new JsonArray());
            else
                j.add(SwaggerDocGen.JWT_AUTH, new JsonArray());

            a.add(j);
        }
        return a;


    }

    private JsonElement getDefinition(List<SwaggerParams> params) {

        JsonObject properties = new JsonObject();
        JsonArray requiredProps = new JsonArray();
        boolean empty = true;

        if (params != null) {
            for (SwaggerParams element : params) {

                if (element.inBody()) {
                    empty = false;
                    properties.add(element.getName(), element.getSchemeElement());

                    if (element.getRequired()) requiredProps.add(element.getName());
                }

            }
        }

        if (empty) return JsonNull.INSTANCE;
        else {
            JsonObject j = new JsonObject();
            j.addProperty("type", "object");
            j.add("properties", properties);

            if (requiredProps.size() > 0)
                j.add("required", requiredProps);

            return j;
        }


    }

    JsonElement getRequestDefinition() {
        return getDefinition(requestBodyParams);
    }

    JsonElement getResponseDefinition() {
        return getDefinition(responseBodyParams);
    }

}
