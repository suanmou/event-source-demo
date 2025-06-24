package com.example.fixserver.service;

import org.springframework.stereotype.Service;
import quickfix.SessionID;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IpWhitelistService {

    // 存储IP白名单
    private final Map<String, IpInfo> ipWhitelist = new ConcurrentHashMap<>();
    
    // 存储会话与IP的映射关系
    private final Map<String, String> sessionIpMap = new ConcurrentHashMap<>();
    
    // 存储IP最后登录时间
    private final Map<String, LocalDateTime> ipLastLoginTime = new ConcurrentHashMap<>();
    
    // IP白名单限制数量
    private static final int MAX_WHITELIST_SIZE = 30;
    
    // IP使用期限（188天）
    private static final long IP_USAGE_EXPIRATION_DAYS = 188;
    
    // 登录频率限制（1次/秒）
    private static final long LOGIN_FREQUENCY_LIMIT_MS = 1000;

    /**
     * 添加IP到白名单
     */
    public boolean addIpToWhitelist(String ipAddress, String description) {
        if (ipWhitelist.size() >= MAX_WHITELIST_SIZE) {
            return false; // 超出白名单限制
        }
        
        ipWhitelist.put(ipAddress, new IpInfo(
            ipAddress,
            description,
            LocalDateTime.now(),
            IpStatus.ACTIVE
        ));
        
        return true;
    }

    /**
     * 从白名单中移除IP
     */
    public boolean removeIpFromWhitelist(String ipAddress) {
        return ipWhitelist.remove(ipAddress) != null;
    }

    /**
     * 获取所有IP白名单
     */
    public List<IpInfo> getAllWhitelistedIps() {
        return new ArrayList<>(ipWhitelist.values());
    }

    /**
     * 验证IP是否在白名单中且有效
     */
    public boolean validateIp(String ipAddress) {
        if (!ipWhitelist.containsKey(ipAddress)) {
            return false;
        }
        
        IpInfo ipInfo = ipWhitelist.get(ipAddress);
        
        // 检查IP状态
        if (ipInfo.getStatus() != IpStatus.ACTIVE) {
            return false;
        }
        
        // 检查IP使用期限
        LocalDateTime lastLogin = ipLastLoginTime.getOrDefault(ipAddress, LocalDateTime.MIN);
        if (ChronoUnit.DAYS.between(lastLogin, LocalDateTime.now()) > IP_USAGE_EXPIRATION_DAYS) {
            // 标记为过期
            ipInfo.setStatus(IpStatus.EXPIRED);
            return false;
        }
        
        return true;
    }

    /**
     * 验证IP登录频率
     */
    public boolean validateLoginFrequency(String ipAddress) {
        LocalDateTime lastLogin = ipLastLoginTime.get(ipAddress);
        
        if (lastLogin == null) {
            return true; // 首次登录
        }
        
        long timeSinceLastLogin = ChronoUnit.MILLIS.between(lastLogin, LocalDateTime.now());
        return timeSinceLastLogin >= LOGIN_FREQUENCY_LIMIT_MS;
    }

    /**
     * 记录IP登录
     */
    public void recordIpLogin(String ipAddress, SessionID sessionID) {
        ipLastLoginTime.put(ipAddress, LocalDateTime.now());
        sessionIpMap.put(sessionID.toString(), ipAddress);
        
        // 更新IP状态为活跃
        if (ipWhitelist.containsKey(ipAddress)) {
            ipWhitelist.get(ipAddress).setStatus(IpStatus.ACTIVE);
        }
    }

    /**
     * 获取会话关联的IP
     */
    public String getSessionIp(SessionID sessionID) {
        return sessionIpMap.get(sessionID.toString());
    }

    /**
     * IP信息类
     */
    public static class IpInfo {
        private final String ipAddress;
        private final String description;
        private final LocalDateTime addedTime;
        private IpStatus status;

        public IpInfo(String ipAddress, String description, LocalDateTime addedTime, IpStatus status) {
            this.ipAddress = ipAddress;
            this.description = description;
            this.addedTime = addedTime;
            this.status = status;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String getDescription() {
            return description;
        }

        public LocalDateTime getAddedTime() {
            return addedTime;
        }

        public IpStatus getStatus() {
            return status;
        }

        public void setStatus(IpStatus status) {
            this.status = status;
        }
    }

    /**
     * IP状态枚举
     */
    public enum IpStatus {
        ACTIVE,
        INACTIVE,
        EXPIRED
    }
}
