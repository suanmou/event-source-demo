<script>
import { exportJsonToExcel } from '@/vendor/Export2Excel';

export default {
  methods: {
    exportReport() {
      const headers = ['时间戳', '响应时间', '状态码'];
      const data = this.testDetails.map(d => [
        d.timestamp,
        d.duration,
        d.status
      ]);
      
      exportJsonToExcel({
        header: headers,
        data,
        filename: '压测报告'
      });
    }
  }
}
</script>