<template>
  <div class="dashboard-container">
    <div class="card-title">
      <i class="fa fa-dashboard mr-2"></i>FIX Connectivity 概览
    </div>
    
    <!-- 状态指标卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <MetricCard 
        :value="metrics.activeConnections" 
        label="活跃连接" 
        type="success" 
      />
      <MetricCard 
        :value="metrics.totalMessages" 
        label="今日消息量" 
        type="info" 
      />
      <MetricCard 
        :value="metrics.pendingAlerts" 
        label="待处理警报" 
        type="warning" 
      />
      <MetricCard 
        :value="metrics.expiringCertificates" 
        label="即将过期证书" 
        type="danger" 
      />
    </div>
    
    <!-- 图表区域 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
      <div class="bg-white p-4 rounded-lg shadow-sm">
        <div class="flex justify-between items-center mb-4">
          <h3 class="font-semibold">连接状态分布</h3>
          <el-select v-model="connectionChartPeriod" placeholder="选择时间范围">
            <el-option label="今日" value="today"></el-option>
            <el-option label="本周" value="week"></el-option>
            <el-option label="本月" value="month"></el-option>
          </el-select>
        </div>
        <div class="chart-container" id="connectionChart"></div>
      </div>
      
      <div class="bg-white p-4 rounded-lg shadow-sm">
        <div class="flex justify-between items-center mb-4">
          <h3 class="font-semibold">消息流量趋势</h3>
          <el-select v-model="trafficChartPeriod" placeholder="选择时间范围">
            <el-option label="今日" value="today"></el-option>
            <el-option label="本周" value="week"></el-option>
            <el-option label="本月" value="month"></el-option>
          </el-select>
        </div>
        <div class="chart-container" id="trafficChart"></div>
      </div>
    </div>
    
    <!-- 最近警报 -->
    <div class="bg-white p-4 rounded-lg shadow-sm mt-6">
      <div class="flex justify-between items-center mb-4">
        <h3 class="font-semibold">最近警报</h3>
        <el-button size="small" type="primary" @click="viewAllAlerts">查看全部</el-button>
      </div>
      <AlertList :alerts="latestAlerts" />
    </div>
  </div>
</template>

<script>
import MetricCard from './MetricCard.vue';
import AlertList from './AlertList.vue';
import { mockConnections, mockCertificates, mockAlerts } from '../mockData';

export default {
  name: 'Dashboard',
  components: {
    MetricCard,
    AlertList
  },
  data() {
    return {
      metrics: {
        activeConnections: 0,
        totalMessages: 0,
        pendingAlerts: 0,
        expiringCertificates: 0,
        activeSessions: 0,
        failedSessions: 0,
        totalMessagesPerSecond: 0,
        averageLatency: 0
      },
      latestAlerts: [],
      connectionChartPeriod: 'today',
      trafficChartPeriod: 'today'
    };
  },
  created() {
    this.loadMetrics();
    this.loadLatestAlerts();
    this.$nextTick(() => {
      this.renderCharts();
    });
  },
  methods: {
    loadMetrics() {
      // 计算活跃连接数
      this.metrics.activeConnections = mockConnections.filter(conn => conn.status === 'active').length;
      
      // 计算今日消息量 (模拟值)
      this.metrics.totalMessages = 25483;
      
      // 计算待处理警报数
      this.metrics.pendingAlerts = mockAlerts.filter(alert => alert.status === 'pending').length;
      
      // 计算即将过期证书数 (30天内)
      this.metrics.expiringCertificates = mockCertificates.filter(cert => cert.daysLeft <= 30 && cert.daysLeft >= 0).length;
    },
    loadLatestAlerts() {
      // 获取最近的5个警报
      this.latestAlerts = [...mockAlerts].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp)).slice(0, 5);
    },
    renderCharts() {
      // 这里将使用ECharts或其他图表库渲染图表
      // 由于没有引入图表库，这里只是示例
      console.log('Rendering charts...');
    },
    viewAllAlerts() {
      this.$emit('menu-select', 'monitoring.alerts');
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
.chart-container {
  margin-top: 20px;
  height: 300px;
}
</style>
