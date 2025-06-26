<template>
  <div class="sessions-container">
    <div class="card-title">
      <i class="fa fa-clock-o mr-2"></i>FIX 会话状态监控
    </div>
    
    <!-- 状态指标卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <MetricCard 
        :value="metrics.activeSessions" 
        label="活跃会话" 
        type="success" 
      />
      <MetricCard 
        :value="metrics.failedSessions" 
        label="失败会话" 
        type="danger" 
      />
      <MetricCard 
        :value="metrics.totalMessagesPerSecond" 
        label="消息/秒" 
        type="info" 
      />
      <MetricCard 
        :value="metrics.averageLatency + 'ms'" 
        label="平均延迟" 
        type="warning" 
      />
    </div>
    
    <div class="bg-white p-4 rounded-lg shadow-sm">
      <div class="flex justify-between items-center mb-4">
        <el-input
          v-model="sessionSearch"
          placeholder="搜索会话ID或连接名称"
          clearable
          style="width: 300px;">
          <i slot="prefix" class="fa fa-search"></i>
        </el-input>
        <el-button size="small" type="success" @click="refreshSessionData">
          <i class="fa fa-refresh mr-1"></i>刷新数据
        </el-button>
      </div>
      
      <el-table
        :data="filteredSessions"
        border
        stripe>
        <el-table-column prop="id" label="会话ID" width="140"></el-table-column>
        <el-table-column prop="connectionName" label="连接名称"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <StatusIndicator :status="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="messagesReceived" label="接收消息" width="120"></el-table-column>
        <el-table-column prop="messagesSent" label="发送消息" width="120"></el-table-column>
        <el-table-column prop="lastMessageTime" label="最后消息时间" width="180"></el-table-column>
        <el-table-column prop="sessionStartTime" label="会话开始时间" width="180"></el-table-column>
        <el-table-column prop="latency" label="延迟" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.latency }}ms</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="viewSessionDetails(scope.row)">
              <i class="fa fa-eye mr-1"></i>详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <Pagination 
        :total="filteredSessions.length" 
        :page-size="pageSize" 
        :current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 会话详情对话框 -->
    <el-dialog :visible.sync="sessionDetailsDialogVisible" title="会话详情">
      <SessionDetails :session="selectedSession" />
    </el-dialog>
  </div>
</template>

<script>
import { mockSessions } from '../mockData';
import MetricCard from './MetricCard.vue';
import StatusIndicator from './StatusIndicator.vue';
import Pagination from './Pagination.vue';
import SessionDetails from './SessionDetails.vue';

export default {
  name: 'SessionMonitoring',
  components: {
    MetricCard,
    StatusIndicator,
    Pagination,
    SessionDetails
  },
  data() {
    return {
      sessions: [...mockSessions],
      sessionSearch: '',
      metrics: {
        activeSessions: 0,
        failedSessions: 0,
        totalMessagesPerSecond: 0,
        averageLatency: 0
      },
      selectedSession: null,
      sessionDetailsDialogVisible: false,
      pageSize: 10,
      currentPage: 1
    };
  },
  created() {
    this.updateMetrics();
  },
  computed: {
    filteredSessions() {
      return this.sessions.filter(session => {
        const search = this.sessionSearch.toLowerCase();
        return session.id.toLowerCase().includes(search) || 
               session.connectionName.toLowerCase().includes(search);
      });
    }
  },
  methods: {
    updateMetrics() {
      // 计算活跃会话数
      this.metrics.activeSessions = this.sessions.filter(session => session.status === 'active').length;
      
      // 计算失败会话数
      this.metrics.failedSessions = this.sessions.filter(session => session.status === 'inactive').length;
      
      // 计算消息/秒
      this.metrics.totalMessagesPerSecond = this.sessions.reduce((sum, session) => sum + session.messagesPerSecond, 0);
      
      // 计算平均延迟
      const activeSessions = this.sessions.filter(session => session.status === 'active');
      this.metrics.averageLatency = activeSessions.length > 0 
        ? Math.round(activeSessions.reduce((sum, session) => sum + session.latency, 0) / activeSessions.length)
        : 0;
    },
    viewSessionDetails(session) {
      this.selectedSession = session;
      this.sessionDetailsDialogVisible = true;
    },
    refreshSessionData() {
      // 模拟刷新数据
      this.$notify({
        title: '刷新中',
        message: '正在刷新会话数据，请稍候...',
        type: 'info'
      });
      
      // 模拟网络请求延迟
      setTimeout(() => {
        // 更新会话数据
        this.sessions = this.sessions.map(session => {
          // 随机更新一些数据
          const updatedSession = { ...session };
          
          if (session.status === 'active') {
            // 增加一些消息计数
            updatedSession.messagesReceived += Math.floor(Math.random() * 10);
            updatedSession.messagesSent += Math.floor(Math.random() * 5);
            
            // 随机波动延迟
            updatedSession.latency = Math.max(10, Math.min(100, session.latency + (Math.random() * 10 - 5)));
            
            // 随机添加新消息
            if (Math.random() > 0.7) {
              const newMessage = {
                direction: Math.random() > 0.5 ? 'in' : 'out',
                timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19),
                content: '8=FIX.4.4|9=142|35=W|49=NYMD|56=CLIENT|34=' + 
                          (updatedSession.messagesReceived + updatedSession.messagesSent) + 
                          '|52=' + new Date().toISOString().replace('T', '-').substring(0, 19) + 
                          '|268=3|269=0|270=150.25|271=100|269=1|270=150.30|271=200|269=2|270=150.20|271=150|10=123'
              };
              
              updatedSession.messages = [newMessage, ...updatedSession.messages.slice(0, 9)];
            }
          }
          
          return updatedSession;
        });
        
        // 更新指标
        this.updateMetrics();
        
        this.$notify({
          title: '成功',
          message: '会话数据已刷新',
          type: 'success'
        });
      }, 1000);
    },
    handleSizeChange(size) {
      this.pageSize = size;
    },
    handleCurrentChange(page) {
      this.currentPage = page;
    }
  }
};
</script>

<style scoped>
.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #333;
}
</style>
