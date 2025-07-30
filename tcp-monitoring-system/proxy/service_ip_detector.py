import subprocess
import platform
import re
import socket
from typing import Dict, List, Optional, Tuple

class ServiceIPDetector:
    """跨平台服务IP地址检测器"""
    
    @staticmethod
    def get_service_ip(service_name: str) -> Optional[List[str]]:
        """
        获取指定服务绑定的IP地址
        
        Args:
            service_name: 服务名称，如"nginx"
        
        Returns:
            服务绑定的IP地址列表，如果未找到则返回None
        """
        system = platform.system()
        
        if system == "Linux":
            return ServiceIPDetector._get_linux_service_ip(service_name)
        elif system == "Windows":
            return ServiceIPDetector._get_windows_service_ip(service_name)
        else:
            print(f"不支持的操作系统: {system}")
            return None
    
    @staticmethod
    def _get_linux_service_ip(service_name: str) -> Optional[List[str]]:
        """获取Linux系统上服务的IP地址"""
        try:
            # 检查服务是否运行
            status = subprocess.run(
                ["systemctl", "is-active", service_name],
                capture_output=True,
                text=True
            )
            
            if status.stdout.strip() != "active":
                print(f"服务 {service_name} 未运行")
                return None
            
            # 获取服务监听的端口
            ports = ServiceIPDetector._get_linux_service_ports(service_name)
            if not ports:
                print(f"无法确定服务 {service_name} 监听的端口")
                return None
            
            # 获取端口对应的IP地址
            ips = []
            for port in ports:
                port_ips = ServiceIPDetector._get_linux_listening_ips(port)
                if port_ips:
                    ips.extend(port_ips)
            
            return list(set(ips))  # 去重
        
        except Exception as e:
            print(f"检测Linux服务IP失败: {e}")
            return None
    
    @staticmethod
    def _get_linux_service_ports(service_name: str) -> List[int]:
        """获取Linux服务监听的端口"""
        try:
            # 使用ss命令查找服务监听的端口
            result = subprocess.run(
                ["ss", "-tulpn", "|", "grep", service_name],
                shell=True,
                capture_output=True,
                text=True
            )
            
            ports = []
            for line in result.stdout.splitlines():
                # 解析形如":80"的端口信息
                match = re.search(r':(\d+)\s', line)
                if match:
                    ports.append(int(match.group(1)))
            
            return list(set(ports))
        
        except Exception as e:
            print(f"获取Linux服务端口失败: {e}")
            return []
    
    @staticmethod
    def _get_linux_listening_ips(port: int) -> List[str]:
        """获取Linux系统上指定端口监听的IP地址"""
        try:
            result = subprocess.run(
                ["ss", "-tulpn", "|", "grep", f":{port}"],
                shell=True,
                capture_output=True,
                text=True
            )
            
            ips = []
            for line in result.stdout.splitlines():
                # 解析形如"0.0.0.0:80"的IP地址
                match = re.search(r'([\d\.]+):\d+', line)
                if match:
                    ip = match.group(1)
                    if ip != "0.0.0.0":  # 排除通配符地址
                        ips.append(ip)
                    else:
                        # 获取本地所有IP地址
                        ips.extend(ServiceIPDetector._get_local_ips())
            
            return list(set(ips))
        
        except Exception as e:
            print(f"获取Linux监听IP失败: {e}")
            return []
    
    @staticmethod
    def _get_windows_service_ip(service_name: str) -> Optional[List[str]]:
        """获取Windows系统上服务的IP地址"""
        try:
            # 检查服务是否运行
            result = subprocess.run(
                ["sc", "query", service_name],
                capture_output=True,
                text=True
            )
            
            if "RUNNING" not in result.stdout:
                print(f"服务 {service_name} 未运行")
                return None
            
            # 获取服务可执行文件路径
            path = ServiceIPDetector._get_windows_service_path(service_name)
            if not path:
                print(f"无法获取服务 {service_name} 的可执行文件路径")
                return None
            
            # 获取可执行文件监听的端口
            ports = ServiceIPDetector._get_windows_process_ports(path)
            if not ports:
                print(f"无法确定服务 {service_name} 监听的端口")
                return None
            
            # 获取端口对应的IP地址
            ips = []
            for port in ports:
                port_ips = ServiceIPDetector._get_windows_listening_ips(port)
                if port_ips:
                    ips.extend(port_ips)
            
            return list(set(ips))  # 去重
        
        except Exception as e:
            print(f"检测Windows服务IP失败: {e}")
            return None
    
    @staticmethod
    def _get_windows_service_path(service_name: str) -> Optional[str]:
        """获取Windows服务的可执行文件路径"""
        try:
            result = subprocess.run(
                ["sc", "qc", service_name],
                capture_output=True,
                text=True
            )
            
            for line in result.stdout.splitlines():
                if line.strip().startswith("BINARY_PATH_NAME"):
                    return line.split(":", 1)[1].strip()
            
            return None
        
        except Exception as e:
            print(f"获取Windows服务路径失败: {e}")
            return None
    
    @staticmethod
    def _get_windows_process_ports(exe_path: str) -> List[int]:
        """获取Windows进程监听的端口"""
        try:
            # 获取所有监听端口及其PID
            result = subprocess.run(
                ["netstat", "-ano", "-p", "TCP"],
                capture_output=True,
                text=True
            )
            
            # 获取进程PID
            pid = ServiceIPDetector._get_windows_process_pid(exe_path)
            if not pid:
                return []
            
            ports = []
            for line in result.stdout.splitlines():
                if "LISTENING" in line and f" {pid} " in line:
                    # 解析形如"0.0.0.0:80"的端口信息
                    match = re.search(r':(\d+)\s', line)
                    if match:
                        ports.append(int(match.group(1)))
            
            return list(set(ports))
        
        except Exception as e:
            print(f"获取Windows进程端口失败: {e}")
            return []
    
    @staticmethod
    def _get_windows_process_pid(exe_path: str) -> Optional[int]:
        """获取Windows进程的PID"""
        try:
            result = subprocess.run(
                ["tasklist", "/v", "/fo", "csv"],
                capture_output=True,
                text=True
            )
            
            exe_name = exe_path.split("\\")[-1].split(".")[0].lower()
            
            for line in result.stdout.splitlines():
                if exe_name in line.lower() and exe_path.lower() in line.lower():
                    parts = line.strip().split('","')
                    if len(parts) > 1:
                        try:
                            return int(parts[1])
                        except ValueError:
                            continue
            
            return None
        
        except Exception as e:
            print(f"获取Windows进程PID失败: {e}")
            return None
    
    @staticmethod
    def _get_windows_listening_ips(port: int) -> List[str]:
        """获取Windows系统上指定端口监听的IP地址"""
        try:
            result = subprocess.run(
                ["netstat", "-ano", "-p", "TCP", "|", "findstr", f":{port}"],
                shell=True,
                capture_output=True,
                text=True
            )
            
            ips = []
            for line in result.stdout.splitlines():
                if "LISTENING" in line:
                    # 解析形如"0.0.0.0:80"的IP地址
                    match = re.search(r'([\d\.]+):\d+', line)
                    if match:
                        ip = match.group(1)
                        if ip != "0.0.0.0":  # 排除通配符地址
                            ips.append(ip)
                        else:
                            # 获取本地所有IP地址
                            ips.extend(ServiceIPDetector._get_local_ips())
            
            return list(set(ips))
        
        except Exception as e:
            print(f"获取Windows监听IP失败: {e}")
            return []
    
    @staticmethod
    def _get_local_ips() -> List[str]:
        """获取本地所有IP地址"""
        try:
            hostname = socket.gethostname()
            return socket.gethostbyname_ex(hostname)[2]
        except Exception:
            return ["127.0.0.1"]

# 使用示例
if __name__ == "__main__":
    # 检测Nginx服务的IP地址
    nginx_ips = ServiceIPDetector.get_service_ip("nginx")
    if nginx_ips:
        print(f"Nginx服务IP地址: {', '.join(nginx_ips)}")
    else:
        print("未找到Nginx服务或无法确定其IP地址")
    
    # 检测其他服务的IP地址
    # other_ips = ServiceIPDetector.get_service_ip("other_service")
    # print(f"其他服务IP地址: {', '.join(other_ips) if other_ips else '未找到'}")    