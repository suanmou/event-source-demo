<template>
  <div class="connections-container">
    <div class="card-title">
      <i class="fa fa-link mr-2"></i>FIX 连接管理
      <el-button size="small" type="primary" style="float: right;" @click="openAddConnectionDialog">
        <i class="fa fa-plus mr-1"></i>添加连接
      </el-button>
    </div>
    
    <div class="bg-white p-4 rounded-lg shadow-sm">
      <div class="flex justify-between items-center mb-4">
        <el-input
          v-model="connectionSearch"
          placeholder="搜索连接名称或ID"
          clearable
          style="width: 300px;">
          <i slot="prefix" class="fa fa-search"></i>
        </el-input>
        <el-button size="small" type="success" @click="testAllConnections">
          <i class="fa fa-plug mr-1"></i>测试所有连接
        </el-button>
      </div>
      
      <el-table
        :data="filteredConnections"
        border
        stripe
        @row-click="handleConnectionClick">
        <el-table-column prop="id" label="连接ID" width="120"></el-table-column>
        <el-table-column prop="name" label="连接名称"></el-table-column>
        <el-table-column prop="type" label="连接类型" width="100">
          <template slot-scope="scope">
            {{ getConnectionTypeText(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <StatusIndicator :status="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="host" label="服务器地址"></el-table-column>
        <el-table-column prop="port" label="端口" width="80"></el-table-column>
        <el-table-column prop="lastActive" label="最后活动时间" width="180"></el-table-column>
        <el-table-column label="操作" width="160">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleEditConnection(scope.row)">
              <i class="fa fa-edit mr-1"></i>编辑
            </el-button>
            <el-button size="mini" type="danger" @click="handleDeleteConnection(scope.row)">
              <i class="fa fa-trash mr-1"></i>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <Pagination 
        :total="filteredConnections.length" 
        :page-size="pageSize" 
        :current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 添加连接对话框 -->
    <el-dialog :visible.sync="addConnectionDialogVisible" title="添加 FIX 连接">
      <AddConnectionForm 
        :connection-types="connectionTypes"
        :auth-methods="authMethods"
        :form-data="newConnection"
        :rules="connectionRules"
        @save="handleAddConnection"
      />
    </el-dialog>
  </div>
</template>

<script>
import { mockConnections } from '../mockData';
import StatusIndicator from './StatusIndicator.vue';
import Pagination from './Pagination.vue';
import AddConnectionForm from './AddConnectionForm.vue';

export default {
  name: 'ConnectionManagement',
  components: {
    StatusIndicator,
    Pagination,
    AddConnectionForm
  },
  data() {
    return {
      connections: [...mockConnections],
      connectionSearch: '',
      newConnection: {
        name: '',
        type: 'market_data',
        host: '',
        port: 8228,
        authMethod: 'tls',
        description: ''
      },
      connectionRules: {
        name: [
          { required: true, message: '请输入连接名称', trigger: 'blur' }
        ],
        host: [
          { required: true, message: '请输入服务器地址', trigger: 'blur' }
        ],
        port: [
          { required: true, message: '请输入端口', trigger: 'blur' },
          { type: 'number', message: '端口必须为数字', trigger: 'blur' }
        ]
      },
      connectionTypes: [
        { value: 'market_data', label: '市场数据' },
        { value: 'execution', label: '交易执行' },
        { value: 'clearing', label: '清算' },
        { value: 'risk', label: '风险管理' }
      ],
      authMethods: [
        { value: 'tls', label: 'TLS 证书' },
        { value: 'basic', label: '用户名/密码' },
        { value: '2fa', label: '双因素认证' }
      ],
      addConnectionDialogVisible: false,
      pageSize: 10,
      currentPage: 1
    };
  },
  computed: {
    filteredConnections() {
      return this.connections.filter(conn => {
        const search = this.connectionSearch.toLowerCase();
        return conn.id.toLowerCase().includes(search) || 
               conn.name.toLowerCase().includes(search);
      });
    }
  },
  methods: {
    getConnectionTypeText(type) {
      const item = this.connectionTypes.find(t => t.value === type);
      return item ? item.label : type;
    },
    openAddConnectionDialog() {
      this.newConnection = {
        name: '',
        type: 'market_data',
        host: '',
        port: 8228,
        authMethod: 'tls',
        description: ''
      };
      this.addConnectionDialogVisible = true;
    },
    handleAddConnection() {
      // 模拟添加连接
      const newId = `CONN-${String(this.connections.length + 1).padStart(3, '0')}`;
      const newConnection = {
        ...this.newConnection,
        id: newId,
        status: 'inactive',
        lastActive: null
      };
      
      this.connections.push(newConnection);
      this.addConnectionDialogVisible = false;
      
      // 显示成功消息
      this.$notify({
        title: '成功',
        message: '连接添加成功',
        type: 'success'
      });
    },
    handleEditConnection(connection) {
      // 编辑连接逻辑
      console.log('Edit connection:', connection);
      this.$notify({
        title: '提示',
        message: '编辑功能将在后续版本中实现',
        type: 'info'
      });
    },
    handleDeleteConnection(connection) {
      this.$confirm('确定要删除此连接吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟删除连接
        this.connections = this.connections.filter(conn => conn.id !== connection.id);
        this.$notify({
          title: '成功',
          message: '连接删除成功',
          type: 'success'
        });
      }).catch(() => {
        // 用户取消操作
      });
    },
    handleConnectionClick(row) {
      // 连接点击事件
      console.log('Connection clicked:', row);
    },
    testAllConnections() {
      // 测试所有连接
      this.$notify({
        title: '测试中',
        message: '正在测试所有连接，请稍候...',
        type: 'info'
      });
      
      // 模拟测试结果
      setTimeout(() => {
        this.$notify({
          title: '测试完成',
          message: '所有连接测试完成，结果已更新',
          type: 'success'
        });
      }, 1500);
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
