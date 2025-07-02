<template>
  <div class="fix-tester">
    <el-card shadow="hover">
      <div slot="header">
        <span>FIX Client 连接测试</span>
      </div>
      
      <el-form :model="form" label-width="120px">
        <el-form-item label="选择客户端配置">
          <el-select v-model="selectedConfig" placeholder="请选择" @change="handleConfigChange">
            <el-option
              v-for="config in clientConfigs"
              :key="config.id"
              :label="config.name"
              :value="config">
            </el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="状态">
          <el-tag :type="connectionStatus === 'Connected' ? 'success' : 'danger'">
            {{ connectionStatus || '未连接' }}
          </el-tag>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="connectToFix"
            :disabled="!selectedConfig || connecting">
            {{ connecting ? '连接中...' : '连接到FIX Server' }}
          </el-button>
          <el-button @click="disconnect" :disabled="!connected">断开连接</el-button>
        </el-form-item>
      </el-form>
      
      <div class="log-container">
        <h3>通信日志</h3>
        <div class="log-content">{{ log }}</div>
      </div>
    </el-card>
  </div>
</template>

<script>
import io from 'socket.io-client';

export default {
  data() {
    return {
      form: {},
      selectedConfig: null,
      clientConfigs: [
        { id: 'client1', name: '交易客户端1', host: 'fix.example.com', port: 5001 },
        { id: 'client2', name: '风控客户端2', host: 'risk-fix.example.com', port: 5002 }
      ],
      connectionStatus: '',
      log: '',
      socket: null,
      connected: false,
      connecting: false
    };
  },
  methods: {
    handleConfigChange(config) {
      console.log('Selected config:', config);
    },
    
    connectToFix() {
      if (!this.selectedConfig) return;
      
      this.connecting = true;
      this.log += `[${new Date().toLocaleTimeString()}] 正在连接FIX服务器...\n`;
      
      // 建立WebSocket连接
      this.socket = io('http://localhost:8080'); // 后端地址
      
      this.socket.on('connect', () => {
        this.connectionStatus = 'Connected';
        this.connected = true;
        this.connecting = false;
        this.log += `[${new Date().toLocaleTimeString()}] WebSocket连接成功\n`;
        
        // 发送配置到后端
        this.socket.emit('fix-config', this.selectedConfig);
      });
      
      this.socket.on('fix-event', (data) => {
        this.log += `[${new Date().toLocaleTimeString()}] ${data}\n`;
      });
      
      this.socket.on('error', (err) => {
        this.log += `[${new Date().toLocaleTimeString()}] 错误: ${err.message}\n`;
        this.resetConnection();
      });
      
      this.socket.on('disconnect', () => {
        this.log += `[${new Date().toLocaleTimeString()}] 连接已断开\n`;
        this.resetConnection();
      });
    },
    
    disconnect() {
      if (this.socket) {
        this.socket.disconnect();
      }
      this.resetConnection();
    },
    
    resetConnection() {
      this.connecting = false;
      this.connected = false;
      this.connectionStatus = '';
      this.socket = null;
    }
  },
  beforeDestroy() {
    if (this.socket) {
      this.socket.disconnect();
    }
  }
};
</script>

<style scoped>
.log-container {
  margin-top: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 15px;
}
.log-content {
  height: 200px;
  overflow-y: auto;
  background: #f5f7fa;
  padding: 10px;
  font-family: monospace;
  white-space: pre-wrap;
}
</style>