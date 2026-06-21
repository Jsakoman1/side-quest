<script setup lang="ts">
import {computed, ref} from "vue"
import DashboardQuestSummaryRow from "./DashboardQuestSummaryRow.vue"
import DashboardSectionHeader from "./DashboardSectionHeader.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"
import {type Quest} from "../../api/sidequestApi.ts"
import type {QuestAudience} from "../../shared/sidequestDomain.ts"
import {normalizeSearchQuery} from "../../lib/searchQuery.ts"
import {buildQuestSearchParams} from "../../lib/questSearch.ts"
import {useQuestSearchResults} from "../../composables/useQuestSearchResults.ts"
import UiPagination from "../ui/UiPagination.vue"

const props = defineProps<{
  dashboard: QuestDashboard
}>()

const questSearch = ref("")
const audienceFilter = ref<QuestAudience | "ALL">("ALL")
const dateFrom = ref("")
const dateTo = ref("")
const itemsPerPage = 8
const {results: questResults, loadQuests, watchAndReload} = useQuestSearchResults(itemsPerPage, (page) => buildQuestSearchParams({
  q: normalizeSearchQuery(questSearch.value),
  status: props.dashboard.adminQuestStatusFilter === "ALL" ? null : props.dashboard.adminQuestStatusFilter,
  audience: audienceFilter.value === "ALL" ? null : audienceFilter.value,
  dateFrom: dateFrom.value || null,
  dateTo: dateTo.value || null,
  sort: "recommended",
  page,
  size: itemsPerPage
}))
const pagedQuests = computed(() => questResults.items.value)
const totalItems = questResults.totalItems
const totalPages = questResults.totalPages
const currentPage = questResults.currentPage
const isLoading = questResults.isLoading
const pageStart = questResults.pageStart
const pageEnd = questResults.pageEnd
const hasPreviousPage = questResults.hasPreviousPage
const hasNextPage = questResults.hasNextPage

watchAndReload([questSearch, () => props.dashboard.adminQuestStatusFilter, audienceFilter, dateFrom, dateTo])

const previousPage = () => {
  void questResults.previousPage(loadQuests)
}

const nextPage = () => {
  void questResults.nextPage(loadQuests)
}
</script>

<template>
  <section class="stack">
    <article class="card admin-hero-card">
      <DashboardSectionHeader
        title="Admin control center"
        subtitle="Quest workspace for approvals, editing, status control, and term confirmations."
      />

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

        <label class="field">
          <span class="label">Status</span>
          <select v-model="dashboard.adminQuestStatusFilter" class="input">
            <option
              v-for="option in dashboard.questStatusOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>
        </label>
      </div>

      <div class="grid grid--three admin-toolbar admin-toolbar--filters">
        <label class="field">
          <span class="label">Audience</span>
          <select v-model="audienceFilter" class="input">
            <option value="ALL">All</option>
            <option v-for="option in props.dashboard.questAudienceOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="field">
          <span class="label">From date</span>
          <input v-model="dateFrom" class="input" type="date" />
        </label>

        <label class="field">
          <span class="label">To date</span>
          <input v-model="dateTo" class="input" type="date" />
        </label>
      </div>

      <div v-if="isLoading" class="empty-state mt-4">
        Loading quests...
      </div>

      <template v-else>
        <div v-if="!pagedQuests.length" class="empty-state mt-4">
          No quests in this group.
        </div>

        <template v-else>
          <UiPagination class="mt-4" :label="`Showing ${pageStart}-${pageEnd} of ${totalItems}`" :has-previous="hasPreviousPage" :has-next="hasNextPage" @previous="previousPage" @next="nextPage" />

          <div class="quest-list mt-4">
            <button
              v-for="quest in pagedQuests"
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
                  <span class="badge badge--secondary">{{ quest.audience === "EVERYONE" ? "Everyone" : "Circles" }}</span>
                  <span v-if="quest.termFixed" class="badge badge--success">Fixed time</span>
                  <span v-else class="badge badge--warning">Flexible time</span>
                </template>
              </DashboardQuestSummaryRow>
            </button>
          </div>

          <UiPagination class="dashboard-find-work__pagination--bottom mt-4" :label="`Page ${currentPage} of ${totalPages}`" :has-previous="hasPreviousPage" :has-next="hasNextPage" @previous="previousPage" @next="nextPage" />
        </template>
      </template>
    </article>
  </section>
</template>
