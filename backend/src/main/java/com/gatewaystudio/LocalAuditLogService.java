package com.gatewaystudio;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class LocalAuditLogService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Path LOG_PATH = Paths.get("logs", "audit.log");

    public List<Map<String, Object>> getRecentAuditLogs(int limit) {
        File logFile = LOG_PATH.toFile();
        if (!logFile.exists()) {
            return List.of();
        }

        List<Map<String, Object>> logs = new ArrayList<>();

        try (Stream<String> lines = Files.lines(LOG_PATH)) {
            List<String> allLines = lines.toList();
            int start = Math.max(0, allLines.size() - limit);

            // Read lines in reverse order (newest logs first)
            for (int i = allLines.size() - 1; i >= start; i--) {
                String line = allLines.get(i);
                if (!line.isBlank()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> logEntry = objectMapper.readValue(line, Map.class);
                    logs.add(logEntry);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading local audit logs", e);
        }

        return logs;
    }
}
