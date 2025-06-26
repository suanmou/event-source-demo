<template>
  <el-main>
    <transition name="fade">
      <!-- 仪表盘 -->
      <Dashboard v-if="activeMenu === 'dashboard'" />
      
      <!-- 连接配置与网络管理 -->
      <ConnectionManagement v-if="activeMenu === 'network.connections'" />
      <IpManagement v-if="activeMenu === 'network.ip'" />
      <DnsManagement v-if="activeMenu === 'network.dns'" />
      
      <!-- 安全认证与证书管理 -->
      <CertificateManagement v-if="activeMenu === 'security.certificates'" />
      <AuthPolicyManagement v-if="activeMenu === 'security.auth'" />
      
      <!-- 会话监控与警报系统 -->
      <SessionMonitoring v-if="activeMenu === 'monitoring.sessions'" />
      <AlertManagement v-if="activeMenu === 'monitoring.alerts'" />
      <AuditLogs v-if="activeMenu === 'monitoring.logs'" />
      
      <!-- 系统设置 -->
      <SystemSettings v-if="activeMenu === 'settings'" />
    </transition>
  </el-main>
</template>

<script>
import Dashboard from './Dashboard.vue';
import ConnectionManagement from './ConnectionManagement.vue';
import IpManagement from './IpManagement.vue';
import DnsManagement from './DnsManagement.vue';
import CertificateManagement from './CertificateManagement.vue';
import AuthPolicyManagement from './AuthPolicyManagement.vue';
import SessionMonitoring from './SessionMonitoring.vue';
import AlertManagement from './AlertManagement.vue';
import AuditLogs from './AuditLogs.vue';
import SystemSettings from './SystemSettings.vue';

export default {
  name: 'MainContent',
  props: {
    activeMenu: {
      type: String,
      required: true
    }
  },
  components: {
    Dashboard,
    ConnectionManagement,
    IpManagement,
    DnsManagement,
    CertificateManagement,
    AuthPolicyManagement,
    SessionMonitoring,
    AlertManagement,
    AuditLogs,
    SystemSettings
  }
};
</script>

<style scoped>
.el-main {
  padding: 20px;
  overflow-y: auto;
}
</style>
