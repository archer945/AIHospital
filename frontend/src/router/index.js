import { createRouter, createWebHistory } from 'vue-router'

// 创建路由规则
const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/Home.vue')
    },
]

// 创建 router
const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
