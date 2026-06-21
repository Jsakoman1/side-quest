<script setup lang="ts">
import {computed, ref} from "vue"
import {useRouter} from "vue-router"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"
import {formatQuestNewsType, questNewsBadgeClass} from "../../shared/questNews.ts"

const props = defineProps<{
  dashboard: QuestDashboard
}>()

const router = useRouter()
const feedMode = ref<"unread" | "all">("unread")

const feedItems = computed(() => {
  const sourceItems = feedMode.value === "all" ? props.dashboard.recentNewsItems : props.dashboard.unreadNewsItems

  return sourceItems.map((item) => ({
    id: item.id,
    title: item.title,
    message: item.message,
    actorUsername: item.actorUsername,
    questTitle: item.questTitle,
    createdAt: item.createdAt,
    typeLabel: formatQuestNewsType(item.type),
    badgeClass: questNewsBadgeClass(item.type),
    questId: item.questId,
    applicationId: item.applicationId,
    isUnread: item.readAt === null
  }))
})

const openQuest = async (item: {id: number; questId: number | null}) => {
  await props.dashboard.markNewsItemAsRead(item.id)
  props.dashboard.closeNotificationsDialog()

  if (item.questId !== null && props.dashboard.questForId(item.questId)) {
    await router.push(`/quests/${item.questId}`)
    return
  }

  await router.push("/quests")
}

const openApplication = async (item: {id: number; applicationId: number | null; questId: number | null}) => {
  if (item.applicationId === null) {
    return
  }

  await props.dashboard.markNewsItemAsRead(item.id)
  props.dashboard.closeNotificationsDialog()
  await router.push({
    path: `/applications/${item.applicationId}`,
    query: item.questId !== null ? {questId: String(item.questId)} : undefined
  })
}
</script>

<template>
  <section class="stack dashboard-news-panel dashboard-news-panel--drawer">
    <div class="dashboard-news__titlebar u-row-between u-items-center u-gap-10">
      <h2 class="dashboard-news__title">Notifications</h2>

      <div class="button-row">
        <button class="button button--secondary dashboard-news__mark-all" type="button" @click="feedMode = 'unread'">
          Unread
        </button>
        <button class="button button--secondary dashboard-news__mark-all" type="button" @click="feedMode = 'all'">
          All recent
        </button>
        <button
          v-if="props.dashboard.unreadNewsCount > 0"
          class="button button--secondary dashboard-news__mark-all"
          type="button"
          @click="props.dashboard.markNewsAsRead()"
        >
          Clear all
        </button>
      </div>
    </div>

    <div v-if="props.dashboard.newsError" class="empty-state empty-state--error">
      {{ props.dashboard.newsError }}
    </div>

    <div v-else-if="feedItems.length" class="news-feed news-feed--dialog">
      <article
        v-for="item in feedItems"
        :key="item.id"
        class="news-item news-item--dialog dashboard-news__item"
      >
        <div class="news-item__top u-row-between u-items-center u-gap-10">
          <div class="news-item__badges">
            <span v-if="item.isUnread" class="badge badge--accent">Unread</span>
            <span :class="['badge', item.badgeClass]">{{ item.typeLabel }}</span>
          </div>
          <span class="news-item__time">{{ props.dashboard.formatDateTime(item.createdAt) }}</span>
        </div>

        <strong class="news-item__title">{{ item.title }}</strong>
        <p class="news-item__message">{{ item.message }}</p>

        <div class="news-item__footer u-row-between u-items-center u-wrap u-gap-10">
          <span class="news-item__meta">
            {{ item.actorUsername }}<template v-if="item.questTitle"> on {{ item.questTitle }}</template>
          </span>

          <div class="button-row">
            <button
              v-if="item.questId !== null"
              class="button button--secondary news-item__actions-cta"
              type="button"
              @click="openQuest(item)"
            >
              Open quest
            </button>
            <button
              v-if="item.applicationId !== null"
              class="button button--secondary news-item__actions-cta"
              type="button"
              @click="openApplication(item)"
            >
              Open application
            </button>
          </div>
        </div>
      </article>
    </div>

    <div v-else class="empty-state empty-state--soft">
      No notifications yet.
    </div>
  </section>
</template>
