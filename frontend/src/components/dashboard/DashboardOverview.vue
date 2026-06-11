<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from "vue"

const props = defineProps<{
  dashboard: any
}>()

const overviewRef = ref<HTMLElement | null>(null)

const closeOnOutsideClick = (event: MouseEvent) => {
  if (!props.dashboard.overviewFocus) {
    return
  }

  const target = event.target as Node | null
  if (overviewRef.value && target && !overviewRef.value.contains(target)) {
    props.dashboard.clearOverviewFocus()
  }
}

onMounted(() => {
  document.addEventListener("click", closeOnOutsideClick)
})

onBeforeUnmount(() => {
  document.removeEventListener("click", closeOnOutsideClick)
})
</script>

<template>
  <section ref="overviewRef" class="stack">
    <article
      v-for="card in dashboard.overviewCards"
      :key="card.id"
      :class="['overview-card panel', { 'overview-card--active': dashboard.overviewFocus === card.id }]"
    >
      <button type="button" class="overview-card__trigger" @click="dashboard.toggleOverviewFocus(card.id)">
        <div class="overview-card__top">
          <span class="overview-card__label">{{ card.label }}</span>
          <span class="overview-card__arrow">{{ dashboard.overviewFocus === card.id ? "−" : "+" }}</span>
        </div>
        <strong class="overview-card__value">{{ card.value }}</strong>
      </button>

      <Transition name="sheet-fade">
        <div v-if="dashboard.overviewFocus === card.id" class="overview-card__body">
          <div v-if="card.id === 'posted-work'">
            <div class="card__header">
              <div>
                <h2 class="card__title">Posted work</h2>
                <p class="muted mt-2">Quick look at your quests.</p>
              </div>
              <button class="button button--secondary" type="button" @click="dashboard.goToTab('my-quests')">Open</button>
            </div>

            <div class="grid grid--three">
              <div class="mini-stat">
                <span class="label">Open</span>
                <strong>{{ dashboard.countMyQuestsByStatus("OPEN") }}</strong>
              </div>
              <div class="mini-stat">
                <span class="label">Active</span>
                <strong>{{ dashboard.countMyQuestsByStatus("ASSIGNED") + dashboard.countMyQuestsByStatus("IN_PROGRESS") }}</strong>
              </div>
              <div class="mini-stat">
                <span class="label">Done</span>
                <strong>{{ dashboard.countMyQuestsByStatus("COMPLETED") }}</strong>
              </div>
            </div>

            <div v-if="dashboard.recentMyQuests.length" class="compact-list mt-4">
              <div v-for="quest in dashboard.recentMyQuests" :key="quest.id" class="compact-row">
                <div>
                  <strong>{{ quest.title }}</strong>
                  <div class="muted mt-1 text-clamp">{{ quest.description }}</div>
                </div>
                <span :class="dashboard.statusBadgeClass(quest.status)">{{ dashboard.formatStatus(quest.status) }}</span>
              </div>
            </div>
          </div>

          <div v-else-if="card.id === 'applied-tasks'">
            <div class="card__header">
              <div>
                <h2 class="card__title">Applied tasks</h2>
                <p class="muted mt-2">Track where you applied.</p>
              </div>
              <button class="button button--secondary" type="button" @click="dashboard.goToTab('my-applications')">Open</button>
            </div>

            <div class="grid grid--three">
              <div class="mini-stat">
                <span class="label">Total</span>
                <strong>{{ dashboard.sortedMyApplications.length }}</strong>
              </div>
              <div class="mini-stat">
                <span class="label">Pending</span>
                <strong>{{ dashboard.countMyApplicationsByStatus("PENDING") }}</strong>
              </div>
              <div class="mini-stat">
                <span class="label">Accepted</span>
                <strong>{{ dashboard.countMyApplicationsByStatus("ACCEPTED") }}</strong>
              </div>
            </div>

            <div v-if="dashboard.recentMyApplications.length" class="compact-list mt-4">
              <div v-for="application in dashboard.recentMyApplications" :key="application.id" class="compact-row">
                <div>
                  <strong>{{ application.questTitle }}</strong>
                  <div class="muted mt-1 text-clamp">{{ application.questDescription }}</div>
                </div>
                <span :class="dashboard.statusBadgeClass(application.status)">{{ dashboard.formatStatus(application.status) }}</span>
              </div>
            </div>
          </div>

          <div v-else>
            <div class="card__header">
              <div>
                <h2 class="card__title">Completed</h2>
                <p class="muted mt-2">Finished work at a glance.</p>
              </div>
              <button class="button button--secondary" type="button" @click="dashboard.goToTab('my-quests')">Open</button>
            </div>

            <div class="grid grid--three">
              <div class="mini-stat">
                <span class="label">Completed quests</span>
                <strong>{{ dashboard.countMyQuestsByStatus("COMPLETED") }}</strong>
              </div>
              <div class="mini-stat">
                <span class="label">Total posted</span>
                <strong>{{ dashboard.myQuests.length }}</strong>
              </div>
              <div class="mini-stat">
                <span class="label">Still open</span>
                <strong>{{ dashboard.countMyQuestsByStatus("OPEN") }}</strong>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </article>
  </section>
</template>
