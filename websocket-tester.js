class WebSocketTester {
  constructor(url) {
    this.url = url;
    this.ws = null;
    this.testSteps = [];
    this.currentStepIndex = 0;
    this.receivedMessages = [];
    this.testResults = [];
    this.timeoutId = null;
  }

  // 添加测试步骤
  addStep(step) {
    this.testSteps.push(step);
    return this;
  }

  // 运行测试
  async run() {
    console.log('开始WebSocket测试...');
    this.testResults = [];
    this.currentStepIndex = 0;
    this.receivedMessages = [];

    await this.connect();
    await this.executeNextStep();
  }

  // 连接WebSocket
  connect() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        console.log('WebSocket连接已建立');
        resolve();
      };

      this.ws.onmessage = (event) => {
        console.log('收到消息:', event.data);
        this.receivedMessages.push(event.data);
        this.checkCurrentStep();
      };

      this.ws.onclose = (event) => {
        console.log('WebSocket连接已关闭', event);
        this.checkCurrentStep();
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket错误:', error);
        reject(error);
      };
    });
  }

  // 执行下一步测试
  async executeNextStep() {
    if (this.currentStepIndex >= this.testSteps.length) {
      console.log('所有测试步骤执行完毕');
      this.printTestSummary();
      return;
    }

    const step = this.testSteps[this.currentStepIndex];
    console.log(`执行步骤 ${this.currentStepIndex + 1}:`, step.name);

    if (step.type === 'send') {
      this.send(step.message);
    } else if (step.type === 'close') {
      this.close();
    } else if (step.type === 'wait') {
      this.waitForMessage(step.expectedMessage, step.timeout);
    } else if (step.type === 'reconnect') {
      await this.reconnect();
    }

    // 记录步骤开始时间
    this.currentStepStartTime = Date.now();
  }

  // 发送消息
  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('发送消息:', message);
      this.ws.send(JSON.stringify(message));
      this.executeNextStep();
    } else {
      this.failStep(`无法发送消息: WebSocket未连接`);
    }
  }

  // 等待消息
  waitForMessage(expectedMessage, timeout) {
    // 检查是否已经收到消息
    if (this.checkMessageReceived(expectedMessage)) {
      this.passStep();
      return;
    }

    // 设置超时
    this.timeoutId = setTimeout(() => {
      this.timeoutId = null;
      this.failStep(`等待消息超时: ${expectedMessage}`);
    }, timeout || 5000);
  }

  // 检查消息是否已接收
  checkMessageReceived(expectedMessage) {
    if (!expectedMessage) return true;

    const isMatch = this.receivedMessages.some((msg) => {
      try {
        const parsedMsg = JSON.parse(msg);
        return this.deepEqual(parsedMsg, expectedMessage);
      } catch (e) {
        return msg === expectedMessage;
      }
    });

    return isMatch;
  }

  // 检查当前步骤是否完成
  checkCurrentStep() {
    if (!this.timeoutId) return;

    const step = this.testSteps[this.currentStepIndex];
    if (step.type === 'wait' && this.checkMessageReceived(step.expectedMessage)) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
      this.passStep();
    }
  }

  // 关闭连接
  close() {
    if (this.ws) {
      this.ws.close();
      this.executeNextStep();
    } else {
      this.failStep('无法关闭: WebSocket未连接');
    }
  }

  // 重新连接
  reconnect() {
    return new Promise(async (resolve) => {
      if (this.ws) {
        this.ws.close();
        // 等待连接关闭
        await new Promise((r) => setTimeout(r, 500));
      }
      await this.connect();
      this.executeNextStep();
      resolve();
    });
  }

  // 通过当前步骤
  passStep() {
    const step = this.testSteps[this.currentStepIndex];
    const duration = Date.now() - this.currentStepStartTime;
    this.testResults.push({
      step: this.currentStepIndex + 1,
      name: step.name,
      status: 'pass',
      duration,
    });
    console.log(`步骤 ${this.currentStepIndex + 1} 通过 (${duration}ms)`);
    this.currentStepIndex++;
    this.executeNextStep();
  }

  // 失败当前步骤
  failStep(reason) {
    const step = this.testSteps[this.currentStepIndex];
    const duration = Date.now() - this.currentStepStartTime;
    this.testResults.push({
      step: this.currentStepIndex + 1,
      name: step.name,
      status: 'fail',
      duration,
      reason,
    });
    console.error(`步骤 ${this.currentStepIndex + 1} 失败: ${reason} (${duration}ms)`);
    this.currentStepIndex++;
    this.executeNextStep();
  }

  // 打印测试摘要
  printTestSummary() {
    console.log('\n===== 测试摘要 =====');
    this.testResults.forEach((result) => {
      console.log(
        `${result.status === 'pass' ? '✅' : '❌'} 步骤 ${result.step}: ${result.name} - ${
          result.status
        }${result.reason ? ` (${result.reason})` : ''}`
      );
    });

    const passed = this.testResults.filter((r) => r.status === 'pass').length;
    const failed = this.testResults.length - passed;
    console.log(`总测试: ${this.testResults.length}, 通过: ${passed}, 失败: ${failed}`);
  }

  // 对象深度比较
  deepEqual(obj1, obj2) {
    if (obj1 === obj2) return true;
    if (typeof obj1 !== 'object' || obj1 === null || typeof obj2 !== 'object' || obj2 === null)
      return false;

    const keys1 = Object.keys(obj1);
    const keys2 = Object.keys(obj2);

    if (keys1.length !== keys2.length) return false;

    for (const key of keys1) {
      if (!keys2.includes(key) || !this.deepEqual(obj1[key], obj2[key])) return false;
    }

    return true;
  }
}

// 模拟WebSocket服务器
class MockWebSocketServer {
  constructor(url) {
    this.url = url;
    this.clients = new Set();
    this.onmessageHandlers = [];
    this.autoReconnect = false;
    this.isClosed = false;
  }

  // 启动服务器
  start() {
    console.log(`模拟WebSocket服务器已启动: ${this.url}`);
    // 实际应用中这里会使用真正的WebSocket服务器
  }

  // 停止服务器
  stop() {
    console.log('模拟WebSocket服务器已停止');
    this.isClosed = true;
    this.clients.forEach((client) => client.close());
    this.clients.clear();
  }

  // 模拟接收消息
  onmessage(handler) {
    this.onmessageHandlers.push(handler);
  }

  // 模拟发送消息到所有客户端
  sendToAll(message) {
    console.log(`服务器发送消息: ${JSON.stringify(message)}`);
    this.clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.onmessage({ data: JSON.stringify(message) });
      }
    });
  }

  // 模拟断开连接
  disconnectAll() {
    console.log('服务器断开所有连接');
    this.clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.onclose({ code: 1006, reason: '模拟连接断开' });
      }
    });
    this.clients.clear();
  }

  // 模拟重新连接
  reconnect() {
    console.log('服务器重新上线');
    this.isClosed = false;
  }
}

// 使用示例
async function runWebSocketTest() {
  // 创建模拟服务器
  const mockServer = new MockWebSocketServer('ws://localhost:8080');
  mockServer.start();

  // 创建测试器
  const tester = new WebSocketTester('ws://localhost:8080');

  // 设置服务器消息处理
  mockServer.onmessage((message) => {
    console.log('服务器收到消息:', message);
    if (message === 'connect') {
      mockServer.sendToAll({ type: 'connected', status: 'ok' });
    } else if (message === 'ping') {
      mockServer.sendToAll({ type: 'pong', timestamp: Date.now() });
    } else if (message === 'close') {
      mockServer.disconnectAll();
    }
  });

  // 定义测试步骤
  tester
    .addStep({ name: '连接测试', type: 'send', message: 'connect' })
    .addStep({
      name: '验证连接成功',
      type: 'wait',
      expectedMessage: { type: 'connected', status: 'ok' },
    })
    .addStep({ name: '发送ping', type: 'send', message: 'ping' })
    .addStep({
      name: '验证pong响应',
      type: 'wait',
      expectedMessage: { type: 'pong' },
    })
    .addStep({ name: '模拟断线', type: 'send', message: 'close' })
    .addStep({
      name: '验证连接关闭',
      type: 'wait',
      expectedMessage: null, // 不需要特定消息，只需要等待连接关闭事件
    })
    .addStep({ name: '重新连接', type: 'reconnect' })
    .addStep({ name: '重新连接后发送ping', type: 'send', message: 'ping' })
    .addStep({
      name: '验证重新连接后pong响应',
      type: 'wait',
      expectedMessage: { type: 'pong' },
    });

  // 运行测试
  await tester.run();

  // 停止服务器
  mockServer.stop();
}

// 执行测试
runWebSocketTest();    