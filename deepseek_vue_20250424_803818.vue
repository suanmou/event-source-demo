<script>
export default {
  mounted() {
    this.initCharts();
  },
  
  methods: {
    initCharts() {
      this.rpsChart = echarts.init(this.$refs.rpsChart);
      this.rpsChart.setOption({
        title: { text: '吞吐量 (RPS)' },
        xAxis: { type: 'time' },
        yAxis: { type: 'value' },
        series: [{
          type: 'line',
          smooth: true
        }]
      });

      this.latencyChart = echarts.init(this.$refs.latencyChart);
      // 类似初始化...
    }
  }
}
</script>