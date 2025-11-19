import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'   // ← 加这行

createApp(App)
    .use(router)                  // ← 再加这行
    .mount('#app')
