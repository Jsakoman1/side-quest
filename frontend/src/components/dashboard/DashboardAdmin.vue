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
    <article class="card">
      <DashboardSectionHeader title="Admin tools" subtitle="Users and quests live on the admin page." />

      <div class="button-row">
        <a class="button button--secondary" href="#users">Open users section</a>
      </div>
    </article>

    <article class="card">
      <DashboardSectionHeader title="All quests" subtitle="Edit any quest." />

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

      <div v-if="!dashboard.filteredAdminQuests.length" class="empty-state mt-4">
        No quests in this group.
      </div>

      <div v-else class="quest-list mt-4">
        <button
          v-for="quest in dashboard.filteredAdminQuests"
          :key="quest.id"
          type="button"
          class="compact-disclosure compact-disclosure--tight compact-disclosure--launch"
          :class="[dashboard.statusSurfaceClass(quest.status), { 'ui-pulse': dashboard.successPulseTarget === `quest-${quest.id}` }]"
          @click="dashboard.openQuestDialog(quest.id)"
        >
          <DashboardQuestSummaryRow
            primary-label="Creator"
            :primary-value="quest.creatorUsername"
            secondary-label="Amount"
            :secondary-value="quest.awardAmount"
            secondary-icon="$"
            :money-tone="dashboard.currentUser?.id === quest.creatorId ? 'expense' : 'income'"
            :title="quest.title"
            :description="quest.description"
          >
            <template #meta>
              <span :class="dashboard.statusBadgeClass(quest.status)">{{ dashboard.formatStatus(quest.status) }}</span>
              <span class="badge badge--accent">{{ quest.creatorUsername }}</span>
            </template>
          </DashboardQuestSummaryRow>
        </button>
      </div>
    </article>
  </section>
</template>
