import Vue from 'vue';
import Router from 'vue-router';
import Login from '@/views/Login.vue';
import Dashboard from '@/views/Dashboard.vue';
import IPWhitelist from '@/views/IPWhitelist.vue';
import Certificates from '@/views/Certificates.vue';
import Sessions from '@/views/Sessions.vue';
import SessionHistory from '@/views/SessionHistory.vue';
import Logs from '@/views/Logs.vue';

Vue.use(Router);

const router = new Router({
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: Login
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: Dashboard,
      meta: { requiresAuth: true }
    },
    {
      path: '/ip-whitelist',
      name: 'IPWhitelist',
      component: IPWhitelist,
      meta: { requiresAuth: true }
    },
    {
      path: '/certificates',
      name: 'Certificates',
      component: Certificates,
      meta: { requiresAuth: true }
    },
    {
      path: '/sessions',
      name: 'Sessions',
      component: Sessions,
      meta: { requiresAuth: true }
    },
    {
      path: '/session-history',
      name: 'SessionHistory',
      component: SessionHistory,
      meta: { requiresAuth: true }
    },
    {
      path: '/logs',
      name: 'Logs',
      component: Logs,
      meta: { requiresAuth: true }
    },
    {
      path: '*',
      redirect: '/dashboard'
    }
  ]
});

// 路由守卫，检查是否需要登录
router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 检查本地存储中是否有token
    const token = localStorage.getItem('fix-console-token');
    if (!token) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      });
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
    