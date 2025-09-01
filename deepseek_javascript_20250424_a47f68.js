const { parentPort, workerData } = require('worker_threads');
const axios = require('axios');
const http = require('http');

class ThreadGroupExecutor {
  constructor(config) {
    this.config = config;
    this.agent = new http.Agent({
      keepAlive: true,
      maxSockets: 1000
    });
  }

  async execute() {
    for (const group of this.config.threadGroups) {
      await this.runThreadGroup(group);
    }
  }

  async runThreadGroup(group) {
    const threads = [];
    for (let i = 0; i < group.threads; i++) {
      threads.push(this.createThread(i, group));
    }
    await Promise.all(threads);
  }

  async createThread(threadId, group) {
    for (let iter = 0; iter < group.iterations; iter++) {
      for (const reqConfig of group.requests) {
        await this.executeRequest(reqConfig);
      }
    }
  }

  async executeRequest(config) {
    const start = Date.now();
    try {
      const response = await axios({
        method: config.method,
        url: config.url,
        headers: config.headers,
        data: config.body,
        httpAgent: this.agent,
        timeout: config.timeout
      });
      
      parentPort.postMessage({
        total: 1,
        success: 1,
        errors: 0,
        responseTimes: [Date.now() - start],
        errors: {}
      });
    } catch (error) {
      const statusCode = error.response?.status || 'NETWORK_ERROR';
      parentPort.postMessage({
        total: 1,
        success: 0,
        errors: 1,
        responseTimes: [],
        errors: { [statusCode]: 1 }
      });
    }
  }
}

// 启动测试
new ThreadGroupExecutor(workerData.config).execute()
  .then(() => parentPort.postMessage('done'))
  .catch(err => console.error(err));