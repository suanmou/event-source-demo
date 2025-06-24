package com.example.fixserver.controller;

import com.example.fixserver.service.CertificateService;
import com.example.fixserver.service.IpWhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fix/admin")
public class FixAdminController {

    @Autowired
    private IpWhitelistService ipWhitelistService;

    @Autowired
    private CertificateService certificateService;

    /**
     * 添加IP到白名单
     */
    @PostMapping("/whitelist/ips")
    public ResponseEntity<?> addIpToWhitelist(@RequestBody Map<String, String> request) {
        String ipAddress = request.get("ipAddress");
        String description = request.get("description");

        boolean success = ipWhitelistService.addIpToWhitelist(ipAddress, description);

        if (success) {
            return ResponseEntity.ok(Map.of(
                "message", "IP added to whitelist successfully",
                "ipAddress", ipAddress,
                "description", description
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Failed to add IP to whitelist. Maximum limit reached."
            ));
        }
    }

    /**
     * 获取所有白名单IP
     */
    @GetMapping("/whitelist/ips")
    public ResponseEntity<?> getAllWhitelistedIps() {
        List<IpWhitelistService.IpInfo> ipList = ipWhitelistService.getAllWhitelistedIps();
        
        return ResponseEntity.ok(ipList);
    }

    /**
     * 从白名单中移除IP
     */
    @DeleteMapping("/whitelist/ips/{ipAddress}")
    public ResponseEntity<?> removeIpFromWhitelist(@PathVariable String ipAddress) {
        boolean success = ipWhitelistService.removeIpFromWhitelist(ipAddress);
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                "message", "IP removed from whitelist successfully",
                "ipAddress", ipAddress
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 为会话生成证书
     */
    @PostMapping("/sessions/{sessionId}/certificates")
    public ResponseEntity<?> generateCertificateForSession(@PathVariable String sessionId) {
        try {
            // 创建一个模拟的SessionID
            SessionID sessionID = new SessionID("FIX.4.4", "SenderCompID", "TargetCompID");
            
            // 生成证书
            Map<String, String> certInfo = certificateService.generateCertificateForSession(sessionID);
            
            return ResponseEntity.ok(Map.of(
                "message", "Certificate generated successfully",
                "sessionId", sessionId,
                "certificateInfo", certInfo
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to generate certificate",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 断开FIX会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> disconnectSession(@PathVariable String sessionId) {
        try {
            // 解析sessionId为SessionID对象
            String[] parts = sessionId.split(":");
            if (parts.length < 3) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid session ID format"
                ));
            }
            
            SessionID sessionID = new SessionID(parts[0], parts[1], parts[2]);
            
            // 断开会话
            boolean success = Session.lookupSession(sessionID).logout("Admin requested logout");
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "message", "Session disconnected successfully",
                    "sessionId", sessionId
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Failed to disconnect session",
                    "sessionId", sessionId
                ));
            }
        } catch (SessionNotFound e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取会话状态
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSessionStatus(@PathVariable String sessionId) {
        try {
            // 解析sessionId为SessionID对象
            String[] parts = sessionId.split(":");
            if (parts.length < 3) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid session ID format"
                ));
            }
            
            SessionID sessionID = new SessionID(parts[0], parts[1], parts[2]);
            Session session = Session.lookupSession(sessionID);
            
            // 获取会话信息
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", sessionId);
            sessionInfo.put("isLoggedOn", session.isLoggedOn());
            sessionInfo.put("lastSentTime", formatDateTime(session.getLastSentTime()));
            sessionInfo.put("lastReceivedTime", formatDateTime(session.getLastReceivedTime()));
            sessionInfo.put("nextSenderMsgSeqNum", session.getExpectedSenderNum());
            sessionInfo.put("nextTargetMsgSeqNum", session.getExpectedTargetNum());
            
            // 获取关联的IP
            String ipAddress = ipWhitelistService.getSessionIp(sessionID);
            sessionInfo.put("ipAddress", ipAddress);
            
            return ResponseEntity.ok(sessionInfo);
        } catch (SessionNotFound e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(long timestamp) {
        if (timestamp <= 0) {
            return "N/A";
        }
        
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
