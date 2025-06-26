<template>
  <div class="dns-management-container">
    <div class="dns-management-header">
      <h3 class="dns-management-title">DNS 配置</h3>
      <div class="dns-management-actions">
        <el-button type="primary" @click="handleAddDns">
          <i class="el-icon-plus"></i> 添加 DNS
        </el-button>
        <el-button @click="handleRefresh">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </div>
    </div>
    
    <div class="dns-management-filter">
      <el-row :gutter="10">
        <el-col :span="6">
          <el-select v-model="filter.type" placeholder="记录类型">
            <el-option label="全部" value=""></el-option>
            <el-option label="A" value="A"></el-option>
            <el-option label="AAAA" value="AAAA"></el-option>
            <el-option label="CNAME" value="CNAME"></el-option>
            <el-option label="MX" value="MX"></el-option>
            <el-option label="TXT" value="TXT"></el-option>
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filter.status" placeholder="状态">
            <el-option label="全部" value=""></el-option>
            <el-option label="活跃" value="active"></el-option>
            <el-option label="非活跃" value="inactive"></el-option>
          </el-select>
        </el-col>
        <el-col :span="12">
          <el-input v-model="filter.keyword" placeholder="搜索域名或记录值" suffix-icon="el-icon-search"></el-input>
        </el-col>
      </el-row>
    </div>
    
    <div class="dns-management-content">
      <el-table
        :data="filteredDnsRecords"
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
          label="域名"
          prop="domain"
          sortable
          min-width="150"
        ></el-table-column>
        <el-table-column
          label="记录类型"
          prop="type"
          width="100"
          sortable
        ></el-table-column>
        <el-table-column
          label="记录值"
          prop="value"
          min-width="200"
        ></el-table-column>
        <el-table-column
          label="TTL (秒)"
          prop="ttl"
          width="120"
          sortable
        ></el-table-column>
        <el-table-column
          label="优先级"
          prop="priority"
          width="100"
          sortable
        ></el-table-column>
        <el-table-column
          label="更新时间"
          prop="updatedAt"
          width="180"
          sortable
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
              @click="handleEditDns(scope.row)"
            >
              <i class="el-icon-edit"></i> 编辑
            </el-button>
            <el-button
              size="mini"
              type="danger"
              @click="handleDeleteDns(scope.row)"
            >
              <i class="el-icon-delete"></i> 删除
            </el-button>
            <el-button
              size="mini"
              type="text"
              :disabled="scope.row.status === 'active'"
              @click="handleActivateDns(scope.row)"
            >
              <i class="el-icon-check"></i> 激活
            </el-button>
            <el-button
              size="mini"
              type="text"
              :disabled="scope.row.status !== 'active'"
              @click="handleDeactivateDns(scope.row)"
            >
              <i class="el-icon-close"></i> 禁用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div v-if="filteredDnsRecords.length === 0" class="dns-management-empty">
        <img src="https://picsum.photos/id/237/200/150" alt="No DNS records" class="empty-image">
        <p class="empty-text">暂无 DNS 记录</p>
      </div>
    </div>
    
    <div class="dns-management-footer">
      <Pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="filteredDnsRecords.length"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <el-dialog
      title="添加 DNS 记录"
      :visible.sync="addDialogVisible"
      width="500px"
    >
      <template #content>
        <el-form :model="dnsForm" :rules="dnsRules" ref="dnsFormRef" label-width="100px">
          <el-form-item label="域名" prop="domain">
            <el-input v-model="dnsForm.domain"></el-input>
          </el-form-item>
          <el-form-item label="记录类型" prop="type">
            <el-select v-model="dnsForm.type" placeholder="请选择记录类型">
              <el-option label="A" value="A"></el-option>
              <el-option label="AAAA" value="AAAA"></el-option>
              <el-option label="CNAME" value="CNAME"></el-option>
              <el-option label="MX" value="MX"></el-option>
              <el-option label="TXT" value="TXT"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="记录值" prop="value">
            <el-input v-model="dnsForm.value"></el-input>
          </el-form-item>
          <el-form-item label="TTL (秒)" prop="ttl">
            <el-input v-model="dnsForm.ttl" type="number"></el-input>
          </el-form-item>
          <el-form-item label="优先级" prop="priority" v-if="dnsForm.type === 'MX'">
            <el-input v-model="dnsForm.priority" type="number"></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="dnsForm.status" placeholder="请选择状态">
              <el-option label="活跃" value="active"></el-option>
              <el-option label="非活跃" value="inactive"></el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveDns">提交</el-button>
      </template>
    </el-dialog>
    
    <el-dialog
      title="编辑 DNS 记录"
      :visible.sync="editDialogVisible"
      width="500px"
    >
      <template #content>
        <el-form :model="editDnsForm" :rules="dnsRules" ref="editDnsFormRef" label-width="100px">
          <el-form-item label="域名" prop="domain">
            <el-input v-model="editDnsForm.domain" disabled></el-input>
          </el-form-item>
          <el-form-item label="记录类型" prop="type">
            <el-select v-model="editDnsForm.type" placeholder="请选择记录类型">
              <el-option label="A" value="A"></el-option>
              <el-option label="AAAA" value="AAAA"></el-option>
              <el-option label="CNAME" value="CNAME"></el-option>
              <el-option label="MX" value="MX"></el-option>
              <el-option label="TXT" value="TXT"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="记录值" prop="value">
            <el-input v-model="editDnsForm.value"></el-input>
          </el-form-item>
          <el-form-item label="TTL (秒)" prop="ttl">
            <el-input v-model="editDnsForm.ttl" type="number"></el-input>
          </el-form-item>
          <el-form-item label="优先级" prop="priority" v-if="editDnsForm.type === 'MX'">
            <el-input v-model="editDnsForm.priority" type="number"></el-input>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="editDnsForm.status" placeholder="请选择状态">
              <el-option label="活跃" value="active"></el-option>
              <el-option label="非活跃" value="inactive"></el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateDns">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import StatusIndicator from './StatusIndicator.vue';
import Pagination from './Pagination.vue';

export default {
  name: 'DnsManagement',
  components: {
    StatusIndicator,
    Pagination
  },
  props: {
    dnsRecords: {
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
        prop: 'domain',
        order: 'ascending'
      },
      addDialogVisible: false,
      editDialogVisible: false,
      dnsForm: {
        domain: '',
        type: 'A',
        value: '',
        ttl: 3600,
        priority: 10,
        status: 'active'
      },
      editDnsForm: {
        id: null,
        domain: '',
        type: 'A',
        value: '',
        ttl: 3600,
        priority: 10,
        status: 'active'
      },
      dnsRules: {
        domain: [
          { required: true, message: '请输入域名', trigger: 'blur' },
          { pattern: /^([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}$/, message: '请输入有效的域名', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择记录类型', trigger: 'change' }
        ],
        value: [
          { required: true, message: '请输入记录值', trigger: 'blur' }
        ],
        ttl: [
          { required: true, message: '请输入 TTL', trigger: 'blur' },
          { type: 'number', message: 'TTL 必须为数字', trigger: 'blur' }
        ]
      }
    };
  },
  computed: {
    filteredDnsRecords() {
      let result = [...this.dnsRecords];
      
      // 应用筛选条件
      if (this.filter.type) {
        result = result.filter(record => record.type === this.filter.type);
      }
      
      if (this.filter.status) {
        result = result.filter(record => record.status === this.filter.status);
      }
      
      if (this.filter.keyword) {
        const keyword = this.filter.keyword.toLowerCase();
        result = result.filter(record => 
          record.domain.toLowerCase().includes(keyword) || 
          record.value.toLowerCase().includes(keyword)
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
    paginatedDnsRecords() {
      const startIndex = (this.currentPage - 1) * this.pageSize;
      return this.filteredDnsRecords.slice(startIndex, startIndex + this.pageSize);
    }
  },
  methods: {
    handleRefresh() {
      this.$emit('refresh');
    },
    handleAddDns() {
      this.dnsForm = {
        domain: '',
        type: 'A',
        value: '',
        ttl: 3600,
        priority: 10,
        status: 'active'
      };
      this.addDialogVisible = true;
    },
    handleEditDns(record) {
      this.editDnsForm = {
        id: record.id,
        domain: record.domain,
        type: record.type,
        value: record.value,
        ttl: record.ttl,
        priority: record.priority || 10,
        status: record.status
      };
      this.editDialogVisible = true;
    },
    handleDeleteDns(record) {
      this.$confirm(`确认删除域名 ${record.domain} 的 ${record.type} 记录？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('delete-dns', record.id);
        this.$message({
          type: 'success',
          message: 'DNS 记录已删除'
        });
      }).catch(() => {});
    },
    handleActivateDns(record) {
      this.$confirm(`确认激活域名 ${record.domain} 的 ${record.type} 记录？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('activate-dns', record.id);
        this.$message({
          type: 'success',
          message: 'DNS 记录已激活'
        });
      }).catch(() => {});
    },
    handleDeactivateDns(record) {
      this.$confirm(`确认禁用域名 ${record.domain} 的 ${record.type} 记录？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('deactivate-dns', record.id);
        this.$message({
          type: 'success',
          message: 'DNS 记录已禁用'
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
    handleSaveDns() {
      this.$refs.dnsFormRef.validate(valid => {
        if (valid) {
          this.$emit('add-dns', this.dnsForm);
          this.addDialogVisible = false;
          this.$message({
            type: 'success',
            message: 'DNS 记录添加成功'
          });
        } else {
          console.log('error submit!!');
          return false;
        }
      });
    },
    handleUpdateDns() {
      this.$refs.editDnsFormRef.validate(valid => {
        if (valid) {
          this.$emit('update-dns', this.editDnsForm);
          this.editDialogVisible = false;
          this.$message({
            type: 'success',
            message: 'DNS 记录更新成功'
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
.dns-management-container {
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 16px;
}

.dns-management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.dns-management-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.dns-management-actions {
  display: flex;
  gap: 8px;
}

.dns-management-filter {
  margin-bottom: 16px;
}

.dns-management-content {
  margin-bottom: 16px;
}

.dns-management-empty {
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
    