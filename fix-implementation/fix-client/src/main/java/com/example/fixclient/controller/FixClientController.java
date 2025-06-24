package com.example.fixclient.controller;

import com.example.fixclient.service.CertificateGenerationService;
import com.example.fixclient.service.FixClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fix/client")
public class FixClientController {

    @Autowired
    private FixClientService fixClientService;

    @Autowired
    private CertificateGenerationService certificateGenerationService;

    /**
     * 启动FIX客户端
     */
    @PostMapping("/start")
    public ResponseEntity<?> startClient() {
        try {
            fixClientService.startClient();
            return ResponseEntity.ok(Map.of(
                "message", "FIX client started successfully",
                "status", "connected"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to start FIX client",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 停止FIX客户端
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopClient() {
        try {
            fixClientService.stopClient();
            return ResponseEntity.ok(Map.of(
                "message", "FIX client stopped successfully",
                "status", "disconnected"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to stop FIX client",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 发送登录请求
     */
    @PostMapping("/logon")
    public ResponseEntity<?> sendLogon() {
        try {
            fixClientService.sendLogon();
            return ResponseEntity.ok(Map.of(
                "message", "Logon request sent successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to send logon request",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 发送登出请求
     */
    @PostMapping("/logout")
    public ResponseEntity<?> sendLogout() {
        try {
            fixClientService.sendLogout();
            return ResponseEntity.ok(Map.of(
                "message", "Logout request sent successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to send logout request",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 发送订单
     */
    @PostMapping("/orders")
    public ResponseEntity<?> sendOrder(@RequestBody Map<String, Object> orderData) {
        try {
            String symbol = (String) orderData.get("symbol");
            char side = ((String) orderData.get("side")).charAt(0);
            double quantity = Double.parseDouble(orderData.get("quantity").toString());
            char ordType = ((String) orderData.get("ordType")).charAt(0);
            double price = Double.parseDouble(orderData.get("price").toString());
            
            fixClientService.sendOrder(symbol, side, quantity, ordType, price);
            
            return ResponseEntity.ok(Map.of(
                "message", "Order sent successfully",
                "orderData", orderData
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to send order",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 发送测试请求
     */
    @PostMapping("/test")
    public ResponseEntity<?> sendTestRequest() {
        try {
            fixClientService.sendTestRequest();
            return ResponseEntity.ok(Map.of(
                "message", "Test request sent successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to send test request",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 检查连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus() {
        boolean isConnected = fixClientService.isConnected();
        
        return ResponseEntity.ok(Map.of(
            "connected", isConnected,
            "status", isConnected ? "CONNECTED" : "DISCONNECTED"
        ));
    }

    /**
     * 生成客户端证书
     */
    @PostMapping("/certificates/generate")
    public ResponseEntity<?> generateCertificate(@RequestBody Map<String, String> request) {
        try {
            String clientId = request.get("clientId");
            String outputPath = request.getOrDefault("outputPath", "client_keystore.jks");
            
            certificateGenerationService.generateClientCertificate(clientId, outputPath);
            
            return ResponseEntity.ok(Map.of(
                "message", "Certificate generated successfully",
                "clientId", clientId,
                "outputPath", outputPath
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Failed to generate certificate",
                "error", e.getMessage()
            ));
        }
    }
}
