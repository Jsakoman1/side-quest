<script setup lang="ts">
defineProps<{
  dashboard: any
}>()
</script>

<template>
  <header class="dashboard-header panel">
    <div>
      <h1 class="dashboard-title">{{ dashboard.sectionTitle }}</h1>
      <p class="dashboard-subtitle">{{ dashboard.sectionSubtitle }}</p>
    </div>
  </header>
</template>
