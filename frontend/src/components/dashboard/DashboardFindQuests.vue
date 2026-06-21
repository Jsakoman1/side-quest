<script setup lang="ts">
import {computed, ref} from "vue"
import DashboardQuestSummaryRow from "./DashboardQuestSummaryRow.vue"
import DashboardSectionHeader from "./DashboardSectionHeader.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"
import {type Quest} from "../../api/sidequestApi.ts"
import type {QuestAudience} from "../../shared/sidequestDomain.ts"
import {formatQuestLifecycleLabel} from "../../lib/questDashboardRules.ts"
import {normalizeSearchQuery} from "../../lib/searchQuery.ts"
import {buildQuestSearchParams} from "../../lib/questSearch.ts"
import {useQuestSearchResults} from "../../composables/useQuestSearchResults.ts"
import UiPagination from "../ui/UiPagination.vue"

const props = withDefaults(defineProps<{
  dashboard: QuestDashboard
  showHeader?: boolean
}>(), {
  showHeader: true,
})

const searchQuery = ref("")
const sortMode = ref<"recommended" | "newest" | "highest">("recommended")
const photoOnly = ref(false)
const scheduledOnly = ref(false)
const audienceFilter = ref<QuestAudience | "ALL">("ALL")
const dateFrom = ref("")
const dateTo = ref("")
const itemsPerPage = 6
const {results: questResults, loadQuests, watchAndReload} = useQuestSearchResults(itemsPerPage, (page) => buildQuestSearchParams({
  q: normalizeSearchQuery(searchQuery.value),
  status: "OPEN",
  audience: audienceFilter.value === "ALL" ? null : audienceFilter.value,
  dateFrom: dateFrom.value || null,
  dateTo: dateTo.value || null,
  excludeMine: true,
  withImages: photoOnly.value || undefined,
  scheduledOnly: scheduledOnly.value || undefined,
  sort: sortMode.value,
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

watchAndReload([searchQuery, sortMode, photoOnly, scheduledOnly, audienceFilter, dateFrom, dateTo])

const previousPage = () => {
  void questResults.previousPage(loadQuests)
}

const nextPage = () => {
  void questResults.nextPage(loadQuests)
}
</script>

<template>
  <section class="stack">
    <div class="card dashboard-work-panel dashboard-work-panel--find">
      <DashboardSectionHeader v-if="props.showHeader" title="Find work" subtitle="Search open jobs and apply fast." />

      <div class="dashboard-find-work__toolbar">
        <label class="field dashboard-find-work__search">
          <span class="label">Search</span>
          <input v-model="searchQuery" class="input" placeholder="Title, creator, description" />
        </label>

        <label class="field dashboard-find-work__sort">
          <span class="label">Sort</span>
          <select v-model="sortMode" class="input">
            <option value="recommended">Recommended</option>
            <option value="newest">Soonest</option>
            <option value="highest">Highest award</option>
          </select>
        </label>
      </div>

      <div class="grid grid--two dashboard-find-work__advanced-filters">
        <label class="field">
          <span class="label">Audience</span>
          <select v-model="audienceFilter" class="input">
            <option value="ALL">All</option>
            <option v-for="option in props.dashboard.questAudienceOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <div class="grid grid--two">
          <label class="field">
            <span class="label">From date</span>
            <input v-model="dateFrom" class="input" type="date" />
          </label>

          <label class="field">
            <span class="label">To date</span>
            <input v-model="dateTo" class="input" type="date" />
          </label>
        </div>
      </div>

      <div class="dashboard-find-work__filters">
        <button class="segment" :class="{ 'segment--active': !photoOnly }" type="button" @click="photoOnly = false">
          All
        </button>
        <button class="segment" :class="{ 'segment--active': photoOnly }" type="button" @click="photoOnly = true">
          With photos
        </button>
        <button class="segment" :class="{ 'segment--active': !scheduledOnly }" type="button" @click="scheduledOnly = false">
          Any time
        </button>
        <button class="segment" :class="{ 'segment--active': scheduledOnly }" type="button" @click="scheduledOnly = true">
          Scheduled
        </button>
      </div>

      <div v-if="isLoading" class="empty-state">
        Loading quests...
      </div>

      <template v-else>
        <div v-if="!pagedQuests.length" class="empty-state">
          No matching open jobs.
        </div>

        <template v-else>
          <UiPagination :label="`Showing ${pageStart}-${pageEnd} of ${totalItems}`" :has-previous="hasPreviousPage" :has-next="hasNextPage" @previous="previousPage" @next="nextPage" />

          <div class="dashboard-find-work__grid">
            <button
              v-for="quest in pagedQuests"
              :key="quest.id"
              type="button"
              class="find-work-card"
              @click="dashboard.openQuestDialog(quest.id)"
            >
              <div v-if="quest.images?.length" class="find-work-card__media">
                <img :src="quest.images[0]" alt="Quest image preview" class="find-work-card__image">
              </div>

              <div class="find-work-card__body">
                <DashboardQuestSummaryRow
                  primary-label="Amount"
                  :primary-value="quest.awardAmount"
                  money-tone="income"
                  secondary-label="Term"
                  :secondary-value="dashboard.formatQuestTermLabel(quest)"
                  :title="quest.title"
                  :description="quest.description"
                  description-class="quest-summary-clamp"
                  :reserve-secondary-space="false"
                  :reserve-description-space="false"
                >
                  <template #meta>
                    <span class="badge badge--accent">{{ formatQuestLifecycleLabel(quest.status) }}</span>
                    <span class="badge badge--secondary">{{ quest.audience === "EVERYONE" ? "Everyone" : "Circles" }}</span>
                    <span v-if="quest.termFixed" class="badge badge--success">Fixed time</span>
                    <span v-else class="badge badge--warning">Flexible time</span>
                  </template>
                </DashboardQuestSummaryRow>

                <div class="find-work-card__footer">
                  <span class="badge badge--accent">$ {{ quest.awardAmount }}</span>
                  <span class="muted">{{ quest.creatorUsername }}</span>
                </div>
              </div>
            </button>
          </div>

          <UiPagination class="dashboard-find-work__pagination--bottom" :label="`Page ${currentPage} of ${totalPages}`" :has-previous="hasPreviousPage" :has-next="hasNextPage" @previous="previousPage" @next="nextPage" />
        </template>
      </template>
    </div>
  </section>
</template>
