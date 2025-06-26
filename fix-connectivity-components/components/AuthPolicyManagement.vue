<template>
  <div class="auth-policy-container">
    <div class="auth-policy-header">
      <h3 class="auth-policy-title">认证策略管理</h3>
      <div class="auth-policy-actions">
        <el-button type="primary" @click="handleAddPolicy">
          <i class="el-icon-plus"></i> 添加策略
        </el-button>
        <el-button @click="handleRefresh">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </div>
    </div>
    
    <div class="auth-policy-tabs">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="用户认证策略" name="user"></el-tab-pane>
        <el-tab-pane label="IP 认证策略" name="ip"></el-tab-pane>
        <el-tab-pane label="证书认证策略" name="certificate"></el-tab-pane>
        <el-tab-pane label="多因素认证策略" name="mfa"></el-tab-pane>
      </el-tabs>
    </div>
    
    <div class="auth-policy-content">
      <div v-if="activeTab === 'user'">
        <div class="policy-filter">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-select v-model="filter.status" placeholder="状态">
                <el-option label="全部" value=""></el-option>
                <el-option label="启用" value="enabled"></el-option>
                <el-option label="禁用" value="disabled"></el-option>
              </el-select>
            </el-col>
            <el-col :span="18">
              <el-input v-model="filter.keyword" placeholder="搜索策略名称或描述" suffix-icon="el-icon-search"></el-input>
            </el-col>
          </el-row>
        </div>
        
        <el-table
          :data="filteredUserPolicies"
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
              <StatusIndicator :status="scope.row.status === 'enabled' ? 'active' : 'inactive'" size="small" />
            </template>
          </el-table-column>
          <el-table-column
            label="策略名称"
            prop="name"
            sortable
            min-width="150"
          ></el-table-column>
          <el-table-column
            label="认证方式"
            prop="authMethod"
            width="120"
            sortable
          >
            <template #default="scope">
              {{ getAuthMethodText(scope.row.authMethod) }}
            </template>
          </el-table-column>
          <el-table-column
            label="密码复杂度"
            width="120"
          >
            <template #default="scope">
              {{ getPasswordComplexityText(scope.row.passwordComplexity) }}
            </template>
          </el-table-column>
          <el-table-column
            label="会话超时(分钟)"
            prop="sessionTimeout"
            width="150"
            sortable
          ></el-table-column>
          <el-table-column
            label="描述"
            prop="description"
            min-width="200"
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
                @click="handleEditPolicy(scope.row)"
              >
                <i class="el-icon-edit"></i> 编辑
              </el-button>
              <el-button
                size="mini"
                type="danger"
                @click="handleDeletePolicy(scope.row)"
              >
                <i class="el-icon-delete"></i> 删除
              </el-button>
              <el-button
                size="mini"
                type="text"
                :disabled="scope.row.status === 'enabled'"
                @click="handleEnablePolicy(scope.row)"
              >
                <i class="el-icon-check"></i> 启用
              </el-button>
              <el-button
                size="mini"
                type="text"
                :disabled="scope.row.status !== 'enabled'"
                @click="handleDisablePolicy(scope.row)"
              >
                <i class="el-icon-close"></i> 禁用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div v-if="filteredUserPolicies.length === 0" class="policy-empty">
          <img src="https://picsum.photos/id/237/200/150" alt="No policies" class="empty-image">
          <p class="empty-text">暂无用户认证策略</p>
        </div>
      </div>
      
      <div v-if="activeTab === 'ip'">
        <div class="policy-filter">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-select v-model="filter.status" placeholder="状态">
                <el-option label="全部" value=""></el-option>
                <el-option label="启用" value="enabled"></el-option>
                <el-option label="禁用" value="disabled"></el-option>
              </el-select>
            </el-col>
            <el-col :span="18">
              <el-input v-model="filter.keyword" placeholder="搜索策略名称或描述" suffix-icon="el-icon-search"></el-input>
            </el-col>
          </el-row>
        </div>
        
        <el-table
          :data="filteredIpPolicies"
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
              <StatusIndicator :status="scope.row.status === 'enabled' ? 'active' : 'inactive'" size="small" />
            </template>
          </el-table-column>
          <el-table-column
            label="策略名称"
            prop="name"
            sortable
            min-width="150"
          ></el-table-column>
          <el-table-column
            label="策略类型"
            prop="policyType"
            width="120"
            sortable
          >
            <template #default="scope">
              {{ scope.row.policyType === 'whitelist' ? '白名单' : '黑名单' }}
            </template>
          </el-table-column>
          <el-table-column
            label="IP 范围"
            prop="ipRange"
            min-width="200"
          ></el-table-column>
          <el-table-column
            label="应用连接"
            prop="connections"
            min-width="150"
          >
            <template #default="scope">
              <span v-for="(conn, index) in scope.row.connections" :key="index">
                {{ conn }}
                <span v-if="index < scope.row.connections.length - 1">, </span>
              </span>
            </template>
          </el-table-column>
          <el-table-column
            label="描述"
            prop="description"
            min-width="200"
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
                @click="handleEditPolicy(scope.row)"
              >
                <i class="el-icon-edit"></i> 编辑
              </el-button>
              <el-button
                size="mini"
                type="danger"
                @click="handleDeletePolicy(scope.row)"
              >
                <i class="el-icon-delete"></i> 删除
              </el-button>
              <el-button
                size="mini"
                type="text"
                :disabled="scope.row.status === 'enabled'"
                @click="handleEnablePolicy(scope.row)"
              >
                <i class="el-icon-check"></i> 启用
              </el-button>
              <el-button
                size="mini"
                type="text"
                :disabled="scope.row.status !== 'enabled'"
                @click="handleDisablePolicy(scope.row)"
              >
                <i class="el-icon-close"></i> 禁用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div v-if="filteredIpPolicies.length === 0" class="policy-empty">
          <img src="https://picsum.photos/id/237/200/150" alt="No policies" class="empty-image">
          <p class="empty-text">暂无 IP 认证策略</p>
        </div>
      </div>
      
      <div v-if="activeTab === 'certificate'">
        <div class="policy-filter">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-select v-model="filter.status" placeholder="状态">
                <el-option label="全部" value=""></el-option>
                <el-option label="启用" value="enabled"></el-option>
                <el-option label="禁用" value="disabled"></el-option>
              </el-select>
            </el-col>
            <el-col :span="18">
              <el-input v-model="filter.keyword" placeholder="搜索策略名称或描述" suffix-icon="el-icon-search"></el-input>
            </el-col>
          </el-row>
        </div>
        
        <el-table
          :data="filteredCertificatePolicies"
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
              <StatusIndicator :status="scope.row.status === 'enabled' ? 'active' : 'inactive'" size="small" />
            </template>
          </el-table-column>
          <el-table-column
            label="策略名称"
            prop="name"
            sortable
            min-width="150"
          ></el-table-column>
          <el-table-column
            label="证书类型"
            prop="certificateType"
            width="120"
            sortable
          ></el-table-column>
          <el-table-column
            label="验证级别"
            prop="verificationLevel"
            width="120"
            sortable
          >
            <template #default="scope">
              {{ getVerificationLevelText(scope.row.verificationLevel) }}
            </template>
          </el-table-column>
          <el-table-column
            label="有效期要求(天)"
            prop="minValidityDays"
            width="150"
            sortable
          ></el-table-column>
          <el-table-column
            label="应用连接"
            prop="connections"
            min-width="150"
          >
            <template #default="scope">
              <span v-for="(conn, index) in scope.row.connections" :key="index">
                {{ conn }}
                <span v-if="index < scope.row.connections.length - 1">, </span>
              </span>
            </template>
          </el-table-column>
          <el-table-column
            label="描述"
            prop="description"
            min-width="200"
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
                @click="handleEditPolicy(scope.row)"
              >
                <i class="el-icon-edit"></i> 编辑
              </el-button>
              <el-button
                size="mini"
                type="danger"
                @click="handleDeletePolicy(scope.row)"
              >
                <i class="el-icon-delete"></i> 删除
              </el-button>
              <el-button
                size="mini"
                type="text"
                :disabled="scope.row.status === 'enabled'"
                @click="handleEnablePolicy(scope.row)"
              >
                <i class="el-icon-check"></i> 启用
              </el-button>
              <el-button
                size="mini"
                type="text"
                :disabled="scope.row.status !== 'enabled'"
                @click="handleDisablePolicy(scope.row)"
              >
                <i class="el-icon-close"></i> 禁用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div v-if="filteredCertificatePolicies.length === 0" class="policy-empty">
          <img src="https://picsum.photos/id/237/200/150" alt="No policies" class="empty-image">
          <p class="empty-text">暂无证书认证策略</p>
        </div>
      </div>
      
      <div v-if="activeTab === 'mfa'">
        <div class="policy-filter">
          <el-row :gutter="10">
            <el-col :span="6">
              <el-select v-model="filter.status" placeholder="状态">
                <el-option label="全部" value=""></el-option>
                <el-option label="启用" value="enabled"></el-option>
                <el-option label="禁用" value="disabled"></el-option>
              </el-select>
            </el-col>
            <el-col :span="18">
              <el-input v-model="filter.keyword" placeholder="搜索策略名称或描述" suffix-icon="el-icon-search"></el-input>
            </el-col>
          </el-row>
        </div>
        
        <el-table
          :data="filteredMfaPolicies"
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
              <StatusIndicator :status="scope.row.status === 'enabled' ? 'active' : 'inactive'" size="small" />
            </template>
          </el-table-column>
          <el-table-column
            label="策略名称"
            prop="name"
            sortable
            min-width="150"
          ></el-table-column>
          <el-table-column
            label="MFA 类型"
            prop="mfaType"
            width="120"
            sortable
          >
            <template #default="scope">
              {{ getMfaTypeText(scope.row.mfaType) }}
            </template>
          </el-table-column>
          <el-table-column
            label="应用场景"
            prop="scenarios"
            min-width="150"
          >
            <template #default="scope">
              <span v-for="(scenario, index) in scope.row.scenarios" :key="index">
                {{ getScenarioText(scenario) }}
                <span v-if="index < scope.row.scenarios.length - 1">, </span>
              </span>
            </template>
          </el-table-column>
          <el-table-column
            label="有效期(分钟)"
            prop="tokenValidityMinutes"
            width="150"
            sortable
          ></el-table-column>
          <el-table-column
            label="失败锁定次数"
            prop="maxFailedAttempts"
            width="150"
            sortable
          ></el-table-column>
          <el-table-column
            label="描述"
            prop="description"
            min-width="200"
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
                @click="handleEditPolicy(scope.row)"
              >
                <i class="el-icon-edit"></i> 编辑
              </el-button>
              <el-button
                size="mini"
                type="danger"
                @click="handleDeletePolicy(scope.row)"
              >
                <i class="el-icon-delete"></i> 删除
              </el-button>
              <el-button
                size="mini"
                type="text"
                :disabled="scope.row.status === 'enabled'"
                @click="handleEnablePolicy(scope.row)"
              >
                <i class="el-icon-check"></i> 启用
              </el-button>
              <el-button
                size="mini"
                type="text"
                :disabled="scope.row.status !== 'enabled'"
                @click="handleDisablePolicy(scope.row)"
              >
                <i class="el-icon-close"></i> 禁用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div v-if="filteredMfaPolicies.length === 0" class="policy-empty">
          <img src="https://picsum.photos/id/237/200/150" alt="No policies" class="empty-image">
          <p class="empty-text">暂无多因素认证策略</p>
        </div>
      </div>
    </div>
    
    <div class="auth-policy-footer">
      <Pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="getCurrentFilteredPolicies().length"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    
    <el-dialog
      title="添加认证策略"
      :visible.sync="addDialogVisible"
      width="600px"
    >
      <template #content>
        <el-form :model="policyForm" :rules="policyRules" ref="policyFormRef" label-width="120px">
          <el-form-item label="策略名称" prop="name">
            <el-input v-model="policyForm.name"></el-input>
          </el-form-item>
          <el-form-item label="策略类型" prop="policyType">
            <el-select v-model="policyForm.policyType" placeholder="请选择策略类型">
              <el-option label="用户认证" value="user"></el-option>
              <el-option label="IP 认证" value="ip"></el-option>
              <el-option label="证书认证" value="certificate"></el-option>
              <el-option label="多因素认证" value="mfa"></el-option>
            </el-select>
          </el-form-item>
          
          <div v-if="policyForm.policyType === 'user'">
            <el-form-item label="认证方式" prop="authMethod">
              <el-select v-model="policyForm.authMethod" placeholder="请选择认证方式">
                <el-option label="用户名密码" value="password"></el-option>
                <el-option label="LDAP" value="ldap"></el-option>
                <el-option label="SAML" value="saml"></el-option>
                <el-option label="OAuth2" value="oauth2"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="密码复杂度" prop="passwordComplexity">
              <el-select v-model="policyForm.passwordComplexity" placeholder="请选择密码复杂度">
                <el-option label="简单" value="simple"></el-option>
                <el-option label="中等" value="medium"></el-option>
                <el-option label="复杂" value="complex"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="会话超时(分钟)" prop="sessionTimeout">
              <el-input v-model="policyForm.sessionTimeout" type="number"></el-input>
            </el-form-item>
          </div>
          
          <div v-if="policyForm.policyType === 'ip'">
            <el-form-item label="策略类型" prop="ipPolicyType">
              <el-select v-model="policyForm.ipPolicyType" placeholder="请选择 IP 策略类型">
                <el-option label="白名单" value="whitelist"></el-option>
                <el-option label="黑名单" value="blacklist"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="IP 范围" prop="ipRange">
              <el-input v-model="policyForm.ipRange" placeholder="例如: 192.168.1.0/24, 10.0.0.1"></el-input>
            </el-form-item>
            <el-form-item label="应用连接" prop="connections">
              <el-select v-model="policyForm.connections" multiple placeholder="请选择应用连接">
                <el-option v-for="connection in connections" :key="connection.id" :label="connection.name" :value="connection.id"></el-option>
              </el-select>
            </el-form-item>
          </div>
          
          <div v-if="policyForm.policyType === 'certificate'">
            <el-form-item label="证书类型" prop="certificateType">
              <el-select v-model="policyForm.certificateType" placeholder="请选择证书类型">
                <el-option label="RSA" value="rsa"></el-option>
                <el-option label="ECDSA" value="ecdsa"></el-option>
                <el-option label="DSA" value="dsa"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="验证级别" prop="verificationLevel">
              <el-select v-model="policyForm.verificationLevel" placeholder="请选择验证级别">
                <el-option label="基本验证" value="basic"></el-option>
                <el-option label="中级验证" value="intermediate"></el-option>
                <el-option label="高级验证" value="advanced"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="有效期要求(天)" prop="minValidityDays">
              <el-input v-model="policyForm.minValidityDays" type="number"></el-input>
            </el-form-item>
            <el-form-item label="应用连接" prop="connections">
              <el-select v-model="policyForm.connections" multiple placeholder="请选择应用连接">
                <el-option v-for="connection in connections" :key="connection.id" :label="connection.name" :value="connection.id"></el-option>
              </el-select>
            </el-form-item>
          </div>
          
          <div v-if="policyForm.policyType === 'mfa'">
            <el-form-item label="MFA 类型" prop="mfaType">
              <el-select v-model="policyForm.mfaType" placeholder="请选择 MFA 类型">
                <el-option label="短信验证码" value="sms"></el-option>
                <el-option label="认证器应用" value="authenticator"></el-option>
                <el-option label="硬件令牌" value="hardware"></el-option>
                <el-option label="生物识别" value="biometric"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="应用场景" prop="scenarios">
              <el-select v-model="policyForm.scenarios" multiple placeholder="请选择应用场景">
                <el-option label="登录" value="login"></el-option>
                <el-option label="敏感操作" value="sensitive"></el-option>
                <el-option label="所有操作" value="all"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="有效期(分钟)" prop="tokenValidityMinutes">
              <el-input v-model="policyForm.tokenValidityMinutes" type="number"></el-input>
            </el-form-item>
            <el-form-item label="失败锁定次数" prop="maxFailedAttempts">
              <el-input v-model="policyForm.maxFailedAttempts" type="number"></el-input>
            </el-form-item>
          </div>
          
          <el-form-item label="状态" prop="status">
            <el-select v-model="policyForm.status" placeholder="请选择状态">
              <el-option label="启用" value="enabled"></el-option>
              <el-option label="禁用" value="disabled"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="policyForm.description"></el-input>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePolicy">提交</el-button>
      </template>
    </el-dialog>
    
    <el-dialog
      title="编辑认证策略"
      :visible.sync="editDialogVisible"
      width="600px"
    >
      <template #content>
        <el-form :model="editPolicyForm" :rules="policyRules" ref="editPolicyFormRef" label-width="120px">
          <el-form-item label="策略名称" prop="name">
            <el-input v-model="editPolicyForm.name"></el-input>
          </el-form-item>
          <el-form-item label="策略类型" prop="policyType">
            <el-input v-model="editPolicyForm.policyType" disabled></el-input>
          </el-form-item>
          
          <div v-if="editPolicyForm.policyType === 'user'">
            <el-form-item label="认证方式" prop="authMethod">
              <el-select v-model="editPolicyForm.authMethod" placeholder="请选择认证方式">
                <el-option label="用户名密码" value="password"></el-option>
                <el-option label="LDAP" value="ldap"></el-option>
                <el-option label="SAML" value="saml"></el-option>
                <el-option label="OAuth2" value="oauth2"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="密码复杂度" prop="passwordComplexity">
              <el-select v-model="editPolicyForm.passwordComplexity" placeholder="请选择密码复杂度">
                <el-option label="简单" value="simple"></el-option>
                <el-option label="中等" value="medium"></el-option>
                <el-option label="复杂" value="complex"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="会话超时(分钟)" prop="sessionTimeout">
              <el-input v-model="editPolicyForm.sessionTimeout" type="number"></el-input>
            </el-form-item>
          </div>
          
          <div v-if="editPolicyForm.policyType === 'ip'">
            <el-form-item label="策略类型" prop="ipPolicyType">
              <el-select v-model="editPolicyForm.ipPolicyType" placeholder="请选择 IP 策略类型">
                <el-option label="白名单" value="whitelist"></el-option>
                <el-option label="黑名单" value="blacklist"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="IP 范围" prop="ipRange">
              <el-input v-model="editPolicyForm.ipRange" placeholder="例如: 192.168.1.0/24, 10.0.0.1"></el-input>
            </el-form-item>
            <el-form-item label="应用连接" prop="connections">
              <el-select v-model="editPolicyForm.connections" multiple placeholder="请选择应用连接">
                <el-option v-for="connection in connections" :key="connection.id" :label="connection.name" :value="connection.id"></el-option>
              </el-select>
            </el-form-item>
          </div>
          
          <div v-if="editPolicyForm.policyType === 'certificate'">
            <el-form-item label="证书类型" prop="certificateType">
              <el-select v-model="editPolicyForm.certificateType" placeholder="请选择证书类型">
                <el-option label="RSA" value="rsa"></el-option>
                <el-option label="ECDSA" value="ecdsa"></el-option>
                <el-option label="DSA" value="dsa"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="验证级别" prop="verificationLevel">
              <el-select v-model="editPolicyForm.verificationLevel" placeholder="请选择验证级别">
                <el-option label="基本验证" value="basic"></el-option>
                <el-option label="中级验证" value="intermediate"></el-option>
                <el-option label="高级验证" value="advanced"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="有效期要求(天)" prop="minValidityDays">
              <el-input v-model="editPolicyForm.minValidityDays" type="number"></el-input>
            </el-form-item>
            <el-form-item label="应用连接" prop="connections">
              <el-select v-model="editPolicyForm.connections" multiple placeholder="请选择应用连接">
                <el-option v-for="connection in connections" :key="connection.id" :label="connection.name" :value="connection.id"></el-option>
              </el-select>
            </el-form-item>
          </div>
          
          <div v-if="editPolicyForm.policyType === 'mfa'">
            <el-form-item label="MFA 类型" prop="mfaType">
              <el-select v-model="editPolicyForm.mfaType" placeholder="请选择 MFA 类型">
                <el-option label="短信验证码" value="sms"></el-option>
                <el-option label="认证器应用" value="authenticator"></el-option>
                <el-option label="硬件令牌" value="hardware"></el-option>
                <el-option label="生物识别" value="biometric"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="应用场景" prop="scenarios">
              <el-select v-model="editPolicyForm.scenarios" multiple placeholder="请选择应用场景">
                <el-option label="登录" value="login"></el-option>
                <el-option label="敏感操作" value="sensitive"></el-option>
                <el-option label="所有操作" value="all"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="有效期(分钟)" prop="tokenValidityMinutes">
              <el-input v-model="editPolicyForm.tokenValidityMinutes" type="number"></el-input>
            </el-form-item>
            <el-form-item label="失败锁定次数" prop="maxFailedAttempts">
              <el-input v-model="editPolicyForm.maxFailedAttempts" type="number"></el-input>
            </el-form-item>
          </div>
          
          <el-form-item label="状态" prop="status">
            <el-select v-model="editPolicyForm.status" placeholder="请选择状态">
              <el-option label="启用" value="enabled"></el-option>
              <el-option label="禁用" value="disabled"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="editPolicyForm.description"></el-input>
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePolicy">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import StatusIndicator from './StatusIndicator.vue';
import Pagination from './Pagination.vue';

export default {
  name: 'AuthPolicyManagement',
  components: {
    StatusIndicator,
    Pagination
  },
  props: {
    userPolicies: {
      type: Array,
      default: () => []
    },
    ipPolicies: {
      type: Array,
      default: () => []
    },
    certificatePolicies: {
      type: Array,
      default: () => []
    },
    mfaPolicies: {
      type: Array,
      default: () => []
    },
    connections: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      activeTab: 'user',
      filter: {
        status: '',
        keyword: ''
      },
      currentPage: 1,
      pageSize: 10,
      sort: {
        prop: 'updatedAt',
        order: 'descending'
      },
      addDialogVisible: false,
      editDialogVisible: false,
      policyForm: {
        name: '',
        policyType: 'user',
        authMethod: 'password',
        passwordComplexity: 'medium',
        sessionTimeout: 30,
        ipPolicyType: 'whitelist',
        ipRange: '',
        connections: [],
        certificateType: 'rsa',
        verificationLevel: 'intermediate',
        minValidityDays: 90,
        mfaType: 'authenticator',
        scenarios: ['login'],
        tokenValidityMinutes: 5,
        maxFailedAttempts: 3,
        status: 'enabled',
        description: ''
      },
      editPolicyForm: {
        id: null,
        name: '',
        policyType: 'user',
        authMethod: 'password',
        passwordComplexity: 'medium',
        sessionTimeout: 30,
        ipPolicyType: 'whitelist',
        ipRange: '',
        connections: [],
        certificateType: 'rsa',
        verificationLevel: 'intermediate',
        minValidityDays: 90,
        mfaType: 'authenticator',
        scenarios: ['login'],
        tokenValidityMinutes: 5,
        maxFailedAttempts: 3,
        status: 'enabled',
        description: ''
      },
      policyRules: {
        name: [
          { required: true, message: '请输入策略名称', trigger: 'blur' },
          { min: 2, max: 50, message: '策略名称长度在 2 到 50 个字符之间', trigger: 'blur' }
        ],
        policyType: [
          { required: true, message: '请选择策略类型', trigger: 'change' }
        ],
        authMethod: [
          { required: true, message: '请选择认证方式', trigger: 'change' }
        ],
        passwordComplexity: [
          { required: true, message: '请选择密码复杂度', trigger: 'change' }
        ],
        sessionTimeout: [
          { required: true, message: '请输入会话超时时间', trigger: 'blur' },
          { type: 'number', message: '会话超时时间必须为数字', trigger: 'blur' }
        ],
        ipPolicyType: [
          { required: true, message: '请选择 IP 策略类型', trigger: 'change' }
        ],
        ipRange: [
          { required: true, message: '请输入 IP 范围', trigger: 'blur' }
        ],
        certificateType: [
          { required: true, message: '请选择证书类型', trigger: 'change' }
        ],
        verificationLevel: [
          { required: true, message: '请选择验证级别', trigger: 'change' }
        ],
        minValidityDays: [
          { required: true, message: '请输入有效期要求', trigger: 'blur' },
          { type: 'number', message: '有效期要求必须为数字', trigger: 'blur' }
        ],
        mfaType: [
          { required: true, message: '请选择 MFA 类型', trigger: 'change' }
        ],
        scenarios: [
          { required: true, message: '请选择应用场景', trigger: 'change' }
        ],
        tokenValidityMinutes: [
          { required: true, message: '请输入有效期', trigger: 'blur' },
          { type: 'number', message: '有效期必须为数字', trigger: 'blur' }
        ],
        maxFailedAttempts: [
          { required: true, message: '请输入失败锁定次数', trigger: 'blur' },
          { type: 'number', message: '失败锁定次数必须为数字', trigger: 'blur' }
        ],
        status: [
          { required: true, message: '请选择状态', trigger: 'change' }
        ]
      }
    };
  },
  computed: {
    filteredUserPolicies() {
      let result = [...this.userPolicies];
      
      // 应用筛选条件
      if (this.filter.status) {
        result = result.filter(policy => policy.status === this.filter.status);
      }
      
      if (this.filter.keyword) {
        const keyword = this.filter.keyword.toLowerCase();
        result = result.filter(policy => 
          policy.name.toLowerCase().includes(keyword) || 
          policy.description.toLowerCase().includes(keyword)
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
    filteredIpPolicies() {
      let result = [...this.ipPolicies];
      
      // 应用筛选条件
      if (this.filter.status) {
        result = result.filter(policy => policy.status === this.filter.status);
      }
      
      if (this.filter.keyword) {
        const keyword = this.filter.keyword.toLowerCase();
        result = result.filter(policy => 
          policy.name.toLowerCase().includes(keyword) || 
          policy.description.toLowerCase().includes(keyword) ||
          policy.ipRange.toLowerCase().includes(keyword)
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
    filteredCertificatePolicies() {
      let result = [...this.certificatePolicies];
      
      // 应用筛选条件
      if (this.filter.status) {
        result = result.filter(policy => policy.status === this.filter.status);
      }
      
      if (this.filter.keyword) {
