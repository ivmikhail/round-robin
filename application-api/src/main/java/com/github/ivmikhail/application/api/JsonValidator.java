package com.github.ivmikhail.application.api;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class JsonValidator {

    public boolean isValid(String json) {
        try {
            JsonParser.parseString(json);
            return true;
        } catch (JsonParseException ignore) {
            return false;
        }
    }
}
