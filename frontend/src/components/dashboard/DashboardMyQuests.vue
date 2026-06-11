<script setup lang="ts">
defineProps<{
  dashboard: any
}>()
</script>

<template>
  <section class="stack">
    <div class="card">
      <div class="section-heading">
        <div>
          <h2 class="section-title">My quests</h2>
          <p class="section-subtitle">Filter by status.</p>
        </div>
      </div>

      <div class="segmented">
        <button
          v-for="option in dashboard.questStatusOptions"
          :key="option.value"
          type="button"
          :class="['segment', { 'segment--active': dashboard.myQuestStatusFilter === option.value }]"
          @click="dashboard.myQuestStatusFilter = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <div v-if="!dashboard.filteredMyQuests.length" class="empty-state">
        No quests here yet.
      </div>

      <div v-else class="quest-list mt-4">
        <details v-for="quest in dashboard.filteredMyQuests" :key="quest.id" class="compact-disclosure">
          <summary>
            <div class="compact-disclosure__summary">
              <div>
                <strong>{{ quest.title }}</strong>
                <div class="muted mt-1 text-clamp">{{ quest.description }}</div>
              </div>
              <div class="compact-disclosure__meta">
                <span class="badge">{{ dashboard.formatStatus(quest.status) }}</span>
                <span class="badge">{{ quest.awardAmount }}</span>
                <button class="button button--secondary debug-copy" type="button" @click.stop="dashboard.toggleApplicationsForQuest(quest.id)">
                  {{ dashboard.openApplicationsQuestIds[quest.id] ? "Hide applications" : "Show applications" }}
                </button>
              </div>
            </div>
          </summary>

          <div class="compact-disclosure__body">
            <div class="button-row mt-4">
              <button
                v-if="quest.status === 'ASSIGNED'"
                class="button button--secondary"
                type="button"
                @click="dashboard.updateQuestStatus(quest.id, 'start')"
              >
                Start
              </button>
              <button
                v-if="quest.status === 'IN_PROGRESS'"
                class="button button--secondary"
                type="button"
                @click="dashboard.updateQuestStatus(quest.id, 'complete')"
              >
                Complete
              </button>
              <button
                v-if="quest.status !== 'COMPLETED' && quest.status !== 'CANCELLED'"
                class="button button--danger"
                type="button"
                @click="dashboard.updateQuestStatus(quest.id, 'cancel')"
              >
                Cancel
              </button>
            </div>

            <div class="split-actions">
              <div>
                <h3 class="card__title quest-section-title">Applications</h3>
              </div>
            </div>

            <div v-if="dashboard.openApplicationsQuestIds[quest.id]" class="stack mt-4">
              <div v-if="!dashboard.visibleApplicationsForQuest(quest.id).length" class="empty-state">
                <span v-if="quest.status === 'CANCELLED' && !dashboard.showAllApplicationsQuestIds[quest.id]">Applications hidden.</span>
                <span v-else>Nothing here yet.</span>
              </div>

              <details
                v-for="application in dashboard.visibleApplicationsForQuest(quest.id)"
                :key="application.id"
                class="compact-disclosure"
              >
                <summary>
                  <div class="compact-disclosure__summary">
                    <div>
                      <strong>{{ application.applicantUsername }}</strong>
                      <div class="muted mt-1">Price {{ application.proposedPrice }}</div>
                    </div>
                    <div class="compact-disclosure__meta">
                      <span :class="dashboard.statusBadgeClass(application.status)">{{ dashboard.formatStatus(application.status) }}</span>
                    </div>
                  </div>
                </summary>

                <div class="compact-disclosure__body">
                  <p class="muted mt-0 mb-0">{{ application.message }}</p>

                  <div v-if="application.status === 'PENDING'" class="button-row mt-4">
                    <button class="button button--secondary" type="button" @click="dashboard.acceptApplication(quest.id, application.id)">
                      Accept
                    </button>
                    <button class="button button--danger" type="button" @click="dashboard.rejectApplication(quest.id, application.id)">
                      Reject
                    </button>
                  </div>
                </div>
              </details>

              <div v-if="dashboard.shouldShowApplicationReveal(quest.id)" class="button-row">
                <button class="button button--secondary" type="button" @click="dashboard.toggleApplicationRevealForQuest(quest.id)">
                  {{ dashboard.applicationRevealLabel(quest.id) }}
                </button>
              </div>
            </div>
          </div>
        </details>
      </div>
    </div>
  </section>
</template>
