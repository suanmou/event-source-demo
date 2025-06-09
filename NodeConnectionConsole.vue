<template>
  <div class="node-connection-console">
    <el-container>
      <el-header>
        <h1>节点连接控制台</h1>
      </el-header>
      <el-container>
        <el-aside width="300px">
          <el-card class="box-card">
            <div slot="header" class="clearfix">
              <span>节点列表</span>
              <el-button style="float: right; padding: 3px 0" type="text">刷新</el-button>
            </div>
            <el-scrollbar style="height: 400px">
              <el-list :data="nodes" border>
                <template #item="{ item }">
                  <el-card shadow="hover">
                    <div class="item">
                      <el-tag :type="item.status === 'online' ? 'success' : 'danger'">
                        {{ item.status === 'online' ? '在线' : '离线' }}
                      </el-tag>
                      <span class="node-name" @click="showNodeDetail(item)">{{ item.name }}</span>
                      <el-button
                        v-if="item.status === 'online'"
                        type="primary"
                        size="mini"
                        @click="connectNode(item, $event)"
                      >
                        连接
                      </el-button>
                    </div>
                  </el-card>
                </template>
              </el-list>
            </el-scrollbar>
          </el-card>
        </el-aside>
        <el-main>
          <el-card v-if="selectedNode" class="node-detail-card">
            <div slot="header" class="clearfix">
              <span>节点详情: {{ selectedNode.name }}</span>
            </div>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form :model="selectedNode" label-width="100px">
                  <el-form-item label="节点名称">
                    <el-input v-model="selectedNode.name" disabled></el-input>
                  </el-form-item>
                  <el-form-item label="节点地址">
                    <el-input v-model="selectedNode.address" disabled></el-input>
                  </el-form-item>
                  <el-form-item label="节点状态">
                    <el-tag :type="selectedNode.status === 'online' ? 'success' : 'danger'">
                      {{ selectedNode.status === 'online' ? '在线' : '离线' }}
                    </el-tag>
                  </el-form-item>
                </el-form>
              </el-col>
              <el-col :span="12">
                <el-form :model="selectedNode" label-width="100px">
                  <el-form-item label="节点类型">
                    <el-input v-model="selectedNode.type" disabled></el-input>
                  </el-form-item>
                  <el-form-item label="连接方式">
                    <el-input v-model="selectedNode.connectionType" disabled></el-input>
                  </el-form-item>
                  <el-form-item label="描述信息">
                    <el-input type="textarea" v-model="selectedNode.description" disabled></el-input>
                  </el-form-item>
                </el-form>
              </el-col>
            </el-row>
          </el-card>
          
          <el-card v-if="isConnected" class="console-card">
            <div slot="header" class="clearfix">
              <span>控制台: {{ selectedNode.name }}</span>
              <el-button
                style="float: right; padding: 3px 0"
                type="danger"
                size="small"
                @click="disconnectNode"
              >
                断开连接
              </el-button>
            </div>
            <div class="console-container">
              <el-scrollbar class="console-messages">
                <div class="message" v-for="(message, index) in consoleMessages" :key="index">
                  <span class="timestamp">{{ formatTimestamp(message.timestamp) }}</span>
                  <span class="content" :class="message.type">{{ message.content }}</span>
                </div>
              </el-scrollbar>
              <el-form class="console-input" @submit.native.prevent="sendMessage">
                <el-input
                  v-model="inputMessage"
                  placeholder="输入命令..."
                  @keyup.enter.native="sendMessage"
                  ref="inputRef"
                ></el-input>
                <el-button type="primary" @click="sendMessage">发送</el-button>
              </el-form>
            </div>
          </el-card>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'NodeConnectionConsole',
  data() {
    return {
      nodes: [
        {
          id: 1,
          name: '开发测试节点',
          address: 'ws://localhost:8080',
          status: 'online',
          type: '开发环境',
          connectionType: 'WebSocket',
          description: '用于开发测试的节点服务器'
        },
        {
          id: 2,
          name: '生产环境节点A',
          address: 'ws://prod-node-a.example.com:8080',
          status: 'online',
          type: '生产环境',
          connectionType: 'WebSocket',
          description: '生产环境主节点服务器'
        },
        {
          id: 3,
          name: '生产环境节点B',
          address: 'ws://prod-node-b.example.com:8080',
          status: 'offline',
          type: '生产环境',
          connectionType: 'WebSocket',
          description: '生产环境备用节点服务器'
        },
        {
          id: 4,
          name: '测试环境节点',
          address: 'ws://test-node.example.com:8080',
          status: 'online',
          type: '测试环境',
          connectionType: 'WebSocket',
          description: '用于集成测试的节点服务器'
        }
      ],
      selectedNode: null,
      isConnected: false,
      socket: null,
      consoleMessages: [],
      inputMessage: '',
      connectionAttempts: 0,
      maxConnectionAttempts: 3
    }
  },
  methods: {
    showNodeDetail(node) {
      this.selectedNode = { ...node };
      if (this.isConnected) {
        this.disconnectNode();
      }
    },
    connectNode(node, event) {
      event.stopPropagation();
      this.selectedNode = { ...node };
      this.connectWebSocket();
    },
    connectWebSocket() {
      if (!this.selectedNode) return;
      
      this.consoleMessages = [];
      this.addConsoleMessage('正在连接到节点...', 'system');
      
      try {
        this.socket = new WebSocket(this.selectedNode.address);
        this.connectionAttempts = 0;
        this.isConnected = true;
        
        this.socket.onopen = () => {
          this.connectionAttempts = 0;
          this.addConsoleMessage('已成功连接到节点', 'success');
          this.addConsoleMessage('欢迎使用节点控制台，请输入命令进行操作', 'info');
        };
        
        this.socket.onmessage = (event) => {
          this.addConsoleMessage(event.data, 'response');
        };
        
        this.socket.onclose = (event) => {
          this.isConnected = false;
          let message = `连接已关闭 (代码: ${event.code}`;
          if (event.reason) message += `, 原因: ${event.reason}`;
          message += ')';
          
          this.addConsoleMessage(message, 'error');
          
          // 自动重连逻辑
          if (this.connectionAttempts < this.maxConnectionAttempts) {
            this.connectionAttempts++;
            this.addConsoleMessage(`尝试重新连接 (${this.connectionAttempts}/${this.maxConnectionAttempts})...`, 'system');
            setTimeout(() => this.connectWebSocket(), 3000);
          }
        };
        
        this.socket.onerror = (error) => {
          this.addConsoleMessage(`连接错误: ${error.message}`, 'error');
        };
      } catch (error) {
        this.isConnected = false;
        this.addConsoleMessage(`连接异常: ${error.message}`, 'error');
      }
    },
    disconnectNode() {
      if (this.socket) {
        this.addConsoleMessage('正在断开连接...', 'system');
        this.socket.close(1000, '用户主动断开连接');
        this.socket = null;
      }
      this.isConnected = false;
    },
    sendMessage() {
      if (!this.isConnected || !this.socket || this.inputMessage.trim() === '') return;
      
      const message = this.inputMessage.trim();
      this.addConsoleMessage(message, 'request');
      
      try {
        this.socket.send(message);
      } catch (error) {
        this.addConsoleMessage(`发送消息失败: ${error.message}`, 'error');
      }
      
      this.inputMessage = '';
      this.$nextTick(() => {
        this.$refs.inputRef.focus();
      });
    },
    addConsoleMessage(content, type = 'info') {
      this.consoleMessages.push({
        timestamp: new Date(),
        content,
        type
      });
      
      // 限制消息数量，防止内存溢出
      if (this.consoleMessages.length > 1000) {
        this.consoleMessages.shift();
      }
    },
    formatTimestamp(timestamp) {
      const date = new Date(timestamp);
      return date.toLocaleTimeString();
    }
  },
  beforeDestroy() {
    this.disconnectNode();
  }
}
</script>

<style scoped>
.node-connection-console {
  height: 100vh;
}

.el-header {
  background-color: #333;
  color: white;
  line-height: 60px;
}

.el-header h1 {
  margin: 0;
  font-size: 24px;
}

.el-aside {
  background-color: #f9fafc;
  padding: 10px;
}

.node-name {
  margin: 0 10px;
  cursor: pointer;
  font-weight: bold;
}

.node-detail-card, .console-card {
  height: 100%;
}

.console-container {
  display: flex;
  flex-direction: column;
  height: 450px;
}

.console-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background-color: #000;
  color: #fff;
  font-family: Consolas, Monaco, monospace;
  font-size: 14px;
}

.console-input {
  display: flex;
  align-items: center;
}

.console-input .el-input {
  flex: 1;
  margin-right: 10px;
}

.message {
  margin-bottom: 5px;
}

.timestamp {
  color: #999;
  margin-right: 10px;
}

.info {
  color: #fff;
}

.success {
  color: #52c41a;
}

.error {
  color: #ff4d4f;
}

.request {
  color: #1890ff;
}

.response {
  color: #faad14;
}

.system {
  color: #8c8c8c;
}
</style>  