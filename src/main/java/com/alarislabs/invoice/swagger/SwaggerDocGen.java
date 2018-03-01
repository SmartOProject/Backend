package com.alarislabs.invoice.swagger;

import com.alarislabs.invoice.common.Conf;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SwaggerDocGen {

    private List<SwaggerResource> resources = new ArrayList<>();
    private List<String> paths = new ArrayList<>();
    private HashMap<String,Set<String>> methods = new HashMap<>();
    private static final Logger Log = LoggerFactory.getLogger(SwaggerDocGen.class);

    static final String BASIC_AUTH = "basic";
    static final String JWT_AUTH = "jwt";
    private static final int maxRows = 10000;

    private String version;

    public void setVersion(String version_) {
        version = version_;
    }

    public SwaggerDocGen() throws Exception {

        for(HashMap<String, String> record : Conf.getOracleConn().getQueryResultAsSetOfRows("begin ? := invoice.prv_rest_api.get_api_resource_list; end;", maxRows)) {

            String[] requestElements = record.get("request_pattern").split(":");
            String method = requestElements[0];
            String path = requestElements[1];

            paths.add(path);

            if (!methods.containsKey(path)) {
                Set<String> tmp = new HashSet<>();
                tmp.add(method);
                methods.put(path, tmp);
            } else {
                methods.get(path).add(method);
            }

            resources.add(new SwaggerResource(method,
                    path,
                    path.split("/")[0],
                    record.get("description"),
                    record.get("auth_required").equals("1"),
                    !record.get("return_cursor").equals("0") && record.get("request_pattern").split("/").length <= 1
            ));

        }

    }

    private SwaggerResource getResourceByPathAndMethod(String path, String method) {

        for(SwaggerResource element : resources) {
            if (element.getPath().equals(path) && element.getMethod().equals(method))
                return element;
        }

        Log.error("Resource " + method + ":" + path + " not found");
        return null;
    }

    public JsonObject getSwaggerJson() {


        JsonObject j = new JsonObject();

        j.addProperty("swagger", "2.0");
        j.addProperty("basePath", "/rest");
        j.add("info", getInfo());
        j.add("schemes", getSchemes());
        j.add("securityDefinitions", getAuthScheme());
        j.add("consumes", getConsumes());
        j.add("produces", getProduces());
        j.add("paths", getPaths());
        j.add("definitions", getAllDefinitions());

        return j;

    }

    private JsonObject getInfo() {
        JsonObject j = new JsonObject();
        j.addProperty("description", "");
        j.addProperty("version", version);
        j.addProperty("title", Conf.getSystemName() + " REST API");
        return j;
    }

    private JsonArray getSchemes() {
        JsonArray a = new JsonArray();
        a.add("https");
        return a;
    }

    private JsonObject getAuthScheme() {

        JsonObject j = new JsonObject();
        JsonObject basicScheme = new JsonObject();

        basicScheme.addProperty("type", "basic");
        basicScheme.addProperty("description", "HTTP Basic Authentication.");

        j.add(SwaggerDocGen.BASIC_AUTH, basicScheme);

        JsonObject jwtScheme = new JsonObject();

        jwtScheme.addProperty("type", "apiKey");
        jwtScheme.addProperty("name", "Authorization");
        jwtScheme.addProperty("in", "header");
        jwtScheme.addProperty("description", "Authorization via JWT.");

        j.add(SwaggerDocGen.JWT_AUTH, jwtScheme);

        return j;
    }

    private JsonArray getConsumes() {
        JsonArray a = new JsonArray();
        a.add("application/json");
        return a;
    }

    private JsonArray getProduces() {
        JsonArray a = new JsonArray();
        a.add("application/json");
        return a;
    }

    private JsonObject getPaths() {
        JsonObject j = new JsonObject();

        for(String pathElement : paths) {
            j.add("/" + pathElement, getMethods(pathElement));
        }


        return j;
    }

    private JsonObject getMethods(String path) {
        JsonObject j = new JsonObject();

        for(String methodElement : methods.get(path)) {

            SwaggerResource resource = getResourceByPathAndMethod(path, methodElement);

            if (resource != null)
                j.add(methodElement.toLowerCase(), resource.getResource());

        }

        return j;
    }

    private JsonObject getAllDefinitions() {

        JsonObject j = new JsonObject();
        for (SwaggerResource element : resources) {


            JsonElement inParams = element.getRequestDefinition();

            if (!inParams.isJsonNull()) {
                j.add(element.getSchemaId("in"), inParams);
            }

            JsonElement outParams = element.getResponseDefinition();

            if (!outParams.isJsonNull()) {
                j.add(element.getSchemaId("out"), outParams);
            }


        }
        return j;

    }

}
