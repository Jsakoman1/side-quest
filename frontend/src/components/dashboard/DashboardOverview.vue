<script setup lang="ts">
import DashboardQuestSummaryRow from "./DashboardQuestSummaryRow.vue"
import DashboardSectionHeader from "./DashboardSectionHeader.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

defineProps<{
  dashboard: QuestDashboard
}>()
</script>

<template>
  <section class="overview-grid">
    <div class="overview-panels">
      <article class="card overview-panel overview-panel--my-work overview-panel--compact">
        <DashboardSectionHeader
          title="Your Quests"
          clickable
          @click="dashboard.clearOverviewFocus(); dashboard.goToTab('my-quests')"
        />

        <div v-if="dashboard.myQuests.length" class="overview-scroll mt-2">
          <button
            v-for="quest in dashboard.myQuests"
            :key="quest.id"
            type="button"
            class="compact-disclosure compact-disclosure--quest compact-disclosure--tight compact-disclosure--overview compact-disclosure--launch"
            :class="[dashboard.statusSurfaceClass(quest.status), { 'ui-pulse': dashboard.successPulseTarget === `quest-${quest.id}` }]"
            @click="dashboard.openQuestDialog(quest.id)"
          >
            <DashboardQuestSummaryRow
              primary-label="Amount"
              :primary-value="quest.awardAmount"
              primary-icon="$"
              money-tone="expense"
              :title="quest.title"
            >
              <template #meta>
                <span class="badge">{{ dashboard.formatStatus(quest.status) }}</span>
              </template>
            </DashboardQuestSummaryRow>
          </button>
        </div>

        <div v-else class="empty-state mt-3">
          No posted work yet.
        </div>
      </article>

      <article class="card overview-panel overview-panel--applied-work overview-panel--compact">
        <DashboardSectionHeader
          title="Applications you sent"
          clickable
          @click="dashboard.clearOverviewFocus(); dashboard.goToTab('my-applications')"
        />

        <div v-if="dashboard.sortedMyApplications.length" class="overview-scroll mt-2">
          <button
            v-for="application in dashboard.sortedMyApplications"
            :key="application.id"
            type="button"
            class="compact-disclosure compact-disclosure--tight compact-disclosure--overview compact-disclosure--launch"
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

        <div v-else class="empty-state mt-3">
          No applications yet.
        </div>
      </article>
    </div>
  </section>
</template>
