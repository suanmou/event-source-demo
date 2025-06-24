import Vue from 'vue';
import App from './App.vue';
import router from './router';
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import axios from './api';

Vue.config.productionTip = false;
Vue.use(ElementUI);

// 将axios挂载到Vue原型上
Vue.prototype.$api = axios;

new Vue({
  router,
  render: h => h(App),
}).$mount('#app');
    