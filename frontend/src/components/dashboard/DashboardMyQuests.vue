<script setup lang="ts">
import DashboardQuestSummaryRow from "./DashboardQuestSummaryRow.vue"
import DashboardSectionHeader from "./DashboardSectionHeader.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

defineProps<{
  dashboard: QuestDashboard
}>()
</script>

<template>
  <section class="stack">
    <div class="card">
      <DashboardSectionHeader
        title="Your work"
        subtitle="Manage your quests by status."
      >
        <template #stats>
          <div class="overview-stat-chip">
            <span class="label">Open</span>
            <strong>{{ dashboard.countMyQuestsByStatus("OPEN") }}</strong>
          </div>
          <div class="overview-stat-chip">
            <span class="label">Waiting</span>
            <strong>{{ dashboard.countMyQuestsByStatus("WAITING_CONFIRMATION") }}</strong>
          </div>
          <div class="overview-stat-chip">
            <span class="label">Active</span>
            <strong>{{ dashboard.countMyQuestsByStatus("ASSIGNED") + dashboard.countMyQuestsByStatus("IN_PROGRESS") }}</strong>
          </div>
          <div class="overview-stat-chip">
            <span class="label">Done</span>
            <strong>{{ dashboard.countMyQuestsByStatus("COMPLETED") }}</strong>
          </div>
        </template>
      </DashboardSectionHeader>

      <div class="segmented">
        <button
          v-for="option in dashboard.questStatusOptions"
          :key="option.value"
          type="button"
          :class="['segment', { 'segment--active': dashboard.myQuestStatusFilter === option.value }]"
          @click="dashboard.myQuestStatusFilter = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <div v-if="!dashboard.filteredMyQuests.length" class="empty-state">
        No quests here yet.
      </div>

      <div v-else class="quest-list mt-4">
        <button
          v-for="quest in dashboard.filteredMyQuests"
          :key="quest.id"
          type="button"
          class="compact-disclosure compact-disclosure--tight compact-disclosure--launch"
          :class="[dashboard.statusSurfaceClass(quest.status), { 'ui-pulse': dashboard.successPulseTarget === `quest-${quest.id}` }]"
          @click="dashboard.openQuestDialog(quest.id)"
        >
          <DashboardQuestSummaryRow
            primary-label="Amount"
            :primary-value="quest.awardAmount"
            primary-icon="$"
            money-tone="expense"
            secondary-label="Term"
            :secondary-value="dashboard.formatQuestTermLabel(quest)"
            :title="quest.title"
            :description="quest.description"
          >
            <template #meta>
              <span class="badge">{{ dashboard.formatStatus(quest.status) }}</span>
            </template>
          </DashboardQuestSummaryRow>
        </button>
      </div>
    </div>
  </section>
</template>
