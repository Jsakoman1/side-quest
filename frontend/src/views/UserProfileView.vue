<script setup lang="ts">
import {computed, onMounted, ref, watch} from "vue"
import {RouterLink, useRoute, useRouter} from "vue-router"
import ProfileAvatar from "../components/profile/ProfileAvatar.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import {currentUser, isAdmin} from "../auth.ts"
import {sidequestApi, type AppUser} from "../api/sidequestApi.ts"

const route = useRoute()
const router = useRouter()
const profile = ref<AppUser | null>(null)
const isLoading = ref(false)
const error = ref("")
const copied = ref(false)

const userId = computed(() => Number(route.params.id))
const isOwnProfile = computed(() => currentUser.value?.id === profile.value?.id)
const profileLink = computed(() => `${window.location.origin}/users/${profile.value?.id ?? userId.value}`)

const copyProfileLink = async () => {
  if (!profile.value) {
    return
  }

  await navigator.clipboard.writeText(profileLink.value)
  copied.value = true
  window.setTimeout(() => {
    copied.value = false
  }, 1500)
}

const fetchProfile = async () => {
  if (!Number.isFinite(userId.value)) {
    error.value = "Invalid profile."
    profile.value = null
    return
  }

  isLoading.value = true
  error.value = ""

  try {
    profile.value = await sidequestApi.getAppUser(userId.value)
  } catch {
    profile.value = null
    error.value = "Profile not found."
  } finally {
    isLoading.value = false
  }
}

watch(() => route.params.id, () => {
  void fetchProfile()
})

onMounted(() => {
  void fetchProfile()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Profile</h1>
        <p class="page-subtitle">Public profile card for quests and applications.</p>
      </div>

      <div class="button-row">
        <button class="button button--secondary" type="button" @click="router.back()">Back</button>
        <button class="button button--secondary" type="button" @click="copyProfileLink">
          {{ copied ? "Link copied" : "Copy link" }}
        </button>
        <button v-if="isOwnProfile" class="button" type="button" @click="router.push('/quests?tab=profile')">
          Edit profile
        </button>
      </div>
    </div>

    <div v-if="isLoading" class="empty-state">Loading profile...</div>
    <div v-else-if="error" class="alert alert--error">{{ error }}</div>

    <div v-else-if="profile" class="card profile-page">
      <div class="profile-page__header">
        <ProfileAvatar :username="profile.username" :avatar-data-url="profile.profileAvatarDataUrl" :size="108" />
        <div class="stack">
          <h2 class="card__title">{{ profile.username }}</h2>
          <div class="muted">{{ profile.role }}</div>
          <div class="muted">{{ profile.email }}</div>
        </div>
      </div>

      <div class="field">
        <span class="label">About</span>
        <ProfileBio :text="profile.profileDescription" placeholder="This user has not added a profile description yet." />
      </div>

      <div class="profile-page__notice">
        <strong>Visible on SideQuest</strong>
        <p class="muted mb-0">
          This is the profile other people see when they open your quests or applications.
        </p>
      </div>

      <div class="profile-stats">
        <div class="profile-stat">
          <span class="label">Open jobs</span>
          <strong>{{ profile.openQuestCount }}</strong>
        </div>
      </div>

      <div class="profile-open-quests">
        <div class="field">
          <span class="label">Open jobs you can apply for</span>
        </div>

        <div v-if="profile.openQuests.length" class="stack">
          <article v-for="quest in profile.openQuests" :key="quest.id" class="profile-open-quest">
            <div class="profile-open-quest__top">
              <div class="stack">
                <strong>{{ quest.title }}</strong>
                <div class="muted">$ {{ quest.awardAmount }}</div>
              </div>
              <span class="badge badge--accent">Open</span>
            </div>
            <p class="profile-open-quest__description">{{ quest.description }}</p>
            <div class="button-row">
              <RouterLink class="button button--secondary" :to="`/quests/${quest.id}`">
                View job
              </RouterLink>
            </div>
          </article>
        </div>

        <div v-else class="empty-state">
          No open jobs right now.
        </div>
      </div>

      <div v-if="isOwnProfile && !isAdmin()" class="profile-page__cta">
        <button class="button button--secondary" type="button" @click="router.push('/quests?tab=profile')">
          Open edit form
        </button>
      </div>
    </div>
  </div>
</template>
