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
          <h2 class="section-title">Find quests</h2>
          <p class="section-subtitle">Open quests from others.</p>
        </div>
      </div>

      <div v-if="!dashboard.availableQuests.length" class="empty-state">
        No open quests right now.
      </div>

      <div v-else class="quest-list mt-4">
        <details v-for="quest in dashboard.availableQuests" :key="quest.id" class="compact-disclosure">
          <summary>
            <div class="compact-disclosure__summary">
              <div>
                <strong>{{ quest.title }}</strong>
                <div class="muted mt-1 text-clamp">{{ quest.description }}</div>
              </div>
              <div class="compact-disclosure__meta">
                <span class="badge">{{ quest.awardAmount }}</span>
                <span class="badge badge--accent">By {{ quest.creatorUsername }}</span>
              </div>
            </div>
          </summary>

          <div class="compact-disclosure__body">
            <form v-if="!dashboard.hasAppliedToQuest(quest.id)" class="stack" @submit.prevent="dashboard.applyForQuest(quest.id)">
              <div class="split-actions">
                <div>
                  <h3 class="card__title quest-section-title">Apply</h3>
                  <p class="muted mt-1 mb-0">Short note and price.</p>
                </div>
                <button class="button button--secondary" type="submit">
                  Apply
                </button>
              </div>

              <div class="grid grid--two">
                <label class="field">
                  <span class="label">Message</span>
                  <input v-model="dashboard.applicationMessages[quest.id]" class="input" />
                </label>
                <label class="field">
                  <span class="label">Proposed price</span>
                  <input v-model="dashboard.proposedPrices[quest.id]" class="input" inputmode="decimal" />
                </label>
              </div>
            </form>

            <div v-else class="compact-disclosure__body-note">
              Application sent. Check Applied tasks.
            </div>
          </div>
        </details>
      </div>
    </div>
  </section>
</template>
