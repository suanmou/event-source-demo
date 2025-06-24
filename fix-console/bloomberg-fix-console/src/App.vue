<template>
  <div id="app">
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header>
        <div class="header-container">
          <div class="logo">
            <img src="@/assets/logo.png" alt="Bloomberg FIX Console">
            <span class="title">Bloomberg FIX Console</span>
          </div>
          <div class="user-info">
            <el-dropdown trigger="click">
              <span class="el-dropdown-link">
                <i class="el-icon-user"></i> 管理员 <i class="el-icon-caret-bottom"></i>
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <!-- 侧边栏 -->
      <el-aside width="200px">
        <el-menu default-active="1" class="el-menu-vertical-demo" router>
          <el-menu-item index="1" route="/dashboard">
            <i class="el-icon-dashboard"></i>
            <span>仪表盘</span>
          </el-menu-item>
          <el-submenu index="2">
            <template slot="title">
              <i class="el-icon-shield"></i>
              <span>安全管理</span>
            </template>
            <el-menu-item index="2-1" route="/ip-whitelist">
              <i class="el-icon-s-check"></i>
              <span>IP 白名单</span>
            </el-menu-item>
            <el-menu-item index="2-2" route="/certificates">
              <i class="el-icon-lock"></i>
              <span>证书管理</span>
            </el-menu-item>
          </el-submenu>
          <el-submenu index="3">
            <template slot="title">
              <i class="el-icon-connection"></i>
              <span>会话管理</span>
            </template>
            <el-menu-item index="3-1" route="/sessions">
              <i class="el-icon-link"></i>
              <span>活动会话</span>
            </el-menu-item>
            <el-menu-item index="3-2" route="/session-history">
              <i class="el-icon-history"></i>
              <span>会话历史</span>
            </el-menu-item>
          </el-submenu>
          <el-menu-item index="4" route="/logs">
            <i class="el-icon-document"></i>
            <span>日志记录</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <!-- 主内容区 -->
      <el-main>
        <router-view></router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App',
  methods: {
    logout() {
      // 清除本地存储的token
      localStorage.removeItem('fix-console-token');
      // 跳转到登录页面
      this.$router.push('/login');
    }
  }
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
}

.el-header {
  background-color: #2c3e50;
  color: white;
  line-height: 60px;
  padding: 0 20px;
}

.el-aside {
  background-color: #35495e;
  color: white;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
}

.logo img {
  height: 40px;
  margin-right: 10px;
}

.logo .title {
  font-size: 18px;
  font-weight: bold;
}

.user-info {
  cursor: pointer;
}

.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 200px;
}

.el-menu-item, .el-submenu__title {
  color: white;
}

.el-menu-item:hover, .el-submenu__title:hover {
  background-color: #4e6e8e;
}

.el-menu-item.is-active {
  background-color: #4e6e8e;
}
</style>
    