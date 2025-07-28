import psutil
import socket
import time
from flask import Flask, jsonify
from threading import Thread

app = Flask(__name__)

# 配置参数
TARGET_SERVER = "10.0.3.100"  # 内部FIX服务器IP
TARGET_PORT = 8080            # 内部FIX服务器端口
APP_NAME = "nginx-proxy"      # 应用名称

# 存储历史数据
connection_history = []
MAX_HISTORY = 60  # 保留60个数据点（约5分钟）

def get_tcp_connections():
    """获取所有TCP连接并过滤目标连接"""
    connections = []
    for conn in psutil.net_connections(kind='tcp'):
        if conn.raddr and conn.raddr.ip == TARGET_SERVER and conn.raddr.port == TARGET_PORT:
            connections.append({
                "fd": conn.fd,
                "status": conn.status,
                "laddr": f"{conn.laddr.ip}:{conn.laddr.port}",
                "raddr": f"{conn.raddr.ip}:{conn.raddr.port}",
                "pid": conn.pid,
                "create_time": time.time() - conn.create_time
            })
    return connections

def calculate_rtt(connections):
    """计算平均RTT（模拟值，实际需要更复杂实现）"""
    if not connections:
        return 0
    
    # 在实际生产环境中，这里可以使用更精确的RTT测量
    # 例如通过/proc/net/tcp信息或内核统计
    return sum(conn['create_time'] for conn in connections) / len(connections)

def collect_metrics():
    """收集所有监控指标"""
    connections = get_tcp_connections()
    
    # 按状态统计连接数
    status_count = {}
    for conn in connections:
        status = conn["status"]
        status_count[status] = status_count.get(status, 0) + 1
    
    return {
        "timestamp": time.time(),
        "app_name": APP_NAME,
        "total_connections": len(connections),
        "status_distribution": status_count,
        "rtt": calculate_rtt(connections),
        "active_connections": [conn for conn in connections if conn["status"] == "ESTABLISHED"]
    }

def background_collector():
    """后台定时收集数据"""
    global connection_history
    while True:
        metrics = collect_metrics()
        connection_history.append(metrics)
        
        # 保留固定长度的历史数据
        if len(connection_history) > MAX_HISTORY:
            connection_history = connection_history[-MAX_HISTORY:]
        
        time.sleep(5)  # 每5秒收集一次

@app.route('/metrics', methods=['GET'])
def get_metrics():
    """提供监控数据的REST端点"""
    current = collect_metrics()
    return jsonify({
        "current": current,
        "history": connection_history
    })

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查端点"""
    return jsonify({"status": "healthy", "timestamp": time.time()})

if __name__ == '__main__':
    # 启动后台收集线程
    collector_thread = Thread(target=background_collector)
    collector_thread.daemon = True
    collector_thread.start()
    
    # 启动Flask应用
    app.run(host='0.0.0.0', port=5000, threaded=True)