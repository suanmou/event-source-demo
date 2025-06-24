<template>
  <div class="ip-whitelist">
    <el-card>
      <div slot="header" class="clearfix">
        <span>IP 白名单管理</span>
        <el-button style="float: right; margin-top: -5px" @click="handleAddIP">
          <i class="el-icon-plus"></i> 添加 IP
        </el-button>
      </div>
      <div>
        <el-input
          v-model="searchQuery"
          placeholder="搜索 IP 地址或描述"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
          style="width: 300px; margin-bottom: 20px;"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
        </el-input>
        
        <el-table
          :data="filteredIPs"
          border
          stripe
          highlight-current-row
          @row-dblclick="handleEditIP"
        >
          <el-table-column prop="ipAddress" label="IP 地址" width="180"></el-table-column>
          <el-table-column prop="sessionId" label="关联会话" width="200"></el-table-column>
          <el-table-column prop="description" label="描述"></el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'danger'">
                {{ scope.row.status === 'active' ? '活跃' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastActivity" label="最后活动时间" width="180">
            <template slot-scope="scope">
              {{ scope.row.lastActivity || '从未使用' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template slot-scope="scope">
              <el-button size="mini" type="primary" @click="handleEditIP(scope.row)">
                <i class="el-icon-edit"></i> 编辑
              </el-button>
              <el-button 
                size="mini" 
                type="danger" 
                @click="handleDeleteIP(scope.row)"
                :disabled="scope.row.status === 'disabled'"
              >
                <i class="el-icon-delete"></i> 禁用
              </el-button>
              <el-button 
                size="mini" 
                type="warning" 
                @click="handleEnableIP(scope.row)"
                :disabled="scope.row.status === 'active'"
              >
                <i class="el-icon-check"></i> 启用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="pagination">
          <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="currentPage"
            :page-sizes="[10, 20, 50, 100]"
            :page-size="pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalIPs"
          >
          </el-pagination>
        </div>
      </div>
    </el-card>
    
    <!-- 添加/编辑IP对话框 -->
    <el-dialog :visible.sync="ipDialogVisible" title="IP 管理">
      <el-form :model="ipForm" :rules="ipRules" ref="ipForm">
        <el-form-item label="IP 地址" prop="ipAddress">
          <el-input v-model="ipForm.ipAddress"></el-input>
        </el-form-item>
        <el-form-item label="关联会话" prop="sessionId">
          <el-select v-model="ipForm.sessionId" placeholder="选择会话">
            <el-option
              v-for="session in availableSessions"
              :key="session.id"
              :label="session.id"
              :value="session.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="ipForm.description"></el-input>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="ipForm.status" active-text="活跃" inactive-text="禁用"></el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="ipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveIP">保存</el-button>
      </div>
    </el-dialog>
    
    <!-- 删除确认对话框 -->
    <el-dialog :visible.sync="deleteDialogVisible" title="确认禁用">
      <div slot="content">
        <p>确定要禁用 IP: {{ ipToDelete.ipAddress }} 吗？</p>
        <p class="warning-text">禁用后，此 IP 将无法连接到系统，直到重新启用。</p>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDeleteIP">确认禁用</el-button>
      </div>
    </el-dialog>
    
    <!-- 启用确认对话框 -->
    <el-dialog :visible.sync="enableDialogVisible" title="确认启用">
      <div slot="content">
        <p>确定要启用 IP: {{ ipToEnable.ipAddress }} 吗？</p>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="enableDialogVisible = false">取消</el-button>
        <el-button type="success" @click="confirmEnableIP">确认启用</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  data() {
    return {
      // IP列表数据
      ips: [],
      filteredIPs: [],
      totalIPs: 0,
      currentPage: 1,
      pageSize: 10,
      searchQuery: '',
      
      // 对话框相关
      ipDialogVisible: false,
      ipForm: {
        id: null,
        ipAddress: '',
        sessionId: '',
        description: '',
        status: true
      },
      ipRules: {
        ipAddress: [
          { required: true, message: '请输入IP地址', trigger: 'blur' },
          { pattern: /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/, message: '请输入有效的IP地址', trigger: 'blur' }
        ],
        sessionId: [
          { required: true, message: '请选择关联会话', trigger: 'change' }
        ]
      },
      
      // 删除相关
      deleteDialogVisible: false,
      ipToDelete: null,
      
      // 启用相关
      enableDialogVisible: false,
      ipToEnable: null,
      
      // 可用会话列表
      availableSessions: [
        { id: 'FIX.4.4:SENDER1:RECEIVER1' },
        { id: 'FIX.4.4:SENDER2:RECEIVER2' },
        { id: 'FIX.4.4:SENDER3:RECEIVER3' }
      ]
    };
  },
  
  created() {
    // 初始化时加载IP列表
    this.loadIPList();
  },
  
  methods: {
    // 加载IP列表
    loadIPList() {
      this.$api.get('/api/ip-whitelist', {
        params: {
          page: this.currentPage,
          pageSize: this.pageSize,
          search: this.searchQuery
        }
      })
      .then(response => {
        this.ips = response.data.data;
        this.filteredIPs = this.ips;
        this.totalIPs = response.data.total;
      })
      .catch(error => {
        this.$message.error('加载IP列表失败: ' + error.message);
        console.error(error);
      });
    },
    
    // 处理搜索
    handleSearch() {
      this.currentPage = 1;
      this.loadIPList();
    },
    
    // 处理分页大小变化
    handleSizeChange(newSize) {
      this.pageSize = newSize;
      this.loadIPList();
    },
    
    // 处理当前页变化
    handleCurrentChange(newPage) {
      this.currentPage = newPage;
      this.loadIPList();
    },
    
    // 处理添加IP
    handleAddIP() {
      this.ipForm = {
        id: null,
        ipAddress: '',
        sessionId: '',
        description: '',
        status: true
      };
      this.ipDialogVisible = true;
    },
    
    // 处理编辑IP
    handleEditIP(row) {
      this.ipForm = {
        id: row.id,
        ipAddress: row.ipAddress,
        sessionId: row.sessionId,
        description: row.description,
        status: row.status === 'active'
      };
      this.ipDialogVisible = true;
    },
    
    // 保存IP
    saveIP() {
      this.$refs.ipForm.validate(valid => {
        if (valid) {
          const formData = {
            ipAddress: this.ipForm.ipAddress,
            sessionId: this.ipForm.sessionId,
            description: this.ipForm.description,
            status: this.ipForm.status ? 'active' : 'disabled'
          };
          
          if (this.ipForm.id) {
            // 更新现有IP
            this.$api.put(`/api/ip-whitelist/${this.ipForm.id}`, formData)
              .then(() => {
                this.$message.success('IP 更新成功');
                this.ipDialogVisible = false;
                this.loadIPList();
              })
              .catch(error => {
                this.$message.error('IP 更新失败: ' + error.message);
                console.error(error);
              });
          } else {
            // 添加新IP
            this.$api.post('/api/ip-whitelist', formData)
              .then(() => {
                this.$message.success('IP 添加成功');
                this.ipDialogVisible = false;
                this.loadIPList();
              })
              .catch(error => {
                this.$message.error('IP 添加失败: ' + error.message);
                console.error(error);
              });
          }
        }
      });
    },
    
    // 处理删除IP
    handleDeleteIP(row) {
      this.ipToDelete = row;
      this.deleteDialogVisible = true;
    },
    
    // 确认删除IP
    confirmDeleteIP() {
      this.$api.put(`/api/ip-whitelist/${this.ipToDelete.id}/disable`)
        .then(() => {
          this.$message.success('IP 禁用成功');
          this.deleteDialogVisible = false;
          this.loadIPList();
        })
        .catch(error => {
          this.$message.error('IP 禁用失败: ' + error.message);
          console.error(error);
        });
    },
    
    // 处理启用IP
    handleEnableIP(row) {
      this.ipToEnable = row;
      this.enableDialogVisible = true;
    },
    
    // 确认启用IP
    confirmEnableIP() {
      this.$api.put(`/api/ip-whitelist/${this.ipToEnable.id}/enable`)
        .then(() => {
          this.$message.success('IP 启用成功');
          this.enableDialogVisible = false;
          this.loadIPList();
        })
        .catch(error => {
          this.$message.error('IP 启用失败: ' + error.message);
          console.error(error);
        });
    }
  }
};
</script>

<style scoped>
.ip-whitelist {
  padding: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.warning-text {
  color: #f56c6c;
  margin-top: 10px;
}
</style>
    