<template>
  <div class="logs-container">
    <div class="card-title">
      <i class="fa fa-list-alt mr-2"></i>审计日志
    </div>
    
    <div class="bg-white p-4 rounded-lg shadow-sm">
      <div class="flex flex-wrap justify-between items-center mb-4">
        <el-input
          v-model="logSearch"
          placeholder="搜索日志内容或用户"
          clearable
          style="width: 300px; margin-bottom: 10px;">
          <i slot="prefix" class="fa fa-search"></i>
        </el-input>
        <div class="flex flex-wrap gap-2">
          <el-select v-model="logTypeFilter" placeholder="日志类型">
            <el-option label="全部" value=""></el-option>
            <el-option label="系统" value="system"></el-option>
            <el-option label="用户" value="user"></el-option>
            <el-option label="安全" value="security"></el-option>
            <el-option label="连接" value="connection"></el-option>
          </el-select>
          <el-date-picker
            v-model="logDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd">
          </el-date-picker>
          <el-button size="small" type="success" @click="refreshLogData">
            <i class="fa fa-refresh mr-1"></i>刷新
          </el-button>
        </div>
      </div>
      
      <el-table
        :data="filteredLogs"
        border
        stripe>
        <el-table-column prop="id" label="日志ID" width="120"></el-table-column>
        <el-table-column prop="timestamp" label="时间" width="180"></el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template slot-scope="scope">
            <el-tag :type="getLogTypeTagType(scope.row.type)">
              {{ getLogTypeText(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="user" label="用户" width="140"></el-table-column>
        <el-table-column prop="action" label="操作"></el-table-column>
        <el-table-column prop="details" label="详情">
          <template slot-scope="scope">
            <el-tooltip content="查看详情" placement="top">
              <div class="text-ellipsis max-w-xs cursor-pointer" @click="viewLogDetails(scope.row)">
                {{ scope.row.details.length > 100 ? scope.row.details.substring(0, 100) + '...' : scope.row.details }}
              </div>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      
      <Pagination 
        :total="filteredLogs.length" 
        :page-size="pageSize" 
        :current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 日志详情对话框 -->
    <el-dialog :visible.sync="logDetailsDialogVisible" title="日志详情">
      <LogDetails :log="selectedLog" />
    </el-dialog>
  </div>
</template>

<script>
import { mockLogs } from '../mockData';
import Pagination from './Pagination.vue';
import LogDetails from './LogDetails.vue';

export default {
  name: 'AuditLogs',
  components: {
    Pagination,
    LogDetails
  },
  data() {
    return {
      logs: [...mockLogs],
      logSearch: '',
      logTypeFilter: '',
      logDateRange: [],
      selectedLog: null,
      logDetailsDialogVisible: false,
      pageSize: 20,
      currentPage: 1
    };
  },
  computed: {
    filteredLogs() {
      return this.logs.filter(log => {
        // 搜索过滤
        const search = this.logSearch.toLowerCase();
        const matchesSearch = log.action.toLowerCase().includes(search) || 
                              log.details.toLowerCase().includes(search) ||
                              (log.user && log.user.toLowerCase().includes(search));
        
        // 类型过滤
        const matchesType = !this.logTypeFilter || log.type === this.logTypeFilter;
        
        // 日期范围过滤
        let matchesDateRange = true;
        if (this.logDateRange && this.logDateRange.length === 2) {
          const logDate = new Date(log.timestamp.split(' ')[0]);
          const startDate = new Date(this.logDateRange[0]);
          const endDate = new Date(this.logDateRange[1]);
          matchesDateRange = logDate >= startDate && logDate <= endDate;
        }
        
        return matchesSearch && matchesType && matchesDateRange;
      });
    }
  },
  methods: {
    getLogTypeTagType(type) {
      switch (type) {
        case 'system': return 'info';
        case 'user': return 'primary';
        case 'security': return 'danger';
        case 'connection': return 'warning';
        default: return 'info';
      }
    },
    getLogTypeText(type) {
      switch (type) {
        case 'system': return '系统';
        case 'user': return '用户';
        case 'security': return '安全';
        case 'connection': return '连接';
        default: return type;
      }
    },
    viewLogDetails(log) {
      this.selectedLog = log;
      this.logDetailsDialogVisible = true;
    },
    refreshLogData() {
      // 模拟刷新数据
      this.$notify({
        title: '刷新中',
        message: '正在刷新日志数据，请稍候...',
        type: 'info'
      });
      
      // 模拟网络请求延迟
      setTimeout(() => {
        // 随机添加一条新日志
        if (Math.random() > 0.7) {
          const newLog = {
            id: `LOG-${String(this.logs.length + 1).padStart(3, '0')}`,
            timestamp: new Date().toISOString().replace('T', ' ').substring(0, 19),
            type: 'system',
            user: '系统',
            action: '自动清理日志',
            objectType: 'log',
            objectId: null,
            ipAddress: '127.0.0.1',
            details: '系统自动清理了30天前的日志记录，共删除1200条记录。\n\n操作详情:\n- 开始时间: ' + 
                     new Date().toISOString().replace('T', ' ').substring(0, 19) + 
                     '\n- 结束时间: ' + new Date().toISOString().replace('T', ' ').substring(0, 19) + 
                     '\n- 删除记录数: 1200\n- 释放存储空间: 45MB'
          };
          
          this.logs.unshift(newLog);
        }
        
        this.$notify({
          title: '成功',
          message: '日志数据已刷新',
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
