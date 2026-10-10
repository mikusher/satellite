package com.mikusher.parameter;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Formats SatelliteData for diagnostic output without modifying its values. */
final class SatelliteDataPrinter {
    private SatelliteDataPrinter() {
    }

    static void print(PrintWriter writer, SatelliteData data) {
        prettyPrint(writer, data, -4);
    }

    private static void prettyPrint(PrintWriter writer, Map<String, Object> value, int indent) {

        int nextIndent = indent + 4;

        writer.println();

        for (Map.Entry<String, Object> entry : value.entrySet()) {
            prettyPrint(writer, entry.getKey(), entry.getValue(), nextIndent);
        }
    }

    private static void prettyPrint(PrintWriter writer, List<?> value, int indent) {

        int nextIndent = indent + 4;

        writer.println();

        int index = 0;
        for (Iterator<?> i = value.iterator(); i.hasNext(); ) {
            String key = "(" + index++ + ")";
            Object parVal = i.next();
            prettyPrint(writer, key, parVal, nextIndent);
        }
    }

    private static void prettyPrint(PrintWriter writer, String key, Object value, int indent) {

        printIndent(writer, indent);
        if (key != null) {
            writer.print(key);
            writer.print(" ");
        }

        if (value instanceof CharSequence) {
            writer.print("(String)");
            writer.println(" \"" + value + "\"");
            return;
        }
        if (value instanceof Map) {
            writer.print("(Map)");
            prettyPrint(writer, (Map) value, indent);
            return;
        }
        if (value instanceof List) {
            writer.print("(Array)");
            prettyPrint(writer, (List) value, indent);
            return;
        }
        if (value instanceof BigDecimal) {
            writer.print("(Decimal)");
            writer.println(" \"" + value + "\"");
            return;
        }
        if (value == null) {
            writer.println("(NULL)");
            return;
        }

        writer.print("(" + getTypeName(value) + ")");
        writer.println(" " + value);
    }

    private static String getTypeName(Object obj) {

        String className = obj.getClass().getName();
        int lastDotIndex = className.lastIndexOf('.');
        String typeName = (lastDotIndex >= 0) ? className.substring(lastDotIndex + 1) : className;

        return typeName;
    }

    private static void printIndent(PrintWriter writer, int count) {

        for (int i = 0; i < count; i++) {
            writer.print(' ');
        }
    }

}
