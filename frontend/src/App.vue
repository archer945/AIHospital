<template>
  <div class="app-shell">
    <router-view />
  </div>
</template>

<script setup>
// 入口组件仅负责提供全局背景与路由出口
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--gradient-secondary);
  padding: 32px 24px 48px;
}

@media (max-width: 768px) {
  .app-shell {
    padding: 16px;
  }
}
</style>
