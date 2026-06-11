<script setup lang="ts">
defineProps<{
  dashboard: any
}>()
</script>

<template>
  <section class="stack">
    <article class="card">
      <div class="card__header">
        <div>
          <h2 class="card__title">Post work</h2>
          <p class="muted mt-2">Create a new quest and publish it.</p>
        </div>
      </div>

      <form class="stack" @submit.prevent="dashboard.createQuest">
        <div class="grid grid--two">
          <label class="field">
            <span class="label">Title</span>
            <input v-model="dashboard.questTitle" class="input" maxlength="120" />
          </label>

          <label class="field">
            <span class="label">Award amount</span>
            <input v-model="dashboard.questAwardAmount" class="input" inputmode="decimal" />
          </label>
        </div>

        <label class="field">
          <span class="label">Description</span>
          <textarea v-model="dashboard.questDescription" class="textarea" />
        </label>

        <label v-if="dashboard.isAdmin()" class="field">
          <span class="label">Creator</span>
          <select v-model="dashboard.questCreatorId" class="input">
            <option v-for="user in dashboard.appUsers" :key="user.id" :value="String(user.id)">
              {{ user.username }} ({{ user.email }})
            </option>
          </select>
        </label>

        <div class="button-row">
          <button class="button" type="submit">Create quest</button>
        </div>
      </form>
    </article>
  </section>
</template>
