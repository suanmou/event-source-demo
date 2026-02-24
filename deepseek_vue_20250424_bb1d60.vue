<template>
  <div class="app-container">
    <!-- 配置表单 -->
    <el-card class="config-form">
      <div slot="header">
        <span>压力测试配置</span>
        <el-button @click="addThreadGroup" style="float: right">添加线程组</el-button>
      </div>
      <el-form ref="form" :model="config" label-width="120px">
        <div v-for="(group, gIndex) in config.threadGroups" :key="gIndex">
          <el-divider>线程组 {{ gIndex + 1 }}</el-divider>
          <el-form-item label="组名称" required>
            <el-input v-model="group.name"></el-input>
          </el-form-item>
          
          <el-form-item label="线程数">
            <el-input-number 
              v-model="group.threads" 
              :min="1" 
              :max="1000">
            </el-input-number>
          </el-form-item>

          <el-form-item label="请求配置">
            <el-button @click="addRequest(gIndex)">添加请求</el-button>
            <div v-for="(req, rIndex) in group.requests" :key="rIndex">
              <el-card class="request-card">
                <el-form-item label="URL" required>
                  <el-input v-model="req.url"></el-input>
                </el-form-item>
                <!-- 其他请求字段 -->
              </el-card>
            </div>
          </el-form-item>
        </div>
      </el-form>

      <el-button type="primary" @click="startTest">启动测试</el-button>
      <el-button @click="stopTest">停止测试</el-button>
    </el-card>

    <!-- 实时监控 -->
    <el-card class="monitor">
      <div slot="header">实时监控</div>
      <div class="charts">
        <div ref="rpsChart" class="chart"></div>
        <div ref="latencyChart" class="chart"></div>
      </div>
    </el-card>

    <!-- 测试报告 -->
    <el-card class="report">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="摘要" name="summary">
          <el-table :data="[summary]">
            <!-- 摘要表格 -->
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="详细数据" name="details">
          <el-table
            :data="testDetails"
            height="400"
            style="width: 100%">
            <!-- 详细数据表格 -->
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>