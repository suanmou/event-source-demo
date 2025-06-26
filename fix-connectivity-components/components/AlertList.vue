<template>
  <div class="alert-list-container">
    <div class="alert-list-header">
      <h3 class="alert-list-title">系统警报</h3>
      <div class="alert-list-actions">
        <el-button size="small" @click="handleRefresh">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
        <el-button size="small" type="primary" @click="handleMarkAllAsRead">
          <i class="el-icon-check"></i> 全部标记为已读
        </el-button>
      </div>
    </div>
    
    <div class="alert-list-filter">
      <el-row :gutter="10">
        <el-col :span="6">
          <el-select v-model="filter.level" placeholder="警报级别">
            <el-option label="全部" value=""></el-option>
            <el-option label="信息" value="info"></el-option>
            <el-option label="警告" value="warning"></el-option>
            <el-option label="危险" value="danger"></el-option>
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filter.status" placeholder="状态">
            <el-option label="全部" value=""></el-option>
            <el-option label="待处理" value="pending"></el-option>
            <el-option label="已解决" value="resolved"></el-option>
          </el-select>
        </el-col>
        <el-col :span="12">
          <el-input v-model="filter.keyword" placeholder="搜索关键词" suffix-icon="el-icon-search"></el-input>
        </el-col>
      </el-row>
    </div>
    
    <div class="alert-list-content">
      <el-table
        :data="filteredAlerts"
        stripe
        border
        @row-click="handleRowClick"
        @sort-change="handleSortChange"
      >
        <el-table-column
          label="状态"
          width="100"
          align="center"
        >
          <template #default="scope">
            <StatusIndicator :status="scope.row.level" size="small" />
          </template>
        </el-table-column>
        <el-table-column
          label="标题"
          prop="title"
          sortable
          min-width="200"
        >
          <template #default="scope">
            <div class="alert-title" :class="{ 'alert-unread': scope.row.status === 'pending' }">
              {{ scope.row.title }}
            </div>
          </template>
        </el-table-column>
        <el-table-column
          label="来源"
          prop="source"
          width="120"
          sortable
        ></el-table-column>
        <el-table-column
          label="时间"
          prop="timestamp"
          width="180"
          sortable
        ></el-table-column>
        <el-table-column
          label="操作"
          width="120"
          align="center"
        >
          <template #default="scope">
            <el-button
              size="mini"
              type="primary"
              @click="handleViewAlert(scope.row, $event)"
            >
              查看
            </el-button>
            <el-button
              size="mini"
              type="text"
              v-if="scope.row.status === 'pending'"
              @click="handleResolveAlert(scope.row, $event)"
            >
              解决
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div v-if="filteredAlerts.length === 0" class="alert-list-empty">
        <img src="https://picsum.photos/id/237/200/150" alt="No alerts" class="empty-image">
        <p class="empty-text">暂无警报</p>
      </div>
    </div>
    
    <div class="alert-list-footer">
      <Pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredAlerts.length"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <el-dialog
      title="警报详情"
      :visible.sync="dialogVisible"
      width="700px"
    >
      <template #content>
        <div class="alert-detail">
          <div class="alert-detail-header">
            <div class="alert-detail-title">{{ currentAlert.title }}</div>
            <div class="alert-detail-status">
              <StatusIndicator :status="currentAlert.level" />
              <span class="status-text">
                {{ currentAlert.status === 'pending' ? '待处理' : '已解决' }}
              </span>
            </div>
          </div>
          
          <div class="alert-detail-info">
            <div class="info-item">
              <span class="info-label">来源:</span>
              <span class="info-value">{{ currentAlert.source }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">时间:</span>
              <span class="info-value">{{ currentAlert.timestamp }}</span>
            </div>
            <div class="info-item" v-if="currentAlert.resolvedAt">
              <span class="info-label">解决时间:</span>
              <span class="info-value">{{ currentAlert.resolvedAt }}</span>
            </div>
            <div class="info-item" v-if="currentAlert.resolvedBy">
              <span class="info-label">解决人:</span>
              <span class="info-value">{{ currentAlert.resolvedBy }}</span>
            </div>
          </div>
          
          <div class="alert-detail-content">
            <div class="content-title">警报详情</div>
            <div class="content-text">{{ currentAlert.message }}</div>
          </div>
          
          <div class="alert-detail-actions" v-if="currentAlert.status === 'pending'">
            <el-button type="primary" @click="handleResolveCurrentAlert">
              <i class="el-icon-check"></i> 标记为已解决
            </el-button>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import StatusIndicator from './StatusIndicator.vue';
import Pagination from './Pagination.vue';

export default {
  name: 'AlertList',
  components: {
    StatusIndicator,
    Pagination
  },
  props: {
    alerts: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      filter: {
        level: '',
        status: '',
        keyword: ''
      },
      currentPage: 1,
      pageSize: 10,
      dialogVisible: false,
      currentAlert: null,
      sort: {
        prop: 'timestamp',
        order: 'descending'
      }
    };
  },
  computed: {
    filteredAlerts() {
      let result = [...this.alerts];
      
      // 应用筛选条件
      if (this.filter.level) {
        result = result.filter(alert => alert.level === this.filter.level);
      }
      
      if (this.filter.status) {
        result = result.filter(alert => alert.status === this.filter.status);
      }
      
      if (this.filter.keyword) {
        const keyword = this.filter.keyword.toLowerCase();
        result = result.filter(alert => 
          alert.title.toLowerCase().includes(keyword) || 
          alert.message.toLowerCase().includes(keyword) ||
          alert.source.toLowerCase().includes(keyword)
        );
      }
      
      // 应用排序
      if (this.sort.prop) {
        result.sort((a, b) => {
          const valueA = a[this.sort.prop];
          const valueB = b[this.sort.prop];
          
          if (typeof valueA === 'string' && typeof valueB === 'string') {
            return this.sort.order === 'ascending' 
              ? valueA.localeCompare(valueB) 
              : valueB.localeCompare(valueA);
          } else {
            return this.sort.order === 'ascending' 
              ? valueA - valueB 
              : valueB - valueA;
          }
        });
      }
      
      return result;
    },
    paginatedAlerts() {
      const startIndex = (this.currentPage - 1) * this.pageSize;
      return this.filteredAlerts.slice(startIndex, startIndex + this.pageSize);
    }
  },
  methods: {
    handleRefresh() {
      this.$emit('refresh');
    },
    handleMarkAllAsRead() {
      this.$confirm('确认将所有警报标记为已读？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('mark-all-as-read');
        this.$message({
          type: 'success',
          message: '所有警报已标记为已读'
        });
      }).catch(() => {});
    },
    handleRowClick(row) {
      this.currentAlert = row;
      this.dialogVisible = true;
    },
    handleViewAlert(row, event) {
      event.stopPropagation();
      this.currentAlert = row;
      this.dialogVisible = true;
    },
    handleResolveAlert(row, event) {
      event.stopPropagation();
      this.$confirm('确认解决此警报？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('resolve-alert', row.id);
        this.$message({
          type: 'success',
          message: '警报已解决'
        });
      }).catch(() => {});
    },
    handleResolveCurrentAlert() {
      this.$confirm('确认解决此警报？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        if (this.currentAlert) {
          this.$emit('resolve-alert', this.currentAlert.id);
          this.$message({
            type: 'success',
            message: '警报已解决'
          });
          this.dialogVisible = false;
        }
      }).catch(() => {});
    },
    handleSortChange({ column, prop, order }) {
      this.sort = {
        prop,
        order
      };
    },
    handleSizeChange(newSize) {
      this.pageSize = newSize;
    },
    handleCurrentChange(newPage) {
      this.currentPage = newPage;
    }
  }
};
</script>

<style scoped>
.alert-list-container {
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 16px;
}

.alert-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.alert-list-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.alert-list-actions {
  display: flex;
  gap: 8px;
}

.alert-list-filter {
  margin-bottom: 16px;
}

.alert-list-content {
  margin-bottom: 16px;
}

.alert-unread {
  font-weight: bold;
}

.alert-list-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.empty-image {
  width: 120px;
  height: 90px;
  object-fit: cover;
  opacity: 0.6;
  margin-bottom: 16px;
}

.empty-text {
  color: #909399;
  font-size: 14px;
}

.alert-detail {
  padding: 16px;
}

.alert-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 1px solid #EBEEF5;
  padding-bottom: 16px;
}

.alert-detail-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.alert-detail-status {
  display: flex;
  align-items: center;
}

.status-text {
  margin-left: 8px;
  font-size: 14px;
}

.alert-detail-info {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 16px;
  border-bottom: 1px solid #EBEEF5;
  padding-bottom: 16px;
}

.info-item {
  width: 50%;
  margin-bottom: 8px;
}

.info-label {
  color: #606266;
  font-weight: 500;
  margin-right: 8px;
}

.info-value {
  color: #303133;
}

.alert-detail-content {
  margin-bottom: 16px;
  border-bottom: 1px solid #EBEEF5;
  padding-bottom: 16px;
}

.content-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 8px;
}

.content-text {
  color: #606266;
  line-height: 1.5;
}

.alert-detail-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
    