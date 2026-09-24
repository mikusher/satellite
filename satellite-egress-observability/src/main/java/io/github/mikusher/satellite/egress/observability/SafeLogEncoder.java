package io.github.mikusher.satellite.egress.observability;

import java.util.Map;
import java.util.TreeMap;

final class SafeLogEncoder {
    private SafeLogEncoder() {
    }

    static String encode(String message, Map<String, Object> values) {
        StringBuilder builder = new StringBuilder();
        builder.append(escape(String.valueOf(message)));

        if (!values.isEmpty()) {
            builder.append(" {");
            boolean first = true;
            for (Map.Entry<String, Object> entry : new TreeMap<String, Object>(values).entrySet()) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(escape(entry.getKey()))
                        .append('=')
                        .append(escape(String.valueOf(entry.getValue())));
                first = false;
            }
            builder.append('}');
        }

        return builder.toString();
    }

    static String escape(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (Character.isISOControl(c)) {
                        builder.append('?');
                    } else {
                        builder.append(c);
                    }
            }
        }
        return builder.toString();
    }
}
