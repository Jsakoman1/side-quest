<script setup lang="ts">
import DashboardSectionHeader from "./DashboardSectionHeader.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"
import {formatQuestNewsType, questNewsBadgeClass} from "../../shared/questNews.ts"

defineProps<{
  dashboard: QuestDashboard
}>()
</script>

<template>
  <article class="card overview-panel overview-panel--news overview-panel--compact">
    <DashboardSectionHeader
      title="Updates"
      subtitle="Recent activity across your quests and applications"
    >
      <template #stats>
        <span v-if="dashboard.unreadNewsCount > 0" class="badge badge--accent">
          Unread ({{ dashboard.unreadNewsCount }})
        </span>
        <button
          v-if="dashboard.unreadNewsCount > 0"
          type="button"
          class="button button--secondary"
          @click="dashboard.markNewsAsRead()"
        >
          Mark all read
        </button>
      </template>
    </DashboardSectionHeader>

    <div v-if="dashboard.newsError" class="empty-state empty-state--error mt-3">
      {{ dashboard.newsError }}
    </div>

    <div v-else-if="dashboard.recentNewsItems.length" class="news-feed mt-2">
      <div
        v-for="item in dashboard.recentNewsItems"
        :key="item.id"
        class="news-item"
        :class="{ 'news-item--unread': item.readAt === null }"
      >
        <div class="news-item__top">
          <div class="news-item__badges">
            <span v-if="item.readAt === null" class="badge badge--accent">Unread</span>
            <span :class="['badge', questNewsBadgeClass(item.type)]">
              {{ formatQuestNewsType(item.type) }}
            </span>
          </div>
          <span class="news-item__time">{{ dashboard.formatDateTime(item.createdAt) }}</span>
        </div>

        <strong class="news-item__title">{{ item.title }}</strong>
        <p class="news-item__message">{{ item.message }}</p>

        <div class="news-item__footer">
          <span class="news-item__meta">
            {{ item.actorUsername }} on {{ item.questTitle }}
          </span>

          <button
            v-if="dashboard.questForId(item.questId)"
            type="button"
            class="button button--secondary"
            @click="dashboard.markNewsItemAsRead(item.id); dashboard.clearOverviewFocus(); dashboard.openQuestDialog(item.questId)"
          >
            Open quest
          </button>
          <button
            v-if="item.readAt === null"
            type="button"
            class="button button--secondary"
            @click="dashboard.markNewsItemAsRead(item.id)"
          >
            Mark read
          </button>
        </div>
      </div>
    </div>

    <div v-else-if="dashboard.isLoadingNews" class="empty-state mt-3">
      Loading updates...
    </div>

    <div v-else class="empty-state mt-3">
      No updates yet.
    </div>
  </article>
</template>
