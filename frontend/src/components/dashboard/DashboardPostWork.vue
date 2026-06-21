<script setup lang="ts">
import DashboardEditSheet from "./DashboardEditSheet.vue"
import RichTextEditor from "../editor/RichTextEditor.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

defineProps<{
  dashboard: QuestDashboard
}>()
</script>

<template>
  <section class="stack">
    <article :class="['card dashboard-work-panel dashboard-work-panel--create', { 'ui-pulse': dashboard.successPulseTarget === 'create-job' }]">
      <form @submit.prevent="dashboard.createQuest">
        <DashboardEditSheet :minimal="true">
          <div class="dashboard-create-job__layout">
            <div class="dashboard-create-job__column dashboard-create-job__column--main">
              <label class="field dashboard-edit-field dashboard-edit-field--title">
                <span class="label">Job title</span>
                <input v-model="dashboard.questTitle" class="input" maxlength="120" placeholder="" />
              </label>

              <label class="field dashboard-edit-field dashboard-edit-field--description">
                <span class="label">Description</span>
                <RichTextEditor
                  v-model="dashboard.questDescription"
                  placeholder=""
                  toolbar-label="Description tools"
                />
              </label>

              <div class="field dashboard-edit-field dashboard-edit-field--gallery">
                <div class="field__header">
                  <span class="label">Quest photos</span>
                  <span class="badge badge--accent">{{ dashboard.questImages.length }}/10</span>
                </div>
                <input
                  class="input"
                  type="file"
                  accept="image/*"
                  multiple
                  @change="dashboard.addQuestImages(($event.target as HTMLInputElement).files ?? null); ($event.target as HTMLInputElement).value = ''"
                />
                <div v-if="dashboard.questImages.length" class="quest-gallery">
                  <div v-for="(image, index) in dashboard.questImages" :key="`${index}-${image.slice(0, 20)}`" class="quest-gallery__item">
                    <img class="quest-gallery__image" :src="image" alt="Quest image preview" />
                    <button class="button button--ghost quest-gallery__remove" type="button" @click="dashboard.removeQuestImage(index)">
                      Remove
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div class="dashboard-create-job__column dashboard-create-job__column--side">
              <label class="field dashboard-edit-field dashboard-edit-field--amount">
                <span class="label">Award amount</span>
                <div class="dashboard-edit-amount dashboard-edit-amount--compact">
                  <span class="dashboard-edit-amount__symbol" aria-hidden="true">$</span>
                  <input
                    v-model="dashboard.questAwardAmount"
                    class="input dashboard-edit-amount__input"
                    inputmode="decimal"
                    placeholder="50"
                  />
                </div>
              </label>

              <label class="field dashboard-edit-field">
                <span class="label">Scheduled time</span>
                <input v-model="dashboard.questScheduledAt" class="input" type="datetime-local" />
              </label>

              <label class="field dashboard-edit-field dashboard-edit-field--toggle dashboard-create-job__toggle">
                <span class="label">Time type</span>
                <div class="checkbox-field">
                  <input v-model="dashboard.questTermFixed" type="checkbox" />
                  <span>Fixed term</span>
                </div>
              </label>

              <label class="field dashboard-edit-field">
                <span class="label">Who can see this</span>
                <select v-model="dashboard.questAudience" class="input">
                  <option v-for="option in dashboard.questAudienceOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
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
          </div>

          <template #actions>
            <button class="button button--action button--flat-primary" type="submit">Publish job</button>
          </template>
        </DashboardEditSheet>
      </form>
    </article>
  </section>
</template>
