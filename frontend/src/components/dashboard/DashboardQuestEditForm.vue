<script setup lang="ts">
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"
import RichTextEditor from "../editor/RichTextEditor.vue"
import DashboardEditSheet from "./DashboardEditSheet.vue"

defineProps<{ dashboard: QuestDashboard }>()
defineEmits<{ discard: [] }>()
</script>

<template>
  <form class="stack" @submit.prevent="dashboard.saveEditedQuest">
    <DashboardEditSheet minimal>
      <div class="dashboard-edit-form dashboard-edit-form--dialog">
        <label class="field dashboard-edit-field dashboard-edit-field--message">
          <span class="label">Award amount</span>
          <div class="dashboard-edit-amount">
            <span class="dashboard-edit-amount__symbol" aria-hidden="true">$</span>
            <input v-model="dashboard.editQuestAwardAmount" class="input dashboard-edit-amount__input" inputmode="decimal" placeholder="50" />
          </div>
        </label>
        <label class="field dashboard-edit-field dashboard-edit-field--price">
          <span class="label">Title</span>
          <input v-model="dashboard.editQuestTitle" class="input" />
        </label>
        <label class="field dashboard-edit-field">
          <span class="label">Description</span>
          <RichTextEditor v-model="dashboard.editQuestDescription" placeholder="" toolbar-label="Description tools" />
        </label>
        <label class="field dashboard-edit-field">
          <span class="label">Scheduled time</span>
          <input v-model="dashboard.editQuestScheduledAt" class="input" type="datetime-local" />
        </label>
        <label class="field dashboard-edit-field dashboard-edit-field--toggle">
          <span class="label">Time type</span>
          <div class="checkbox-field">
            <input v-model="dashboard.editQuestTermFixed" type="checkbox" />
            <span>Fixed term</span>
          </div>
        </label>
        <label class="field dashboard-edit-field">
          <span class="label">Who can see this</span>
          <select v-model="dashboard.editQuestAudience" class="input">
            <option v-for="option in dashboard.questAudienceOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <template v-if="dashboard.isAdmin()">
          <label class="field dashboard-edit-field">
            <span class="label">Creator</span>
            <select v-model="dashboard.editQuestCreatorId" class="input">
              <option v-for="user in dashboard.appUsers" :key="user.id" :value="String(user.id)">{{ user.username }} ({{ user.email }})</option>
            </select>
          </label>
          <label class="field dashboard-edit-field">
            <span class="label">Status</span>
            <select v-model="dashboard.editQuestStatus" class="input">
              <option v-for="option in dashboard.questStatusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
          </label>
        </template>
      </div>
      <template #actions>
        <button class="button button--action" type="submit">Save changes</button>
        <button class="button button--ghost" type="button" @click="$emit('discard')">Discard changes</button>
      </template>
    </DashboardEditSheet>
  </form>
</template>
