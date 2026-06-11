<script setup lang="ts">
defineProps<{
  dashboard: any
}>()
</script>

<template>
  <section class="stack">
    <article class="card">
      <div class="card__header">
        <div>
          <h2 class="card__title">Admin tools</h2>
          <p class="muted mt-2">Users live on the users page.</p>
        </div>
      </div>

      <div class="button-row">
        <RouterLink class="button button--secondary" to="/app-users">Open users page</RouterLink>
      </div>
    </article>

    <article class="card">
      <div class="section-heading">
        <div>
          <h2 class="section-title">All quests</h2>
          <p class="section-subtitle">Edit any quest.</p>
        </div>
      </div>

      <div class="segmented">
        <button
          v-for="option in dashboard.questStatusOptions"
          :key="option.value"
          type="button"
          :class="['segment', { 'segment--active': dashboard.adminQuestStatusFilter === option.value }]"
          @click="dashboard.adminQuestStatusFilter = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <div v-if="!dashboard.filteredAdminQuests.length" class="empty-state mt-4">
        No quests in this group.
      </div>

      <div v-else class="quest-list mt-4">
        <details v-for="quest in dashboard.filteredAdminQuests" :key="quest.id" class="compact-disclosure">
          <summary>
            <div class="compact-disclosure__summary">
              <div>
                <strong>{{ quest.title }}</strong>
                <div class="muted mt-1 text-clamp">{{ quest.description }}</div>
              </div>
              <div class="compact-disclosure__meta">
                <span :class="dashboard.statusBadgeClass(quest.status)">{{ dashboard.formatStatus(quest.status) }}</span>
                <span class="badge badge--accent">{{ quest.awardAmount }}</span>
              </div>
            </div>
          </summary>

          <div class="compact-disclosure__body">
            <div v-if="dashboard.editingQuestId !== quest.id">
              <div class="grid grid--auto">
                <div class="field">
                  <span class="label">Quest ID</span>
                  <strong>{{ quest.id }}</strong>
                </div>
                <div class="field">
                  <span class="label">Creator</span>
                  <strong>{{ quest.creatorUsername }}</strong>
                </div>
              </div>

              <div class="button-row mt-4">
                <button class="button button--secondary" type="button" @click="dashboard.startEditingQuest(quest)">Edit</button>
              </div>
            </div>

            <form v-else class="stack" @submit.prevent="dashboard.saveEditedQuest">
              <div class="card__header">
                <div>
                  <h3 class="card__title">Edit quest</h3>
                  <p class="muted mt-2">Change content, owner, or status.</p>
                </div>
                <span class="badge badge--accent">Editing</span>
              </div>

              <div class="grid grid--two">
                <label class="field">
                  <span class="label">Title</span>
                  <input v-model="dashboard.editQuestTitle" class="input" />
                </label>

                <label class="field">
                  <span class="label">Award amount</span>
                  <input v-model="dashboard.editQuestAwardAmount" class="input" inputmode="decimal" />
                </label>
              </div>

              <label class="field">
                <span class="label">Description</span>
                <textarea v-model="dashboard.editQuestDescription" class="textarea" />
              </label>

              <div class="grid grid--two">
                <label class="field">
                  <span class="label">Creator</span>
                  <select v-model="dashboard.editQuestCreatorId" class="input">
                    <option v-for="user in dashboard.appUsers" :key="user.id" :value="String(user.id)">
                      {{ user.username }} ({{ user.email }})
                    </option>
                  </select>
                </label>

                <label class="field">
                  <span class="label">Status</span>
                  <select v-model="dashboard.editQuestStatus" class="input">
                    <option value="OPEN">Open</option>
                    <option value="ASSIGNED">Assigned</option>
                    <option value="IN_PROGRESS">In progress</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </label>
              </div>

              <div class="button-row">
                <button class="button" type="submit">Save</button>
                <button class="button button--secondary" type="button" @click="dashboard.cancelEditingQuest">Cancel</button>
              </div>
            </form>
          </div>
        </details>
      </div>
    </article>
  </section>
</template>
