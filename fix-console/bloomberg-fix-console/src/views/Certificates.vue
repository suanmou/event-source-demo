<template>
  <div class="certificates">
    <el-card>
      <div slot="header" class="clearfix">
        <span>证书管理</span>
        <div class="btn-group">
          <el-button @click="handleGenerateCertificate">
            <i class="el-icon-plus"></i> 生成证书
          </el-button>
          <el-button @click="handleImportCertificate">
            <i class="el-icon-upload"></i> 导入证书
          </el-button>
        </div>
      </div>
      <div>
        <el-tabs v-model="activeTab" @tab-click="handleTabClick">
          <el-tab-pane label="客户端证书" name="client">
            <el-table
              :data="clientCertificates"
              border
              stripe
              highlight-current-row
            >
              <el-table-column prop="serialNumber" label="序列号" width="180"></el-table-column>
              <el-table-column prop="subject" label="主题"></el-table-column>
              <el-table-column prop="sessionId" label="关联会话" width="200"></el-table-column>
              <el-table-column prop="validFrom" label="有效期开始" width="180"></el-table-column>
              <el-table-column prop="validTo" label="有效期结束" width="180"></el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template slot-scope="scope">
                  <el-tag :type="getTagType(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="lastActivity" label="最后活动时间" width="180">
                <template slot-scope="scope">
                  {{ scope.row.lastActivity || '从未使用' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220">
                <template slot-scope="scope">
                  <el-button size="mini" type="primary" @click="handleViewCertificate(scope.row)">
                    <i class="el-icon-view"></i> 查看
                  </el-button>
                  <el-button size="mini" type="success" @click="handleDownloadCertificate(scope.row)">
                    <i class="el-icon-download"></i> 下载
                  </el-button>
                  <el-button 
                    size="mini" 
                    type="danger" 
                    @click="handleRevokeCertificate(scope.row)"
                    :disabled="scope.row.status === 'revoked'"
                  >
                    <i class="el-icon-delete"></i> 吊销
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="服务器证书" name="server">
            <el-table
              :data="serverCertificates"
              border
              stripe
              highlight-current-row
            >
              <el-table-column prop="serialNumber" label="序列号" width="180"></el-table-column>
              <el-table-column prop="subject" label="主题"></el-table-column>
              <el-table-column prop="validFrom" label="有效期开始" width="180"></el-table-column>
              <el-table-column prop="validTo" label="有效期结束" width="180"></el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template slot-scope="scope">
                  <el-tag :type="getTagType(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="lastActivity" label="最后活动时间" width="180">
                <template slot-scope="scope">
                  {{ scope.row.lastActivity || '从未使用' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220">
                <template slot-scope="scope">
                  <el-button size="mini" type="primary" @click="handleViewCertificate(scope.row)">
                    <i class="el-icon-view"></i> 查看
                  </el-button>
                  <el-button size="mini" type="success" @click="handleDownloadCertificate(scope.row)">
                    <i class="el-icon-download"></i> 下载
                  </el-button>
                  <el-button 
                    size="mini" 
                    type="danger" 
                    @click="handleRevokeCertificate(scope.row)"
                    :disabled="scope.row.status === 'revoked'"
                  >
                    <i class="el-icon-delete"></i> 吊销
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
    
    <!-- 证书详情对话框 -->
    <el-dialog :visible.sync="certificateDialogVisible" title="证书详情">
      <div class="certificate-details">
        <el-card>
          <div class="certificate-info">
            <div class="info-item">
              <span class="label">序列号:</span>
              <span class="value">{{ selectedCertificate.serialNumber }}</span>
            </div>
            <div class="info-item">
              <span class="label">主题:</span>
              <span class="value">{{ selectedCertificate.subject }}</span>
            </div>
            <div class="info-item">
              <span class="label">颁发者:</span>
              <span class="value">{{ selectedCertificate.issuer }}</span>
            </div>
            <div class="info-item">
              <span class="label">有效期开始:</span>
              <span class="value">{{ selectedCertificate.validFrom }}</span>
            </div>
            <div class="info-item">
              <span class="label">有效期结束:</span>
              <span class="value">{{ selectedCertificate.validTo }}</span>
            </div>
            <div class="info-item">
              <span class="label">密钥大小:</span>
              <span class="value">{{ selectedCertificate.keySize }} 位</span>
            </div>
            <div class="info-item">
              <span class="label">密码套件:</span>
              <span class="value">{{ selectedCertificate.cipherSuite }}</span>
            </div>
            <div class="info-item">
              <span class="label">指纹(SHA-1):</span>
              <span class="value">{{ selectedCertificate.fingerprint }}</span>
            </div>
            <div class="info-item">
              <span class="label">状态:</span>
              <span class="value">
                <el-tag :type="getTagType(selectedCertificate.status)">
                  {{ getStatusText(selectedCertificate.status) }}
                </el-tag>
              </span>
            </div>
            <div class="info-item">
              <span class="label">最后活动时间:</span>
              <span class="value">{{ selectedCertificate.lastActivity || '从未使用' }}</span>
            </div>
          </div>
          
          <div class="certificate-content">
            <h4>证书内容</h4>
            <el-card>
              <pre>{{ selectedCertificate.content }}</pre>
            </el-card>
          </div>
        </el-card>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="certificateDialogVisible = false">关闭</el-button>
        <el-button type="success" @click="handleDownloadCertificate(selectedCertificate)">
          <i class="el-icon-download"></i> 下载证书
        </el-button>
      </div>
    </el-dialog>
    
    <!-- 生成证书对话框 -->
    <el-dialog :visible.sync="generateDialogVisible" title="生成新证书">
      <el-form :model="generateForm" :rules="generateRules" ref="generateForm">
        <el-form-item label="证书类型" prop="certType">
          <el-radio-group v-model="generateForm.certType">
            <el-radio label="client">客户端证书</el-radio>
            <el-radio label="server">服务器证书</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联会话" prop="sessionId" v-if="generateForm.certType === 'client'">
          <el-select v-model="generateForm.sessionId" placeholder="选择会话">
            <el-option
              v-for="session in availableSessions"
              :key="session.id"
              :label="session.id"
              :value="session.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="通用名称 (CN)" prop="commonName">
          <el-input v-model="generateForm.commonName"></el-input>
        </el-form-item>
        <el-form-item label="组织 (O)" prop="organization">
          <el-input v-model="generateForm.organization"></el-input>
        </el-form-item>
        <el-form-item label="组织单位 (OU)" prop="organizationalUnit">
          <el-input v-model="generateForm.organizationalUnit"></el-input>
        </el-form-item>
        <el-form-item label="城市 (L)" prop="locality">
          <el-input v-model="generateForm.locality"></el-input>
        </el-form-item>
        <el-form-item label="省份 (ST)" prop="state">
          <el-input v-model="generateForm.state"></el-input>
        </el-form-item>
        <el-form-item label="国家 (C)" prop="country">
          <el-input v-model="generateForm.country"></el-input>
        </el-form-item>
        <el-form-item label="密钥大小" prop="keySize">
          <el-select v-model="generateForm.keySize">
            <el-option label="2048 位" value="2048"></el-option>
            <el-option label="3072 位" value="3072"></el-option>
            <el-option label="4096 位" value="4096"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="有效期 (天)" prop="validityDays">
          <el-input v-model="generateForm.validityDays" type="number"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGenerate">生成证书</el-button>
      </div>
    </el-dialog>
    
    <!-- 导入证书对话框 -->
    <el-dialog :visible.sync="importDialogVisible" title="导入证书">
      <el-form :model="importForm" :rules="importRules" ref="importForm">
        <el-form-item label="证书类型" prop="certType">
          <el-radio-group v-model="importForm.certType">
            <el-radio label="client">客户端证书</el-radio>
            <el-radio label="server">服务器证书</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联会话" prop="sessionId" v-if="importForm.certType === 'client'">
          <el-select v-model="importForm.sessionId" placeholder="选择会话">
            <el-option
              v-for="session in availableSessions"
              :key="session.id"
              :label="session.id"
              :value="session.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="证书文件" prop="certFile">
          <el-upload
            ref="certUpload"
            action="#"
            :show-file-list="false"
            :before-upload="beforeCertUpload"
          >
            <el-button size="small" type="primary">选择证书文件</el-button>
          </el-upload>
          <div v-if="importForm.certFileName" class="file-name">
            {{ importForm.certFileName }}
          </div>
        </el-form-item>
        <el-form-item label="私钥文件" prop="keyFile">
          <el-upload
            ref="keyUpload"
            action="#"
            :show-file-list="false"
            :before-upload="beforeKeyUpload"
          >
            <el-button size="small" type="primary">选择私钥文件</el-button>
          </el-upload>
          <div v-if="importForm.keyFileName" class="file-name">
            {{ importForm.keyFileName }}
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport">导入证书</el-button>
      </div>
    </el-dialog>
    
    <!-- 吊销确认对话框 -->
    <el-dialog :visible.sync="revokeDialogVisible" title="确认吊销证书">
      <div slot="content">
        <p>确定要吊销证书 {{ selectedCertificate.serialNumber }} 吗？</p>
        <p class="warning-text">吊销后，此证书将无法用于建立连接，直到重新颁发。</p>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="revokeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleConfirmRevoke">确认吊销</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  data() {
    return {
      // 选项卡状态
      activeTab: 'client',
      
      // 证书数据
      clientCertificates: [],
      serverCertificates: [],
      
      // 对话框相关
      certificateDialogVisible: false,
      selectedCertificate: null,
      
      generateDialogVisible: false,
      generateForm: {
        certType: 'client',
        sessionId: '',
        commonName: '',
        organization: '',
        organizationalUnit: '',
        locality: '',
        state: '',
        country: '',
        keySize: '4096',
        validityDays: 365
      },
      generateRules: {
        commonName: [
          { required: true, message: '请输入通用名称', trigger: 'blur' }
        ],
        organization: [
          { required: true, message: '请输入组织名称', trigger: 'blur' }
        ],
        country: [
          { required: true, message: '请输入国家代码', trigger: 'blur' },
          { pattern: /^[A-Z]{2}$/, message: '请输入有效的国家代码 (如: US, CN)', trigger: 'blur' }
        ],
        keySize: [
          { required: true, message: '请选择密钥大小', trigger: 'change' }
        ],
        validityDays: [
          { required: true, message: '请输入有效期天数', trigger: 'blur' },
          { type: 'number', message: '请输入有效的数字', trigger: 'blur' }
        ]
      },
      
      importDialogVisible: false,
      importForm: {
        certType: 'client',
        sessionId: '',
        certFile: null,
        certFileName: '',
        keyFile: null,
        keyFileName: ''
      },
      importRules: {
        certFile: [
          { required: true, message: '请选择证书文件', trigger: 'change' }
        ],
        keyFile: [
          { required: true, message: '请选择私钥文件', trigger: 'change' }
        ]
      },
      
      revokeDialogVisible: false,
      
      // 可用会话列表
      availableSessions: [
        { id: 'FIX.4.4:SENDER1:RECEIVER1' },
        { id: 'FIX.4.4:SENDER2:RECEIVER2' },
        { id: 'FIX.4.4:SENDER3:RECEIVER3' }
      ]
    };
  },
  
  created() {
    // 初始化时加载证书列表
    this.loadCertificates();
  },
  
  methods: {
    // 加载证书列表
    loadCertificates() {
      // 加载客户端证书
      this.$api.get('/api/certificates/client')
        .then(response => {
          this.clientCertificates = response.data;
        })
        .catch(error => {
          this.$message.error('加载客户端证书失败: ' + error.message);
          console.error(error);
        });
      
      // 加载服务器证书
      this.$api.get('/api/certificates/server')
        .then(response => {
          this.serverCertificates = response.data;
        })
        .catch(error => {
          this.$message.error('加载服务器证书失败: ' + error.message);
          console.error(error);
        });
    },
    
    // 处理选项卡切换
    handleTabClick(tab) {
      this.activeTab = tab.name;
    },
    
    // 获取标签类型
    getTagType(status) {
      switch (status) {
        case 'active':
          return 'success';
        case 'expired':
          return 'warning';
        case 'revoked':
          return 'danger';
        default:
          return 'info';
      }
    },
    
    // 获取状态文本
    getStatusText(status) {
      switch (status) {
        case 'active':
          return '活跃';
        case 'expired':
          return '已过期';
        case 'revoked':
          return '已吊销';
        default:
          return status;
      }
    },
    
    // 处理查看证书
    handleViewCertificate(certificate) {
      this.selectedCertificate = certificate;
      this.certificateDialogVisible = true;
    },
    
    // 处理下载证书
    handleDownloadCertificate(certificate) {
      // 这里实际应该调用后端API下载证书文件
      // 为简化示例，我们仅显示一个消息
      this.$message.success('证书下载功能已触发');
      console.log('Downloading certificate:', certificate);
    },
    
    // 处理吊销证书
    handleRevokeCertificate(certificate) {
      this.selectedCertificate = certificate;
      this.revokeDialogVisible = true;
    },
    
    // 处理确认吊销
    handleConfirmRevoke() {
      this.$api.post(`/api/certificates/${this.selectedCertificate.id}/revoke`)
        .then(() => {
          this.$message.success('证书吊销成功');
          this.revokeDialogVisible = false;
          this.loadCertificates();
        })
        .catch(error => {
          this.$message.error('证书吊销失败: ' + error.message);
          console.error(error);
        });
    },
    
    // 处理生成证书
    handleGenerateCertificate() {
      this.generateForm = {
        certType: 'client',
        sessionId: '',
        commonName: '',
        organization: '',
        organizationalUnit: '',
        locality: '',
        state: '',
        country: '',
        keySize: '4096',
        validityDays: 365
      };
      this.generateDialogVisible = true;
    },
    
    // 处理生成证书
    handleGenerate() {
      this.$refs.generateForm.validate(valid => {
        if (valid) {
          this.$api.post('/api/certificates/generate', this.generateForm)
            .then(response => {
              this.$message.success('证书生成成功');
              this.generateDialogVisible = false;
              this.loadCertificates();
              
              // 显示生成的证书详情
              this.selectedCertificate = response.data;
              this.certificateDialogVisible = true;
            })
            .catch(error => {
              this.$message.error('证书生成失败: ' + error.message);
              console.error(error);
            });
        }
      });
    },
    
    // 处理导入证书
    handleImportCertificate() {
      this.importForm = {
        certType: 'client',
        sessionId: '',
        certFile: null,
        certFileName: '',
        keyFile: null,
        keyFileName: ''
      };
      this.importDialogVisible = true;
    },
    
    // 证书上传前处理
    beforeCertUpload(file) {
      this.importForm.certFile = file;
      this.importForm.certFileName = file.name;
      return false; // 阻止默认上传行为
    },
    
    // 私钥上传前处理
    beforeKeyUpload(file) {
      this.importForm.keyFile = file;
      this.importForm.keyFileName = file.name;
      return false; // 阻止默认上传行为
    },
    
    // 处理导入证书
    handleImport() {
      this.$refs.importForm.validate(valid => {
        if (valid) {
          const formData = new FormData();
          formData.append('certType', this.importForm.certType);
          if (this.importForm.sessionId) {
            formData.append('sessionId', this.importForm.sessionId);
          }
          formData.append('certFile', this.importForm.certFile);
          formData.append('keyFile', this.importForm.keyFile);
          
          this.$api.post('/api/certificates/import', formData, {
            headers: {
              'Content-Type': 'multipart/form-data'
            }
          })
            .then(() => {
              this.$message.success('证书导入成功');
              this.importDialogVisible = false;
              this.loadCertificates();
            })
            .catch(error => {
              this.$message.error('证书导入失败: ' + error.message);
              console.error(error);
            });
        }
      });
    }
  }
};
</script>

<style scoped>
.certificates {
  padding: 20px;
}

.btn-group {
  float: right;
}

.btn-group button {
  margin-left: 10px;
}

.certificate-details {
  max-height: 600px;
  overflow-y: auto;
}

.certificate-info {
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  margin-bottom: 10px;
}

.info-item .label {
  width: 150px;
  font-weight: bold;
}

.info-item .value {
  flex: 1;
}

.certificate-content pre {
  max-height: 300px;
  overflow-y: auto;
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
}

.warning-text {
  color: #f56c6c;
  margin-top: 10px;
}

.file-name {
  margin-top: 10px;
  color: #606266;
  font-size: 12px;
}
</style>
    