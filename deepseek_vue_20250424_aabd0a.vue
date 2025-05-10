<script>
import axios from 'axios';
import * as echarts from 'echarts';

export default {
  data() {
    return {
      config: {
        threadGroups: [{
          name: '默认线程组',
          threads: 10,
          requests: [{
            method: 'GET',
            url: 'http://api.example.com',
            timeout: 5000
          }]
        }]
      },
      activeTestId: null,
      testStatus: 'ready',
      summary: {},
      testDetails: [],
      activeTab: 'summary',
      rpsChart: null,
      latencyChart: null
    }
  },
  methods: {
    addThreadGroup() {
      this.config.threadGroups.push({
        name: `线程组 ${this.config.threadGroups.length + 1}`,
        threads: 10,
        requests: []
      });
    },
    
    addRequest(groupIndex) {
      this.config.threadGroups[groupIndex].requests.push({
        method: 'GET',
        url: '',
        timeout: 5000
      });
    },
    
    async startTest() {
      try {
        const res = await axios.post('/tests', this.config);
        this.activeTestId = res.data.id;
        this.startMonitoring();
        this.$message.success('测试已启动');
      } catch (error) {
        this.$message.error('启动测试失败');
      }
    },
    
    startMonitoring() {
      this.statusTimer = setInterval(async () => {
        const status = await this.getTestStatus();
        this.updateCharts(status);
        
        if (status.progress >= 100) {
          clearInterval(this.statusTimer);
          this.loadTestResults();
        }
      }, 2000);
    },
    
    updateCharts(status) {
      // 更新ECharts数据
      this.rpsChart.setOption({
        series: [{
          data: status.rpsHistory
        }]
      });
    }
  }
}
</script>