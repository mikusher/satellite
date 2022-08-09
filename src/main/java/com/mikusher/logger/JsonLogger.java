package com.mikusher.logger;


import com.google.gson.JsonElement;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface JsonLogger {

    JsonLogger message(String message);
    JsonLogger message(Supplier<String> message);
    JsonLogger map(String key, Map map);
    JsonLogger map(String key, Supplier<Map> map);
    JsonLogger list(String key, List list);
    JsonLogger list(String key, Supplier<List> list);
    JsonLogger field(String key, Object value);
    JsonLogger field(String key, Supplier value);
    JsonLogger json(String key, JsonElement jsonElement);
    JsonLogger json(String key, Supplier<JsonElement> jsonElement);
    JsonLogger exception(String key, Exception exception);
    JsonLogger stack();
    void log();
}
