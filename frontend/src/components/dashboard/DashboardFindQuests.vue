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
    <div class="card dashboard-work-panel dashboard-work-panel--find">
      <DashboardSectionHeader title="Find work" subtitle="Open quests you can take." />

      <div v-if="!dashboard.availableQuests.length" class="empty-state">
        No open quests right now.
      </div>

      <div v-else class="quest-list mt-4">
        <button
          v-for="quest in dashboard.availableQuests"
          :key="quest.id"
          type="button"
          class="compact-disclosure compact-disclosure--tight compact-disclosure--launch"
          :class="dashboard.statusSurfaceClass('OPEN')"
          @click="dashboard.openQuestDialog(quest.id)"
        >
          <DashboardQuestSummaryRow
            primary-label="Amount"
            :primary-value="quest.awardAmount"
            money-tone="income"
            secondary-label="Term"
            :secondary-value="dashboard.formatQuestTermLabel(quest)"
            :title="quest.title"
            :description="quest.description"
          >
            <template #meta>
              <span class="badge badge--accent">Open</span>
            </template>
          </DashboardQuestSummaryRow>
        </button>
      </div>
    </div>
  </section>
</template>
