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
      <DashboardSectionHeader title="Applied work" subtitle="Applications you sent.">
        <template #stats>
          <div class="overview-stat-chip">
            <span class="label">Open</span>
            <strong>{{ dashboard.countMyApplicationsByStatus("PENDING") }}</strong>
          </div>
          <div class="overview-stat-chip">
            <span class="label">Approved</span>
            <strong>{{ dashboard.countMyApplicationsByStatus("APPROVED") }}</strong>
          </div>
          <div class="overview-stat-chip">
            <span class="label">Total</span>
            <strong>{{ dashboard.sortedMyApplications.length }}</strong>
          </div>
        </template>
      </DashboardSectionHeader>

      <div v-if="!dashboard.sortedMyApplications.length" class="empty-state">
        No applications yet.
      </div>

      <div v-else class="quest-list mt-4">
        <button
          v-for="application in dashboard.sortedMyApplications"
          :key="application.id"
          type="button"
          class="compact-disclosure compact-disclosure--tight compact-disclosure--launch"
          :class="[dashboard.statusSurfaceClass(application.status), { 'ui-pulse': dashboard.successPulseTarget === `application-${application.id}` }]"
          @click="dashboard.openApplicationDialog(application.id)"
        >
          <DashboardQuestSummaryRow
            primary-label="Creator"
            :primary-value="dashboard.questCreatorUsernameForQuest(application.questId)"
            secondary-label="Price"
            :secondary-value="application.proposedPrice"
            secondary-icon="$"
            money-tone="income"
            :title="application.questTitle"
            :description="application.questDescription"
          >
            <template #meta>
              <span :class="dashboard.statusBadgeClass(application.status)">{{ dashboard.formatApplicationStatus(application.status) }}</span>
            </template>
          </DashboardQuestSummaryRow>
        </button>
      </div>
    </div>
  </section>
</template>
