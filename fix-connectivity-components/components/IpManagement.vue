<template>
  <div class="ip-management-container">
    <div class="ip-management-header">
      <h3 class="ip-management-title">IP 允许列表</h3>
      <div class="ip-management-actions">
        <el-button type="primary" @click="handleAddIp">
          <i class="el-icon-plus"></i> 添加 IP
        </el-button>
        <el-button @click="handleRefresh">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </div>
    </div>
    
    <div class="ip-management-filter">
      <el-row :gutter="10">
        <el-col :span="6">
          <el-select v-model="filter.type" placeholder="IP 类型">
            <el-option label="全部" value=""></el-option>
            <el-option label="私有" value="private"></el-option>
            <el-option label="互联网" value="internet"></el-option>
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filter.status" placeholder="状态">
            <el-option label="全部" value=""></el-option>
            <el-option label="活跃" value="active"></el-option>
            <el-option label="非活跃" value="inactive"></el-option>
            <el-option label="警告" value="warning"></el-option>
          </el-select>
        </el-col>
        <el-col :span="12">
          <el-input v-model="filter.keyword" placeholder="搜索 IP 或描述" suffix-icon="el-icon-search"></el-input>
        </el-col>
      </el-row>
    </div>
    
    <div class="ip-management-content">
      <el-table
        :data="filteredIps"
        stripe
        border
        @sort-change="handleSortChange"
      >
        <el-table-column
          label="状态"
          width="100"
          align="center"
        >
          <template #default="scope">
            <StatusIndicator :status="scope.row.status" size="small" />
          </template>
        </el-table-column>
        <el-table-column
          label="IP 地址"
          prop="ip"
          sortable
          min-width="150"
        ></el-table-column>
        <el-table-column
          label="类型"
          prop="type"
          width="100"
          sortable
        >
          <template #default="scope">
            {{ scope.row.type === 'private' ? '私有' : '互联网' }}
          </template>
        </el-table-column>
        <el-table-column
          label="最后使用"
          prop="lastUsed"
          width="180"
          sortable
        ></el-table-column>
        <el-table-column
          label="描述"
          prop="description"
          min-width="200"
        ></el-table-column>
        <el-table-column
          label="操作"
          width="180"
          align="center"
        >
          <template #default="scope">
            <el-button
              size="mini"
              type="primary"
              @click="handleEditIp(scope.row)"
            >
              <i class="el-icon-edit"></i> 编辑
            </el-button>
            <el-button
              size="mini"
              type="danger"
              @click="handleDeleteIp(scope.row)"
            >
              <i class="el-icon-delete"></i> 删除
            </el-button>
            <el-button
              size="mini"
              type="text"
              :disabled="scope.row.status === 'active'"
              @click="handleActivateIp(scope.row)"
            >
              <i class="el-icon-check"></i> 激活
            </el-button>
            <el-button
              size="mini"
              type="text"
              :disabled="scope.row.status !== 'active'"
              @click="handleDeactivateIp(scope.row)"
            >
              <i class="el-icon-close"></i> 禁用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div v-if="filteredIps.length === 0" class="ip-management-empty">
        <img src="https://picsum.photos/id/237/200/150" alt="No IPs" class="empty-image">
        <p class="empty-text">暂无 IP 记录</p>
      </div>
    </div>
    
    <div class="ip-management-footer">
      <Pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredIps.length"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <el-dialog
      title="添加 IP"
      :visible.sync="addDialogVisible"
      width="500px"
    >
      <template #content>
        <el-form :model="ipForm" :rules="ipRules" ref="ipFormRef" label-width="100px">
          <el-form-item label="IP 地址" prop="ip">
            <el-input v-model="ipForm.ip"></el-input>
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="ipForm.type" placeholder="请选择 IP 类型">
              <el-option label="私有" value="private"></el-option>
              <el-option label="互联网" value="internet"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="ipForm.status" placeholder="请选择状态">
              <el-option label="活跃" value="active"></el-option>
              <el-option label="非活跃" value="inactive"></el-option>
              <el-option label="警告" value="warning"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="ipForm.description"></el-input>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveIp">提交</el-button>
      </template>
    </el-dialog>
    
    <el-dialog
      title="编辑 IP"
      :visible.sync="editDialogVisible"
      width="500px"
    >
      <template #content>
        <el-form :model="editIpForm" :rules="ipRules" ref="editIpFormRef" label-width="100px">
          <el-form-item label="IP 地址" prop="ip">
            <el-input v-model="editIpForm.ip" disabled></el-input>
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="editIpForm.type" placeholder="请选择 IP 类型">
              <el-option label="私有" value="private"></el-option>
              <el-option label="互联网" value="internet"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="editIpForm.status" placeholder="请选择状态">
              <el-option label="活跃" value="active"></el-option>
              <el-option label="非活跃" value="inactive"></el-option>
              <el-option label="警告" value="warning"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="editIpForm.description"></el-input>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateIp">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import StatusIndicator from './StatusIndicator.vue';
import Pagination from './Pagination.vue';

export default {
  name: 'IpManagement',
  components: {
    StatusIndicator,
    Pagination
  },
  props: {
    ips: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      filter: {
        type: '',
        status: '',
        keyword: ''
      },
      currentPage: 1,
      pageSize: 10,
      sort: {
        prop: 'ip',
        order: 'ascending'
      },
      addDialogVisible: false,
      editDialogVisible: false,
      ipForm: {
        ip: '',
        type: 'private',
        status: 'active',
        description: ''
      },
      editIpForm: {
        id: null,
        ip: '',
        type: 'private',
        status: 'active',
        description: ''
      },
      ipRules: {
        ip: [
          { required: true, message: '请输入 IP 地址', trigger: 'blur' },
          { pattern: /^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/, message: '请输入有效的 IP 地址', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择 IP 类型', trigger: 'change' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ]
      }
    };
  },
  computed: {
    filteredIps() {
      let result = [...this.ips];
      
      // 应用筛选条件
      if (this.filter.type) {
        result = result.filter(ip => ip.type === this.filter.type);
      }
      
      if (this.filter.status) {
        result = result.filter(ip => ip.status === this.filter.status);
      }
      
      if (this.filter.keyword) {
        const keyword = this.filter.keyword.toLowerCase();
        result = result.filter(ip => 
          ip.ip.toLowerCase().includes(keyword) || 
          ip.description.toLowerCase().includes(keyword)
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
    paginatedIps() {
      const startIndex = (this.currentPage - 1) * this.pageSize;
      return this.filteredIps.slice(startIndex, startIndex + this.pageSize);
    }
  },
  methods: {
    handleRefresh() {
      this.$emit('refresh');
    },
    handleAddIp() {
      this.ipForm = {
        ip: '',
        type: 'private',
        status: 'active',
        description: ''
      };
      this.addDialogVisible = true;
    },
    handleEditIp(ip) {
      this.editIpForm = {
        id: ip.id,
        ip: ip.ip,
        type: ip.type,
        status: ip.status,
        description: ip.description
      };
      this.editDialogVisible = true;
    },
    handleDeleteIp(ip) {
      this.$confirm(`确认删除 IP ${ip.ip}？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('delete-ip', ip.id);
        this.$message({
          type: 'success',
          message: 'IP 已删除'
        });
      }).catch(() => {});
    },
    handleActivateIp(ip) {
      this.$confirm(`确认激活 IP ${ip.ip}？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('activate-ip', ip.id);
        this.$message({
          type: 'success',
          message: 'IP 已激活'
        });
      }).catch(() => {});
    },
    handleDeactivateIp(ip) {
      this.$confirm(`确认禁用 IP ${ip.ip}？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('deactivate-ip', ip.id);
        this.$message({
          type: 'success',
          message: 'IP 已禁用'
        });
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
    },
    handleSaveIp() {
      this.$refs.ipFormRef.validate(valid => {
        if (valid) {
          this.$emit('add-ip', this.ipForm);
          this.addDialogVisible = false;
          this.$message({
            type: 'success',
            message: 'IP 添加成功'
          });
        } else {
          console.log('error submit!!');
          return false;
        }
      });
    },
    handleUpdateIp() {
      this.$refs.editIpFormRef.validate(valid => {
        if (valid) {
          this.$emit('update-ip', this.editIpForm);
          this.editDialogVisible = false;
          this.$message({
            type: 'success',
            message: 'IP 更新成功'
          });
        } else {
          console.log('error submit!!');
          return false;
        }
      });
    }
  }
};
</script>

<style scoped>
.ip-management-container {
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 16px;
}

.ip-management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ip-management-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.ip-management-actions {
  display: flex;
  gap: 8px;
}

.ip-management-filter {
  margin-bottom: 16px;
}

.ip-management-content {
  margin-bottom: 16px;
}

.ip-management-empty {
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
</style>
    