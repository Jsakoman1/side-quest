<script setup lang="ts">
defineProps<{
  dashboard: any
}>()
</script>

<template>
  <section class="card">
    <div class="card__header">
      <div>
        <h2 class="card__title">Profile</h2>
        <p class="muted mt-2">Your details.</p>
      </div>
      <button class="button button--secondary" type="button" @click="dashboard.openProfileEditDialog()">Edit</button>
    </div>

    <div class="stack">
      <div class="field">
        <span class="label">Username</span>
        <strong>{{ dashboard.currentUser?.username }}</strong>
      </div>
      <div class="field">
        <span class="label">Email</span>
        <strong>{{ dashboard.currentUser?.email }}</strong>
      </div>
    </div>
  </section>
</template>
