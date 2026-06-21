<script setup lang="ts">
import {computed, ref} from "vue"
import DashboardQuestSummaryRow from "./DashboardQuestSummaryRow.vue"
import DashboardSectionHeader from "./DashboardSectionHeader.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

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

const filteredQuests = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  const items = props.dashboard.availableQuests.filter((quest) => {
    if (photoOnly.value && !quest.images?.length) {
      return false
    }

    if (scheduledOnly.value && !quest.scheduledAt) {
      return false
    }

    if (!query) {
      return true
    }

    return [quest.title, quest.description, quest.creatorUsername]
      .join(" ")
      .toLowerCase()
      .includes(query)
  })

  return [...items].sort((left, right) => {
    if (sortMode.value === "highest") {
      return (right.awardAmount ?? 0) - (left.awardAmount ?? 0)
    }

    if (sortMode.value === "newest") {
      return new Date(right.scheduledAt ?? 0).getTime() - new Date(left.scheduledAt ?? 0).getTime()
    }

    const leftScheduled = left.scheduledAt ? new Date(left.scheduledAt).getTime() : Number.POSITIVE_INFINITY
    const rightScheduled = right.scheduledAt ? new Date(right.scheduledAt).getTime() : Number.POSITIVE_INFINITY

    return (right.awardAmount ?? 0) - (left.awardAmount ?? 0)
      || leftScheduled - rightScheduled
      || (right.id - left.id)
  })
})
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

      <div v-if="!filteredQuests.length" class="empty-state">
        No matching open jobs.
      </div>

      <div v-else class="dashboard-find-work__grid">
        <button
          v-for="quest in filteredQuests"
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
            />

            <div class="find-work-card__footer">
              <span class="badge badge--accent">$ {{ quest.awardAmount }}</span>
              <span class="muted">{{ quest.creatorUsername }}</span>
            </div>
          </div>
        </button>
      </div>
    </div>
  </section>
</template>
