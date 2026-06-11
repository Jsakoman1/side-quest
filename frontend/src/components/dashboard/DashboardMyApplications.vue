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
          <h2 class="section-title">My applications</h2>
          <p class="section-subtitle">Applications you sent.</p>
        </div>
      </div>

      <div v-if="!dashboard.sortedMyApplications.length" class="empty-state">
        No applications yet.
      </div>

      <div v-else class="quest-list mt-4">
        <details v-for="application in dashboard.sortedMyApplications" :key="application.id" class="compact-disclosure">
          <summary>
            <div class="compact-disclosure__summary">
              <div>
                <strong>{{ application.questTitle }}</strong>
                <div class="muted mt-1 text-clamp">{{ application.questDescription }}</div>
              </div>
              <div class="compact-disclosure__meta">
                <span class="badge">{{ application.proposedPrice }}</span>
                <span :class="dashboard.statusBadgeClass(application.status)">{{ dashboard.formatStatus(application.status) }}</span>
              </div>
            </div>
          </summary>

          <div class="compact-disclosure__body">
            <div v-if="dashboard.editingApplicationId === application.id" class="stack">
              <div class="grid grid--two">
                <label class="field">
                  <span class="label">Message</span>
                  <textarea v-model="dashboard.editApplicationMessage" class="textarea" />
                </label>

                <label class="field">
                  <span class="label">Proposed price</span>
                  <input v-model="dashboard.editApplicationPrice" class="input" inputmode="decimal" />
                </label>
              </div>

              <div class="button-row">
                <button class="button" type="button" @click="dashboard.saveEditedApplication(application.questId)">Save</button>
                <button class="button button--secondary" type="button" @click="dashboard.cancelEditingApplication">Cancel</button>
              </div>
            </div>

            <div v-else class="stack">
              <p class="muted mt-0 mb-0 text-clamp">{{ application.message }}</p>
              <div v-if="application.status === 'PENDING'" class="button-row">
                <button class="button button--secondary" type="button" @click="dashboard.startEditingApplication(application)">
                  Edit
                </button>
              </div>
            </div>
          </div>
        </details>
      </div>
    </div>
  </section>
</template>
