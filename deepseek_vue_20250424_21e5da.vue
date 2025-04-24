<script>
export default {
  methods: {
    async getTestStatus() {
      const res = await axios.get(`/tests/${this.activeTestId}/status`);
      return res.data;
    },
    
    async loadTestResults() {
      const res = await axios.get(`/tests/${this.activeTestId}/results`);
      this.summary = res.data.summary;
      this.testDetails = res.data.details.responseTimes.map((t, i) => ({
        id: i + 1,
        duration: t,
        timestamp: new Date().toLocaleString()
      }));
    }
  }
}
</script>