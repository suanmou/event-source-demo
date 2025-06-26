<template>
  <div class="alerts-container">
    <div class="card-title">
      <i class="fa fa-bell mr-2"></i>警报管理
    </div>
    
    <div class="bg-white p-4 rounded-lg shadow-sm">
      <div class="flex flex-wrap justify-between items-center mb-4">
        <el-input
          v-model="alertSearch"
          placeholder="搜索警报标题或内容"
          clearable
          style="width: 300px; margin-bottom: 10px;">
          <i slot="prefix" class="fa fa-search"></i>
        </el-input>
        <div class="flex flex-wrap gap-2">
          <el-select v-model="alertLevelFilter" placeholder="警报级别">
            <el-option label="全部" value=""></el-option>
            <el-option label="紧急" value="danger"></el-option>
            <el-option label="警告" value="warning"></el-option>
            <el-option label="信息" value="info"></el-option>
            <el-option label="成功" value="success"></el-option>
          </el-select>
          <el-select v-model="alertStatusFilter" placeholder="处理状态">
            <el-option label="全部" value=""></el-option>
            <el-option label="未处理" value="pending"></el-option>
            <el-option label="已处理" value="resolved"></el-option>
          </el-select>
          <el-button size="small" type="success" @click="refreshAlertData">
            <i class="fa fa-refresh mr-1"></i>刷新
          </el-button>
        </div>
      </div>
      
      <el-table
        :data="filteredAlerts"
        border
        stripe>
        <el-table-column prop="id" label="警报ID" width="120"></el-table-column>
        <el-table-column prop="title" label="标题"></el-table-column>
        <el-table-column prop="level" label="级别" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.level">
              {{ getAlertLevelText(scope.row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="140"></el-table-column>
        <el-table-column prop="timestamp" label="时间" width="180"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="viewAlertDetails(scope.row)">
              <i class="fa fa-eye mr-1"></i>详情
            </el-button>
            <el-button 
              size="mini" 
              :type="scope.row.status === 'pending' ? 'success' : 'default'"
              :disabled="scope.row.status === 'resolved'"
              @click="resolveAlert(scope.row)">
              <i class="fa fa-check mr-1"></i>{{ scope.row.status === 'pending' ? '处理' : '已处理' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <Pagination 
        :total="filteredAlerts.length" 
        :page-size="pageSize" 
        :current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 警报详情对话框 -->
    <el-dialog :visible.sync="alertDetailsDialogVisible" title="警报详情">
      <AlertDetails :alert="selectedAlert" @resolve="resolveAlert" />
    </el-dialog>
  </div>
</template>

<script>
import { mockAlerts } from '../mockData';
import Pagination from './Pagination.vue';
import AlertDetails from './AlertDetails.vue';

export default {
  name: 'AlertManagement',
  components: {
    Pagination,
    AlertDetails
  },
  data() {
    return {
      alerts: [...mockAlerts],
      alertSearch: '',
      alertLevelFilter: '',
      alertStatusFilter: '',
      selectedAlert: null,
      alertDetailsDialogVisible: false,
      pageSize: 10,
      currentPage: 1
    };
  },
  computed: {
    filteredAlerts() {
      return this.alerts.filter(alert => {
        // 搜索过滤
        const search = this.alertSearch.toLowerCase();
        const matchesSearch = alert.title.toLowerCase().includes(search) || 
                              alert.message.toLowerCase().includes(search);
        
        // 级别过滤
        const matchesLevel = !this.alertLevelFilter || alert.level === this.alertLevelFilter;
        
        // 状态过滤
        const matchesStatus = !this.alertStatusFilter || alert.status === this.alertStatusFilter;
        
        return matchesSearch && matchesLevel && matchesStatus;
      });
    }
  },
  methods: {
    getAlertLevelText(level) {
      switch (level) {
        case 'danger': return '紧急';
        case 'warning': return '警告';
        case 'info': return '信息';
        case 'success': return '成功';
        default: return level;
      }
    },
    getStatusTagType(status) {
      switch (status) {
        case 'pending': return 'warning';
        case 'resolved': return 'success';
        default: return 'info';
      }
    },
    getStatusText(status) {
      switch (status) {
        case 'pending': return '未处理';
        case 'resolved': return '已处理';
        default: return status;
      }
    },
    viewAlertDetails(alert) {
      this.selectedAlert = alert;
      this.alertDetailsDialogVisible = true;
    },
    resolveAlert(alert) {
      this.$confirm('确定要处理此警报吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟处理警报
        const updatedAlerts = this.alerts.map(a => {
          if (a.id === alert.id) {
            return {
              ...a,
              status: 'resolved',
              resolvedBy: '管理员',
              resolvedAt: new Date().toISOString().replace('T', ' ').substring(0, 19)
            };
          }
          return a;
        });
        
        this.alerts = updatedAlerts;
        this.alertDetailsDialogVisible = false;
        
        this.$notify({
          title: '成功',
          message: '警报已处理',
          type: 'success'
        });
      }).catch(() => {
        // 用户取消操作
      });
    },
    refreshAlertData() {
      // 模拟刷新数据
      this.$notify({
        title: '刷新中',
        message: '正在刷新警报数据，请稍候...',
        type: 'info'
      });
      
      // 模拟网络请求延迟
      setTimeout(() => {
        // 随机添加一条新警报
        if (Math.random() > 0.7) {
          const newAlert = {
            id: `ALERT-${String(this.alerts.length + 1).padStart(3, '0')}`,
            title: '新连接测试成功',
            level: 'success',
            source: '系统',
            timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19),
            status: 'pending',
            message: '刚刚添加的新连接测试成功，已建立稳定连接。',
            recommendedActions: [
              '确认连接参数是否符合预期',
              '开始数据传输测试',
              '记录连接状态变化'
            ]
          };
          
          this.alerts.unshift(newAlert);
        }
        
        this.$notify({
          title: '成功',
          message: '警报数据已刷新',
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
