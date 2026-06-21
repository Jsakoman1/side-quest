<script setup lang="ts">
import {computed} from "vue"
import DashboardWorkPlanner from "./DashboardWorkPlanner.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

const props = defineProps<{
  dashboard: QuestDashboard
}>()

type RailGroup<T> = {
  key: string
  label: string
  items: T[]
}

const activeQuestGroups = computed<RailGroup<(typeof props.dashboard.activeMyQuests)[number]>[]>(() => {
  const order = ["ASSIGNED", "WAITING_CONFIRMATION", "IN_PROGRESS"]
  const grouped = new Map<string, (typeof props.dashboard.activeMyQuests)[number][]>()

  for (const quest of props.dashboard.activeMyQuests) {
    const bucket = grouped.get(quest.status) ?? []
    bucket.push(quest)
    grouped.set(quest.status, bucket)
  }

  return Array.from(grouped.entries())
    .sort((left, right) => {
      const leftIndex = order.indexOf(left[0])
      const rightIndex = order.indexOf(right[0])
      return (leftIndex === -1 ? order.length : leftIndex) - (rightIndex === -1 ? order.length : rightIndex)
    })
    .map(([status, items]) => ({
      key: `active-${status}`,
      label: props.dashboard.formatStatus(status),
      items
    }))
})

const activeOutgoingApplications = computed(() => props.dashboard.activeWorkApplications)

const activeOutgoingGroups = computed<RailGroup<(typeof props.dashboard.activeWorkApplications)[number]>[]>(() => {
  if (!activeOutgoingApplications.value.length) {
    return []
  }

  return [{
    key: "active-outgoing",
    label: "Active",
    items: activeOutgoingApplications.value
  }]
})

const openQuest = (questId: number) => {
  props.dashboard.openQuestDialog(questId)
}

const openApplications = () => {
  props.dashboard.openApplicationsDialog()
}

const openWork = () => {
  props.dashboard.openOpenWorkDialog()
}

const formatRailDateTime = (value: string | null | undefined) => {
  if (!value) {
    return "No scheduled time"
  }

  return new Intl.DateTimeFormat("en-GB", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value))
}
</script>

<template>
  <section class="overview-grid overview-grid--tabs">
    <div class="overview-panels overview-panels--triage">
      <article class="overview-rail overview-rail--left">
        <div class="overview-rail__header">
          <button class="overview-rail__cta-card overview-rail__cta-card--offer" type="button" @click="dashboard.openCreateJobDialog()">
            <span class="overview-rail__cta-title">Create work</span>
            <span class="overview-rail__cta-arrow" aria-hidden="true">→</span>
          </button>
          <button class="overview-rail__cta-card overview-rail__cta-card--open" type="button" @click="openWork">
            <span class="overview-rail__cta-copy">
              <span class="overview-rail__cta-title">Open work</span>
              <span class="overview-rail__cta-subtitle">{{ dashboard.visibleMyQuests.length }} need attention</span>
            </span>
            <span class="overview-rail__cta-arrow" aria-hidden="true">→</span>
          </button>
          <div class="overview-rail__header-copy">
            <span class="overview-rail__label">Jobs you offer</span>
          </div>
        </div>

        <div v-if="activeQuestGroups.length" class="overview-rail__stack">
          <section v-for="group in activeQuestGroups" :key="group.key" class="rail-group">
            <div class="rail-group__header">
              <span class="rail-group__label">{{ group.label }}</span>
            </div>

            <button
              v-for="quest in group.items"
              :key="quest.id"
              class="rail-tab rail-tab--incoming"
              :class="{ 'ui-pulse': dashboard.successPulseTarget === `quest-${quest.id}` }"
              type="button"
              @click="openQuest(quest.id)"
              :title="quest.title"
            >
              <div class="rail-tab__body">
                <div class="rail-tab__datetime">
                  {{ formatRailDateTime(quest.scheduledAt) }}
                </div>

                <strong class="rail-tab__title">{{ quest.title }}</strong>
              </div>
            </button>
          </section>
        </div>

        <div v-else class="rail-empty">
          No active work yet.
        </div>
      </article>

      <DashboardWorkPlanner :dashboard="dashboard" />

      <article class="overview-rail overview-rail--right">
        <div class="overview-rail__header">
          <button class="overview-rail__cta-card overview-rail__cta-card--find" type="button" @click="dashboard.openFindWorkDialog()">
            <span class="overview-rail__cta-copy">
              <span class="overview-rail__cta-title">Find work</span>
              <span class="overview-rail__cta-subtitle">Browse open jobs.</span>
            </span>
            <span class="overview-rail__cta-arrow" aria-hidden="true">→</span>
          </button>
          <button class="overview-rail__cta-card overview-rail__cta-card--applications" type="button" @click="openApplications">
            <span class="overview-rail__cta-copy">
              <span class="overview-rail__cta-title">Applications</span>
              <span class="overview-rail__cta-subtitle">{{ dashboard.pendingWorkApplications.length }} pending</span>
            </span>
            <span class="overview-rail__cta-arrow" aria-hidden="true">→</span>
          </button>
          <div class="overview-rail__header-copy">
            <span class="overview-rail__label">Jobs you do</span>
          </div>
        </div>

        <div v-if="activeOutgoingGroups.length" class="overview-rail__stack">
          <section v-for="group in activeOutgoingGroups" :key="group.key" class="rail-group">
            <div class="rail-group__header">
              <span class="rail-group__label">{{ group.label }}</span>
            </div>

            <button
              v-for="application in group.items"
              :key="application.id"
              class="rail-tab rail-tab--outgoing"
              :class="{
                'ui-pulse': dashboard.successPulseTarget === `application-${application.id}` || dashboard.successPulseTarget === `quest-${application.questId}`
              }"
              type="button"
              @click="openQuest(application.questId)"
              :title="application.questTitle"
            >
              <div class="rail-tab__body">
                <div class="rail-tab__datetime">
                  {{ formatRailDateTime(dashboard.questForId(application.questId)?.scheduledAt) }}
                </div>

                <strong class="rail-tab__title">{{ application.questTitle }}</strong>
              </div>
            </button>
          </section>
        </div>

        <div v-else class="rail-empty">
          No active work yet.
        </div>
      </article>
    </div>
  </section>
</template>
