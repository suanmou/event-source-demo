<template>
  <div id="app">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header>
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <i class="fa fa-exchange mr-2" style="font-size: 24px;"></i>
            <h1 class="text-xl font-bold">彭博 FIX Connectivity 控制台</h1>
          </div>
          <div class="flex items-center">
            <el-dropdown>
              <span class="el-dropdown-link cursor-pointer flex items-center">
                <i class="fa fa-user-circle mr-2"></i>
                <span>{{ currentUser.name }}</span>
                <i class="fa fa-caret-down ml-1"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item>
                  <i class="fa fa-user mr-2"></i>个人信息
                </el-dropdown-item>
                <el-dropdown-item>
                  <i class="fa fa-cog mr-2"></i>系统设置
                </el-dropdown-item>
                <el-dropdown-item>
                  <i class="fa fa-sign-out mr-2"></i>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <!-- 侧边导航和主内容区域 -->
      <el-container>
        <SidebarNav :active-menu="activeMenu" @menu-select="handleMenuSelect" />
        <MainContent :active-menu="activeMenu" />
      </el-container>
    </el-container>
    
    <!-- 全局通知组件 -->
    <GlobalNotifications />
  </div>
</template>

<script>
import SidebarNav from './components/SidebarNav.vue';
import MainContent from './components/MainContent.vue';
import GlobalNotifications from './components/GlobalNotifications.vue';

export default {
  name: 'App',
  components: {
    SidebarNav,
    MainContent,
    GlobalNotifications
  },
  data() {
    return {
      activeMenu: 'dashboard',
      currentUser: {
        name: '系统管理员',
        role: '管理员'
      }
    };
  },
  methods: {
    handleMenuSelect(menuIndex) {
      this.activeMenu = menuIndex;
    }
  }
};
</script>

<style>
/* 全局样式 */
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f7fa;
}
.el-container {
  height: 100vh;
}
.el-header {
  background-color: #2d3a4b;
  color: white;
  line-height: 60px;
  padding: 0 20px;
}
</style>
