package com.mikusher.logger.tracker;

import com.google.gson.JsonElement;
import com.mikusher.logger.JsonLogger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NoopLogger implements JsonLogger {

    @Override
    public JsonLogger map(String key, Map map) {
        return this;
    }

    @Override
    public JsonLogger map(String key, Supplier<Map> map) {
        return this;
    }

    @Override
    public JsonLogger list(String key, List list) {
        return this;
    }

    @Override
    public JsonLogger list(String key, Supplier<List> list) {
        return this;
    }

    @Override
    public JsonLogger message(String message) {
        return this;
    }

    @Override
    public JsonLogger message(Supplier<String> message) {
        return this;
    }

    @Override
    public JsonLogger field(String key, Object value) {
        return this;
    }

    @Override
    public JsonLogger field(String key, Supplier value) {
        return this;
    }

    @Override
    public JsonLogger json(String key, JsonElement jsonElement) {
        return this;
    }

    @Override
    public JsonLogger json(String key, Supplier<JsonElement> jsonElement) {
        return this;
    }

    @Override
    public JsonLogger exception(String key, Exception exception) {
        return this;
    }

    @Override
    public JsonLogger stack() {
        return this;
    }

    @Override
    public void log() {

    }
}
