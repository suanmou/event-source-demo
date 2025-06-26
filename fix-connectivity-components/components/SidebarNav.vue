<template>
  <el-aside width="200px">
    <el-menu
      mode="vertical"
      :default-active="activeMenu"
      :collapse="isCollapsed"
      background-color="#324157"
      text-color="#bfcbd9"
      active-text-color="#409eff"
      @select="handleSelect">
      <el-menu-item index="dashboard">
        <i class="fa fa-dashboard"></i>
        <span slot="title">仪表盘</span>
      </el-menu-item>
      <el-submenu index="network">
        <template slot="title">
          <i class="fa fa-sitemap"></i>
          <span>连接配置与网络管理</span>
        </template>
        <el-menu-item index="network.connections">
          <i class="fa fa-link"></i>
          <span slot="title">FIX 连接</span>
        </el-menu-item>
        <el-menu-item index="network.ip">
          <i class="fa fa-globe"></i>
          <span slot="title">IP 允许列表</span>
        </el-menu-item>
        <el-menu-item index="network.dns">
          <i class="fa fa-server"></i>
          <span slot="title">DNS 配置</span>
        </el-menu-item>
      </el-submenu>
      <el-submenu index="security">
        <template slot="title">
          <i class="fa fa-lock"></i>
          <span>安全认证与证书管理</span>
        </template>
        <el-menu-item index="security.certificates">
          <i class="fa fa-certificate"></i>
          <span slot="title">TLS 证书</span>
        </el-menu-item>
        <el-menu-item index="security.auth">
          <i class="fa fa-key"></i>
          <span slot="title">认证策略</span>
        </el-menu-item>
      </el-submenu>
      <el-submenu index="monitoring">
        <template slot="title">
          <i class="fa fa-line-chart"></i>
          <span>会话监控与警报系统</span>
        </template>
        <el-menu-item index="monitoring.sessions">
          <i class="fa fa-clock-o"></i>
          <span slot="title">会话状态</span>
        </el-menu-item>
        <el-menu-item index="monitoring.alerts">
          <i class="fa fa-bell"></i>
          <span slot="title">警报管理</span>
        </el-menu-item>
        <el-menu-item index="monitoring.logs">
          <i class="fa fa-list-alt"></i>
          <span slot="title">审计日志</span>
        </el-menu-item>
      </el-submenu>
      <el-menu-item index="settings">
        <i class="fa fa-cog"></i>
        <span slot="title">系统设置</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<script>
export default {
  name: 'SidebarNav',
  props: {
    activeMenu: {
      type: String,
      default: 'dashboard'
    }
  },
  data() {
    return {
      isCollapsed: false
    };
  },
  methods: {
    handleSelect(index) {
      this.$emit('menu-select', index);
    }
  }
};
</script>

<style scoped>
.el-aside {
  background-color: #324157;
  color: white;
}
</style>
