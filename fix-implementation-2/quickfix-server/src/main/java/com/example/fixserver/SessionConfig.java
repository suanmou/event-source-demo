package com.example.fixserver;

import quickfix.SessionID;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SessionConfig {
    private SessionID sessionID;
    private final Set<String> allowedIPs = new HashSet<>();
    private final Set<String> allowedCertificates = new HashSet<>();
    private final Map<String, LocalDateTime> ipLastUsed = new HashMap<>();
    private final Map<String, LocalDateTime> certificateLastUsed = new HashMap<>();
    
    public SessionID getSessionID() {
        return sessionID;
    }
    
    public void setSessionID(SessionID sessionID) {
        this.sessionID = sessionID;
    }
    
    public Set<String> getAllowedIPs() {
        return allowedIPs;
    }
    
    public void addAllowedIP(String ip) {
        allowedIPs.add(ip);
        ipLastUsed.put(ip, LocalDateTime.now());
    }
    
    public void removeAllowedIP(String ip) {
        allowedIPs.remove(ip);
        ipLastUsed.remove(ip);
    }
    
    public Set<String> getAllowedCertificates() {
        return allowedCertificates;
    }
    
    public void addAllowedCertificate(String certificate) {
        allowedCertificates.add(certificate);
        certificateLastUsed.put(certificate, LocalDateTime.now());
    }
    
    public void removeAllowedCertificate(String certificate) {
        allowedCertificates.remove(certificate);
        certificateLastUsed.remove(certificate);
    }
    
    public void updateIPLastUsed(String ip) {
        ipLastUsed.put(ip, LocalDateTime.now());
    }
    
    public void updateCertificateLastUsed(String certificate) {
        certificateLastUsed.put(certificate, LocalDateTime.now());
    }
    
    public Map<String, LocalDateTime> getIpLastUsed() {
        return ipLastUsed;
    }
    
    public Map<String, LocalDateTime> getCertificateLastUsed() {
        return certificateLastUsed;
    }
    
    public boolean isIPExpired(String ip, int months) {
        LocalDateTime lastUsed = ipLastUsed.get(ip);
        if (lastUsed == null) {
            return true;
        }
        return lastUsed.plusMonths(months).isBefore(LocalDateTime.now());
    }
    
    public boolean isCertificateExpired(String certificate, int months) {
        LocalDateTime lastUsed = certificateLastUsed.get(certificate);
        if (lastUsed == null) {
            return true;
        }
        return lastUsed.plusMonths(months).isBefore(LocalDateTime.now());
    }
}
    