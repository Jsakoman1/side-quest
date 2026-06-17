<script setup lang="ts">
import DashboardEditSheet from "./DashboardEditSheet.vue"
import DashboardSectionHeader from "./DashboardSectionHeader.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

defineProps<{
  dashboard: QuestDashboard
}>()
</script>

<template>
  <section class="stack">
    <article :class="['card dashboard-work-panel dashboard-work-panel--create', { 'ui-pulse': dashboard.successPulseTarget === 'create-work' }]">
      <DashboardSectionHeader title="Create work" subtitle="Draft a new quest." />

      <form @submit.prevent="dashboard.createQuest">
        <DashboardEditSheet :minimal="true">
          <div class="dashboard-edit-form dashboard-edit-form--create dashboard-edit-form--create-modern">
            <label class="field dashboard-edit-field dashboard-edit-field--title">
              <span class="label">Title</span>
              <input v-model="dashboard.questTitle" class="input" maxlength="120" />
            </label>

            <label class="field dashboard-edit-field dashboard-edit-field--description">
              <span class="label">Description</span>
              <textarea v-model="dashboard.questDescription" class="textarea" />
            </label>

            <label class="field dashboard-edit-field dashboard-edit-field--amount">
              <span class="label">Award amount</span>
              <div class="dashboard-edit-amount">
                <span class="dashboard-edit-amount__symbol" aria-hidden="true">$</span>
                <input v-model="dashboard.questAwardAmount" class="input dashboard-edit-amount__input" inputmode="decimal" placeholder="50" />
              </div>
            </label>

            <label class="field dashboard-edit-field">
              <span class="label">Scheduled time</span>
              <input v-model="dashboard.questScheduledAt" class="input" type="datetime-local" />
            </label>

            <label class="field dashboard-edit-field dashboard-edit-field--toggle">
              <span class="label">Time type</span>
              <div class="checkbox-field">
                <input v-model="dashboard.questTermFixed" type="checkbox" />
                <span>Fixed term</span>
              </div>
              <p class="muted mt-2 mb-0">
                Use a fixed time for a locked schedule. Leave it negotiable if the final time should be agreed later.
              </p>
            </label>

            <label v-if="dashboard.isAdmin()" class="field">
              <span class="label">Creator</span>
              <select v-model="dashboard.questCreatorId" class="input">
                <option v-for="user in dashboard.appUsers" :key="user.id" :value="String(user.id)">
                  {{ user.username }} ({{ user.email }})
                </option>
              </select>
            </label>
          </div>

          <template #actions>
            <button class="button button--action button--flat-primary" type="submit">Create work</button>
          </template>
        </DashboardEditSheet>
      </form>
    </article>
  </section>
</template>
