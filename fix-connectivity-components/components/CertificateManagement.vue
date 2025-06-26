<template>
  <div class="certificates-container">
    <div class="card-title">
      <i class="fa fa-certificate mr-2"></i>TLS 证书管理
      <el-button size="small" type="primary" style="float: right;" @click="openAddCertificateDialog">
        <i class="fa fa-plus mr-1"></i>添加证书
      </el-button>
    </div>
    
    <div class="bg-white p-4 rounded-lg shadow-sm">
      <div class="flex justify-between items-center mb-4">
        <el-input
          v-model="certificateSearch"
          placeholder="搜索证书名称或ID"
          clearable
          style="width: 300px;">
          <i slot="prefix" class="fa fa-search"></i>
        </el-input>
        <el-button size="small" type="success" @click="renewAllExpiringCertificates">
          <i class="fa fa-refresh mr-1"></i>更新即将过期证书
        </el-button>
      </div>
      
      <el-table
        :data="filteredCertificates"
        border
        stripe>
        <el-table-column prop="id" label="证书ID" width="120"></el-table-column>
        <el-table-column prop="name" label="证书名称"></el-table-column>
        <el-table-column prop="type" label="证书类型" width="120">
          <template slot-scope="scope">
            {{ getCertificateTypeText(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <StatusIndicator :status="scope.row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="issuedBy" label="颁发机构"></el-table-column>
        <el-table-column prop="issuedAt" label="颁发时间" width="180"></el-table-column>
        <el-table-column prop="expiresAt" label="过期时间" width="180">
          <template slot-scope="scope">
            <span :class="getExpiryStatusClass(scope.row.expiresAt)">
              {{ scope.row.expiresAt }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="daysLeft" label="剩余天数" width="100">
          <template slot-scope="scope">
            <span :class="getDaysLeftClass(scope.row.daysLeft)">
              {{ scope.row.daysLeft }} 天
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="handleEditCertificate(scope.row)">
              <i class="fa fa-edit mr-1"></i>编辑
            </el-button>
            <el-button size="mini" type="warning" @click="handleRenewCertificate(scope.row)">
              <i class="fa fa-refresh mr-1"></i>更新
            </el-button>
            <el-button size="mini" type="danger" @click="handleDeleteCertificate(scope.row)">
              <i class="fa fa-trash mr-1"></i>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <Pagination 
        :total="filteredCertificates.length" 
        :page-size="pageSize" 
        :current-page="currentPage"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <!-- 添加证书对话框 -->
    <el-dialog :visible.sync="addCertificateDialogVisible" title="添加 TLS 证书">
      <AddCertificateForm 
        :certificate-types="certificateTypes"
        :connections="connections"
        :form-data="newCertificate"
        :rules="certificateRules"
        @save="handleAddCertificate"
      />
    </el-dialog>
  </div>
</template>

<script>
import { mockCertificates, mockConnections } from '../mockData';
import StatusIndicator from './StatusIndicator.vue';
import Pagination from './Pagination.vue';
import AddCertificateForm from './AddCertificateForm.vue';

export default {
  name: 'CertificateManagement',
  components: {
    StatusIndicator,
    Pagination,
    AddCertificateForm
  },
  data() {
    return {
      certificates: [...mockCertificates],
      connections: [...mockConnections],
      certificateSearch: '',
      newCertificate: {
        name: '',
        type: 'rsa2048',
        issuedBy: '',
        issuedAt: null,
        expiresAt: null,
        connectionId: null,
        certificateContent: ''
      },
      certificateRules: {
        name: [
          { required: true, message: '请输入证书名称', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择证书类型', trigger: 'change' }
        ],
        issuedBy: [
          { required: true, message: '请输入颁发机构', trigger: 'blur' }
        ],
        issuedAt: [
          { required: true, message: '请选择颁发时间', trigger: 'change' }
        ],
        expiresAt: [
          { required: true, message: '请选择过期时间', trigger: 'change' }
        ],
        certificateContent: [
          { required: true, message: '请输入证书内容', trigger: 'blur' }
        ]
      },
      certificateTypes: [
        { value: 'rsa2048', label: 'RSA 2048' },
        { value: 'rsa4096', label: 'RSA 4096' },
        { value: 'ecdsa', label: 'ECDSA' }
      ],
      addCertificateDialogVisible: false,
      pageSize: 10,
      currentPage: 1
    };
  },
  computed: {
    filteredCertificates() {
      return this.certificates.filter(cert => {
        const search = this.certificateSearch.toLowerCase();
        return cert.id.toLowerCase().includes(search) || 
               cert.name.toLowerCase().includes(search);
      });
    }
  },
  methods: {
    getCertificateTypeText(type) {
      const item = this.certificateTypes.find(t => t.value === type);
      return item ? item.label : type;
    },
    getExpiryStatusClass(expiryDate) {
      const today = new Date();
      const expiry = new Date(expiryDate);
      const diffTime = expiry - today;
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      
      if (diffDays < 0) {
        return 'text-red-500 font-bold';
      } else if (diffDays <= 30) {
        return 'text-orange-500 font-bold';
      } else {
        return 'text-gray-600';
      }
    },
    getDaysLeftClass(daysLeft) {
      if (daysLeft < 0) {
        return 'text-red-500 font-bold';
      } else if (daysLeft <= 30) {
        return 'text-orange-500 font-bold';
      } else {
        return 'text-gray-600';
      }
    },
    openAddCertificateDialog() {
      this.newCertificate = {
        name: '',
        type: 'rsa2048',
        issuedBy: '',
        issuedAt: null,
        expiresAt: null,
        connectionId: null,
        certificateContent: ''
      };
      this.addCertificateDialogVisible = true;
    },
    handleAddCertificate() {
      // 模拟添加证书
      const newId = `CERT-${String(this.certificates.length + 1).padStart(3, '0')}`;
      
      // 计算剩余天数
      const today = new Date();
      const expiryDate = new Date(this.newCertificate.expiresAt);
      const diffTime = expiryDate - today;
      const daysLeft = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      
      const newCertificate = {
        ...this.newCertificate,
        id: newId,
        status: daysLeft < 0 ? 'expired' : 'active',
        daysLeft: daysLeft
      };
      
      this.certificates.push(newCertificate);
      this.addCertificateDialogVisible = false;
      
      // 显示成功消息
      this.$notify({
        title: '成功',
        message: '证书添加成功',
        type: 'success'
      });
    },
    handleEditCertificate(certificate) {
      // 编辑证书逻辑
      console.log('Edit certificate:', certificate);
      this.$notify({
        title: '提示',
        message: '编辑功能将在后续版本中实现',
        type: 'info'
      });
    },
    handleRenewCertificate(certificate) {
      this.$confirm('确定要更新此证书吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟更新证书
        const updatedCertificates = this.certificates.map(cert => {
          if (cert.id === certificate.id) {
            // 证书有效期延长一年
            const newExpiryDate = new Date(certificate.expiresAt);
            newExpiryDate.setFullYear(newExpiryDate.getFullYear() + 1);
            
            // 计算新的剩余天数
            const today = new Date();
            const diffTime = newExpiryDate - today;
            const daysLeft = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            
            return {
              ...cert,
              expiresAt: newExpiryDate.toISOString().split('T')[0],
              daysLeft: daysLeft,
              status: 'active'
            };
          }
          return cert;
        });
        
        this.certificates = updatedCertificates;
        
        this.$notify({
          title: '成功',
          message: '证书更新成功',
          type: 'success'
        });
      }).catch(() => {
        // 用户取消操作
      });
    },
    handleDeleteCertificate(certificate) {
      this.$confirm('确定要删除此证书吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟删除证书
        this.certificates = this.certificates.filter(cert => cert.id !== certificate.id);
        this.$notify({
          title: '成功',
          message: '证书删除成功',
          type: 'success'
        });
      }).catch(() => {
        // 用户取消操作
      });
    },
    renewAllExpiringCertificates() {
      // 获取30天内即将过期的证书
      const expiringCertificates = this.certificates.filter(cert => cert.daysLeft <= 30 && cert.daysLeft >= 0);
      
      if (expiringCertificates.length === 0) {
        this.$notify({
          title: '提示',
          message: '没有即将过期的证书',
          type: 'info'
        });
        return;
      }
      
      this.$confirm(`确定要更新 ${expiringCertificates.length} 个即将过期的证书吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟更新所有即将过期的证书
        const updatedCertificates = this.certificates.map(cert => {
          if (cert.daysLeft <= 30 && cert.daysLeft >= 0) {
            // 证书有效期延长一年
            const newExpiryDate = new Date(cert.expiresAt);
            newExpiryDate.setFullYear(newExpiryDate.getFullYear() + 1);
            
            // 计算新的剩余天数
            const today = new Date();
            const diffTime = newExpiryDate - today;
            const daysLeft = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            
            return {
              ...cert,
              expiresAt: newExpiryDate.toISOString().split('T')[0],
              daysLeft: daysLeft,
              status: 'active'
            };
          }
          return cert;
        });
        
        this.certificates = updatedCertificates;
        
        this.$notify({
          title: '成功',
          message: `已更新 ${expiringCertificates.length} 个即将过期的证书`,
          type: 'success'
        });
      }).catch(() => {
        // 用户取消操作
      });
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
