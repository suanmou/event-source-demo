import axios from 'axios';

// 创建axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // api的base_url
  timeout: 10000 // 请求超时时间
});

// request拦截器
service.interceptors.request.use(config => {
  // 在发送请求之前做些什么
  const token = localStorage.getItem('fix-console-token');
  if (token) {
    // 让每个请求携带token--['Authorization']为自定义key 请根据实际情况自行修改
    config.headers['Authorization'] = 'Bearer ' + token;
  }
  return config;
}, error => {
  // 处理请求错误
  console.log(error); // for debug
  Promise.reject(error);
});

// response拦截器
service.interceptors.response.use(
  response => {
    // 对响应数据做点什么
    const res = response.data;
    if (res.code !== 200) {
      // 非成功状态码
      console.error('Error:', res.message);
      return Promise.reject(res.message || 'Error');
    } else {
      return res;
    }
  },
  error => {
    // 对响应错误做点什么
    console.log('err' + error); // for debug
    if (error.response) {
      const status = error.response.status;
      if (status === 401) {
        // 未授权，跳转到登录页
        localStorage.removeItem('fix-console-token');
        window.location.href = '/login';
      } else if (status === 403) {
        // 没有权限
        alert('没有权限访问该资源');
      } else if (status === 500) {
        // 服务器错误
        alert('服务器内部错误，请稍后重试');
      }
    }
    return Promise.reject(error);
  }
);

export default service;
    