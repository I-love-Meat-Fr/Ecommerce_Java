package com.ecommerce.cnj70.controller.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Temporary debug controller that writes NDJSON logs from the browser to a local file.
 * This is used by the debug instrumentation in product-detail.html to capture runtime
 * evidence from the browser (which cannot write directly to the filesystem).
 */
@RestController
@RequestMapping("/debug")
public class DebugLogController {

    private static final Path LOG_FILE = Paths.get(".cursor", "debug-04f262.log");

    @PostMapping("/log")
    public ResponseEntity<String> receiveLog(@RequestBody Map<String, Object> payload) {
        try {
            // Ensure parent directory exists
            Path parent = LOG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            // Build NDJSON entry
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : payload.entrySet()) {
                if (!first) sb.append(',');
                sb.append('"').append(escape(e.getKey())).append('"').append(':');
                Object v = e.getValue();
                if (v == null) {
                    sb.append("null");
                } else if (v instanceof Number || v instanceof Boolean) {
                    sb.append(v);
                } else {
                    sb.append('"').append(escape(v.toString())).append('"');
                }
                first = false;
            }
            sb.append("}\n");
            // Append to file (create if missing)
            Files.writeString(LOG_FILE, sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return ResponseEntity.ok("ok");
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("err: " + ex.getMessage());
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
