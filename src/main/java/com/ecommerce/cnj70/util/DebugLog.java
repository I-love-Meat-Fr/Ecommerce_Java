package com.ecommerce.cnj70.util;

import java.io.FileWriter;
import java.io.IOException;

public final class DebugLog {

    public static final String LOG_PATH = "debug-04f262.log";

    private DebugLog() {
    }

    public static synchronized void write(String hypothesisId, String location, String message, String data) {
        try (FileWriter fw = new FileWriter(LOG_PATH, true)) {
            long ts = System.currentTimeMillis();
            String safeData = data == null ? "" : data.replace("\\", "\\\\").replace("\"", "\\\"");
            String safeLoc = location == null ? "" : location.replace("\"", "\\\"");
            String safeMsg = message == null ? "" : message.replace("\"", "\\\"");
            fw.write("{\"sessionId\":\"04f262\",\"location\":\"" + safeLoc
                    + "\",\"message\":\"" + safeMsg
                    + "\",\"data\":\"" + safeData
                    + "\",\"timestamp\":" + ts
                    + ",\"hypothesisId\":\"" + hypothesisId + "\"}\n");
        } catch (IOException ignored) {
        }
    }
}
