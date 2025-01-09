/ EventSource封装类
function EventSourceClient(url, options) {
    this.url = url;
    this.options = options || {};
    this.source = null;
    this.eventListeners = {};
    this.reconnectInterval = 5000; // 默认重连间隔
    this.reconnectAttempts = 0;   // 重连尝试次数
    this.maxReconnectAttempts = Infinity; // 最大重连尝试次数（默认为无限）
 
    // 初始化并连接
    this.init();
}
 
EventSourceClient.prototype = {
    constructor: EventSourceClient,
 
    // 初始化方法
    init: function() {
        this.connect();
    },
 
    // 连接到EventSource服务器
    connect: function() {
        if (this.source) {
            this.source.close(); // 如果已经有一个连接，先关闭它
        }
 
        this.source = new EventSource(this.url, this.options);
 
        // 绑定事件监听器
        this.bindEventListeners();
 
        // 增加重连尝试次数
        this.reconnectAttempts++;
    },
 
    // 绑定所有已注册的事件监听器
    bindEventListeners: function() {
        var self = this;
 
        // 处理连接打开（虽然EventSource没有直接的open事件，但可以通过其他方式模拟）
        // 注意：这里没有直接处理open，因为EventSource不直接支持
 
        // 处理消息接收事件
        this.source.onmessage = function(event) {
            self.onMessage(event);
        };
 
        // 处理连接错误事件
        this.source.onerror = function(event) {
            self.onError(event);
        };
    },
 
    // 接收消息时的回调函数
    onMessage: function(event) {
        var eventType = event.type || 'message'; // 默认事件类型为'message'
        if (this.eventListeners[eventType]) {
            this.eventListeners[eventType].call(this, event.data); // 使用call绑定this上下文
        } else if (this.eventListeners.message) {
            this.eventListeners.message.call(this, event.data);
        }
    },
 
    // 错误发生时的回调函数
    onError: function(event) {
        console.error('EventSource error:', event);
        this.reconnect();
    },
 
    // 添加事件监听器
    addEventListener: function(eventType, listener) {
        if (!this.eventListeners[eventType]) {
            this.eventListeners[eventType] = listener;
 
            // 如果已经连接，则立即绑定监听器（但实际上EventSource不支持动态添加监听器到已存在的连接）
            // 这里只是为了保持接口的一致性，实际绑定是在connect时完成的
        } else {
            console.warn('Event listener for type "' + eventType + '" is already registered.');
        }
    },
 
    // 移除事件监听器（实际上在EventSource中并不常用，因为连接断开后监听器也会失效）
    removeEventListener: function(eventType, listener) {
        if (this.eventListeners[eventType] === listener) {
            delete this.eventListeners[eventType];
        } else {
            console.warn('No event listener for type "' + eventType + '" with the specified listener found.');
        }
    },
 
    // 重新连接
    reconnect: function() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            console.log('Attempting to reconnect (' + this.reconnectAttempts + '/' + this.maxReconnectAttempts + ')...');
            setTimeout(this.connect.bind(this), this.reconnectInterval);
        } else {
            console.error('Max reconnect attempts reached. No further attempts will be made.');
            // 可以选择在这里执行一些清理操作或通知用户
        }
    },
 
    // 关闭连接
    close: function() {
        if (this.source) {
            this.source.close();
            this.source = null;
            this.eventListeners = {}; // 清除事件监听器（可选，因为连接关闭后它们将不再有效）
            this.reconnectAttempts = 0; // 重置重连尝试次数
        }
    },
 
    // 自定义方法：发送心跳消息（模拟，因为EventSource是单向的，客户端不能主动发送消息）
    // 这里只是为了展示如何添加自定义方法，实际上你需要服务器端的配合来实现心跳机制
    sendHeartbeat: function() {
        // 注意：EventSource不支持客户端发送消息到服务器，这只是一个占位方法
        console.log('Heartbeat sent (simulated, as EventSource does not support client-to-server messages).');
    },
 
    // 自定义方法：获取当前的重连尝试次数
    getReconnectAttempts: function() {
        return this.reconnectAttempts;
    },
 
    // 自定义方法：设置最大重连尝试次数
    setMaxReconnectAttempts: function(maxAttempts) {
        this.maxReconnectAttempts = maxAttempts;
    }
};
 
// 使用示例
var esClient = new EventSourceClient('https://example.com/events');
 
// 添加事件监听器
esClient.addEventListener('custom-event', function(data) {
    console.log('Custom event received:', data);
});
 
// 添加默认消息监听器
esClient.addEventListener('message', function(data) {
    console.log('Default message received:', data);
});
 
// 设置最大重连尝试次数为3次
esClient.setMaxReconnectAttempts(3);
 
// 在一段时间后关闭连接（模拟用户操作或超时）
setTimeout(function() {
    esClient.close();
}, 10000);