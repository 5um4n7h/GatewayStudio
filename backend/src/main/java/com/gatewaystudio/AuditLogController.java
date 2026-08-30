package com.gatewaystudio;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AuditLogController {

    private final LocalAuditLogService auditLogService;

    public AuditLogController(LocalAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/v1/admin/audit-logs")
    public List<Map<String, Object>> getAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        return auditLogService.getRecentAuditLogs(limit);
    }
}
