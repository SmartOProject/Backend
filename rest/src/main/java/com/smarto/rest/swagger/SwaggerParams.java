package com.smarto.rest.swagger;

import com.google.gson.JsonObject;

class SwaggerParams {

    private String name;
    private String in;
    private boolean required;
    private String description;
    private String type;
    private boolean inArray;

    SwaggerParams(String name, String in, boolean required, String description, String type, boolean inArray) {
        this.name = name;
        this.in = in;
        this.required = required;
        this.description = description;
        this.type = type;
        this.inArray = inArray;
    }

    JsonObject getParameter(JsonObject schema) {

        JsonObject j = new JsonObject();
        j.addProperty("in", in);
        j.addProperty("name", getExternalName());


        switch (in) {
            case "body":
                if (inArray) j.addProperty("type", "array");
                j.add("schema", schema);
                break;
            default:
                if (required) j.addProperty("required", true);
                j.addProperty("description", description);
                j.addProperty("type", type);
                break;
        }

        return j;
    }



    public String getName() {
        return name;
    }

    boolean getRequired() {
        return required;
    }

    boolean inBody() {
        return in.equals("body");
    }

    String getExternalName() {
        if (inBody()) return "body";
        else return name;
    }

    JsonObject getSchemeElement() {
        JsonObject j = new JsonObject();
        j.addProperty("type", type);
        j.addProperty("description", description);
        return j;
    }

}
