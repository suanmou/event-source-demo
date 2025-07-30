# ... 原有代码 ...

# 新增导入
from service_ip_detector import ServiceIPDetector

class ProxyMonitor:
    """代理服务器监控系统"""
    
    def __init__(self, config_path: str = "proxy_config.json"):
        """初始化代理监控系统"""
        self.config = self._load_config(config_path)
        self.proxies = {}
        self.stats = {}
        
        # 自动检测服务IP
        self._auto_detect_service_ips()
        
        # 初始化统计数据
        for proxy_name, proxy_config in self.config.items():
            self.stats[proxy_name] = {
                "current_connections": 0,
                "total_connections": 0,
                "total_bytes_sent": 0,
                "total_bytes_received": 0,
                "min_rtt": float('inf'),
                "max_rtt": 0,
                "avg_rtt": 0,
                "connections": {},
                "start_time": time.time()
            }
    
    def _auto_detect_service_ips(self):
        """自动检测服务IP地址"""
        for proxy_name, proxy_config in self.config.items():
            if 'service_name' in proxy_config:
                service_name = proxy_config['service_name']
                ips = ServiceIPDetector.get_service_ip(service_name)
                
                if ips:
                    # 更新配置中的IP地址
                    if 'host' in proxy_config:
                        print(f"警告: 配置中已指定主机地址 {proxy_config['host']}，"
                              f"将覆盖自动检测的地址 {', '.join(ips)}")
                    else:
                        proxy_config['host'] = ips[0]
                        print(f"自动检测到 {service_name} 服务的IP地址: {ips[0]}")
                else:
                    print(f"无法检测到 {service_name} 服务的IP地址，使用默认配置")
    
    # ... 原有代码 ...    