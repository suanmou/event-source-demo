const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });

wss.on('connection', (ws) => {
  ws.on('message', (testId) => {
    const interval = setInterval(() => {
      const test = testStore.get(testId);
      if (test) {
        ws.send(JSON.stringify({
          timestamp: Date.now(),
          rps: calculateRPS(test),
          latency: calculateCurrentLatency(test),
          errors: test.stats.errors
        }));
      }
    }, 1000);

    ws.on('close', () => clearInterval(interval));
  });
});