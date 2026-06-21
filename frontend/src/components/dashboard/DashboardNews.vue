<script setup lang="ts">
import {computed} from "vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"
import {formatQuestNewsType, questNewsBadgeClass} from "../../shared/questNews.ts"

const props = defineProps<{
  dashboard: QuestDashboard
}>()

const feedItems = computed(() => {
  return props.dashboard.unreadNewsItems.map((item) => ({
    id: item.id,
    title: item.title,
    message: item.message,
    actorUsername: item.actorUsername,
    questTitle: item.questTitle,
    createdAt: item.createdAt,
    typeLabel: formatQuestNewsType(item.type),
    badgeClass: questNewsBadgeClass(item.type),
    questId: item.questId
  }))
})

const openItem = async (item: {id: number; questId: number | null}) => {
  await props.dashboard.markNewsItemAsRead(item.id)
  props.dashboard.closeNotificationsDialog()

  if (item.questId !== null && props.dashboard.questForId(item.questId)) {
    props.dashboard.openQuestDialog(item.questId)
  }
}
</script>

<template>
  <section class="stack dashboard-news-panel dashboard-news-panel--drawer">
    <div class="dashboard-news__titlebar">
      <h2 class="dashboard-news__title">Notifications</h2>

      <button
        v-if="props.dashboard.unreadNewsCount > 0"
        class="button button--secondary dashboard-news__mark-all"
        type="button"
        @click="props.dashboard.markNewsAsRead()"
      >
        Clear all
      </button>
    </div>

    <div v-if="props.dashboard.newsError" class="empty-state empty-state--error">
      {{ props.dashboard.newsError }}
    </div>

    <div v-else-if="feedItems.length" class="news-feed news-feed--dialog">
      <button
        v-for="item in feedItems"
        :key="item.id"
        type="button"
        class="news-item news-item--dialog dashboard-news__item dashboard-news__item--button"
        @click="openItem(item)"
      >
        <div class="news-item__top">
          <div class="news-item__badges">
            <span class="badge badge--accent">Unread</span>
            <span :class="['badge', item.badgeClass]">{{ item.typeLabel }}</span>
          </div>
          <span class="news-item__time">{{ props.dashboard.formatDateTime(item.createdAt) }}</span>
        </div>

        <strong class="news-item__title">{{ item.title }}</strong>
        <p class="news-item__message">{{ item.message }}</p>

        <div class="news-item__footer">
          <span class="news-item__meta">
            {{ item.actorUsername }}<template v-if="item.questTitle"> on {{ item.questTitle }}</template>
          </span>

          <span class="button button--secondary news-item__actions-cta">
            Open
          </span>
        </div>
      </button>
    </div>

    <div v-else class="empty-state empty-state--soft">
      No notifications yet.
    </div>
  </section>
</template>
