const http = require('http');
const https = require('https');

const createAgent = (max = 1000) => ({
  http: new http.Agent({
    keepAlive: true,
    maxSockets: max,
    timeout: 30000
  }),
  https: new https.Agent({
    keepAlive: true,
    maxSockets: max,
    rejectUnauthorized: false
  })
});

// 在Worker初始化时使用
this.agents = createAgent(1000);