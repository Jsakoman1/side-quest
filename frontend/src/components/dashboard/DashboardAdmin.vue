<script setup lang="ts">
import {computed, ref} from "vue"
import DashboardQuestSummaryRow from "./DashboardQuestSummaryRow.vue"
import DashboardSectionHeader from "./DashboardSectionHeader.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

const props = defineProps<{
  dashboard: QuestDashboard
}>()

const questSearch = ref("")

const filteredQuests = computed(() => {
  const query = questSearch.value.trim().toLowerCase()
  if (!query) {
    return props.dashboard.filteredAdminQuests
  }

  return props.dashboard.filteredAdminQuests.filter((quest) => {
    return [
      quest.title,
      quest.description,
      quest.creatorUsername,
      props.dashboard.formatStatus(quest.status),
      String(quest.awardAmount ?? "")
    ].some((value) => value.toLowerCase().includes(query))
  })
})
</script>

<template>
  <section class="stack">
    <article class="card admin-hero-card">
      <DashboardSectionHeader
        title="Admin control center"
        subtitle="Quest workspace for approvals, editing, status control, and term confirmations."
      >
        <template #stats>
          <div class="overview-stat-chip">
            <span class="label">Quests</span>
            <strong>{{ dashboard.questCount }}</strong>
          </div>
          <div class="overview-stat-chip">
            <span class="label">Waiting</span>
            <strong>{{ dashboard.waitingConfirmationQuestCount }}</strong>
          </div>
          <div class="overview-stat-chip">
            <span class="label">Users</span>
            <strong>{{ dashboard.totalUserCount }}</strong>
          </div>
          <div class="overview-stat-chip">
            <span class="label">Admins</span>
            <strong>{{ dashboard.adminUserCount }}</strong>
          </div>
        </template>
      </DashboardSectionHeader>

      <div class="button-row">
        <button class="button" type="button" @click="dashboard.refreshDashboardData">Refresh data</button>
      </div>
    </article>

    <article class="card" id="quests">
      <DashboardSectionHeader title="All quests" subtitle="Edit any quest from the list." />

      <div class="grid grid--two admin-toolbar">
        <label class="field">
          <span class="label">Search</span>
          <input v-model="questSearch" class="input" placeholder="Title, creator, status, award..." />
        </label>

        <div class="segmented">
          <button
            v-for="option in dashboard.questStatusOptions"
            :key="option.value"
            type="button"
            :class="['segment', { 'segment--active': dashboard.adminQuestStatusFilter === option.value }]"
            @click="dashboard.adminQuestStatusFilter = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <div v-if="!filteredQuests.length" class="empty-state mt-4">
        No quests in this group.
      </div>

      <div v-else class="quest-list mt-4">
        <button
          v-for="quest in filteredQuests"
          :key="quest.id"
          type="button"
          class="compact-disclosure compact-disclosure--tight compact-disclosure--launch"
          :class="[dashboard.statusSurfaceClass(quest.status), { 'ui-pulse': dashboard.successPulseTarget === `quest-${quest.id}` }]"
          @click="dashboard.openQuestDialog(quest.id)"
        >
              <DashboardQuestSummaryRow
                primary-label="Award"
                :primary-value="quest.awardAmount"
                primary-icon="$"
                money-tone="expense"
                secondary-label="Term"
                :secondary-value="dashboard.formatQuestTermLabel(quest)"
                :title="quest.title"
                :description="quest.description"
              >
                <template #meta>
                  <span :class="dashboard.statusBadgeClass(quest.status)">{{ dashboard.formatStatus(quest.status) }}</span>
                  <span v-if="quest.reopenedAt && quest.status === 'OPEN'" class="badge badge--warning">Reopened</span>
                  <span v-if="quest.status === 'WAITING_CONFIRMATION'" class="badge badge--warning">Awaiting confirmation</span>
                  <span class="badge badge--accent">{{ quest.creatorUsername }}</span>
                </template>
              </DashboardQuestSummaryRow>
        </button>
      </div>
    </article>
  </section>
</template>
