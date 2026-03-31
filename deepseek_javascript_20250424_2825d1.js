const express = require('express');
const { Worker } = require('worker_threads');
const { v4: uuidv4 } = require('uuid');
const Joi = require('joi');
const app = express();

// 内存存储
const testStore = new Map();

// 请求体验证中间件
const validateConfig = (req, res, next) => {
  const schema = Joi.object({
    threadGroups: Joi.array().items(
      Joi.object({
        name: Joi.string().required(),
        threads: Joi.number().min(1).max(1000).required(),
        iterations: Joi.number().min(1).default(1),
        rampUp: Joi.number().min(0).default(0),
        requests: Joi.array().items(
          Joi.object({
            method: Joi.string().valid('GET', 'POST', 'PUT', 'DELETE').required(),
            url: Joi.string().uri().required(),
            headers: Joi.object(),
            body: Joi.any(),
            timeout: Joi.number().min(100).default(5000)
          })
        ).min(1).required()
      })
    ).min(1).required()
  });

  const { error } = schema.validate(req.body);
  if (error) return res.status(400).json({ error: error.details[0].message });
  next();
};

// API 端点
app.post('/tests', validateConfig, (req, res) => {
  const testId = uuidv4();
  const config = {
    id: testId,
    status: 'created',
    ...req.body,
    createdAt: new Date(),
    stats: {
      total: 0,
      success: 0,
      errors: 0,
      responseTimes: [],
      errorDetails: {}
    }
  };

  testStore.set(testId, config);
  startTestWorker(testId);

  res.status(201).json({
    id: testId,
    statusUrl: `/tests/${testId}/status`,
    resultsUrl: `/tests/${testId}/results`
  });
});

app.get('/tests/:id/status', (req, res) => {
  const test = testStore.get(req.params.id);
  if (!test) return res.status(404).json({ error: 'Test not found' });
  
  res.json({
    status: test.status,
    progress: calculateProgress(test),
    startedAt: test.startedAt,
    completedAt: test.completedAt
  });
});

app.get('/tests/:id/results', (req, res) => {
  const test = testStore.get(req.params.id);
  if (!test) return res.status(404).json({ error: 'Test not found' });

  res.json({
    config: test.threadGroups,
    summary: generateSummary(test.stats),
    details: test.stats
  });
});

// 启动测试Worker
function startTestWorker(testId) {
  const test = testStore.get(testId);
  test.status = 'running';
  test.startedAt = new Date();

  const worker = new Worker('./test-worker.js', {
    workerData: { testId, config: test }
  });

  worker.on('message', (msg) => {
    const test = testStore.get(testId);
    updateStats(test.stats, msg);
  });

  worker.on('exit', (code) => {
    const test = testStore.get(testId);
    test.status = code === 0 ? 'completed' : 'failed';
    test.completedAt = new Date();
  });
}

function updateStats(stats, data) {
  stats.total += data.total;
  stats.success += data.success;
  stats.errors += data.errors;
  stats.responseTimes.push(...data.responseTimes);
  
  Object.entries(data.errors).forEach(([code, count]) => {
    stats.errorDetails[code] = (stats.errorDetails[code] || 0) + count;
  });
}

function calculateProgress(test) {
  const totalRequests = test.threadGroups.reduce((sum, group) => 
    sum + group.threads * group.iterations * group.requests.length, 0);
  return (test.stats.total / totalRequests * 100).toFixed(1);
}

function generateSummary(stats) {
  const sortedTimes = [...stats.responseTimes].sort((a, b) => a - b);
  return {
    totalRequests: stats.total,
    successRate: `${(stats.success / stats.total * 100).toFixed(2)}%`,
    avgResponseTime: `${average(sortedTimes).toFixed(2)}ms`,
    percentiles: {
      p50: percentile(sortedTimes, 50),
      p90: percentile(sortedTimes, 90),
      p99: percentile(sortedTimes, 99)
    },
    errorDistribution: stats.errorDetails
  };
}

app.listen(3000, () => console.log('API Server running on port 3000'));