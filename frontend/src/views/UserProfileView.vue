<script setup lang="ts">
import {computed, onMounted, ref, watch} from "vue"
import {RouterLink, useRoute, useRouter} from "vue-router"
import ProfileAvatar from "../components/profile/ProfileAvatar.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"
import {currentUser, isAdmin} from "../auth.ts"
import {sidequestApi, type AppUser, type CircleRelation, type CircleRequest} from "../api/sidequestApi.ts"

const route = useRoute()
const router = useRouter()
const profile = ref<AppUser | null>(null)
const myCircles = ref<CircleRequest[]>([])
const incomingCircleRequests = ref<CircleRequest[]>([])
const outgoingCircleRequests = ref<CircleRequest[]>([])
const circleRelation = ref<CircleRelation | null>(null)
const isLoading = ref(false)
const error = ref("")
const copied = ref(false)
const circleRequestMessage = ref("")
const circleRequestTone = ref<"success" | "warning">("success")
const isSendingCircleRequest = ref(false)

const userId = computed(() => Number(route.params.id))
const currentUserId = computed(() => currentUser.value?.id ?? null)
const isOwnProfile = computed(() => currentUser.value?.id === profile.value?.id)
const isCircle = computed(() => {
  if (!profile.value || !currentUser.value) {
    return false
  }

  return myCircles.value.some((circle) => {
    const otherUserId = circle.requesterId === currentUserId.value ? circle.recipientId : circle.requesterId
    return otherUserId === profile.value?.id
  })
})
const hasIncomingRequestFromProfile = computed(() => incomingCircleRequests.value.some((request) => request.requesterId === profile.value?.id))
const hasOutgoingRequestToProfile = computed(() => outgoingCircleRequests.value.some((request) => request.recipientId === profile.value?.id))
const isBlocked = computed(() => circleRelation.value?.relationStatus === "BLOCKED")
const canUnblock = computed(() => !!(isBlocked.value && circleRelation.value?.blockedByCurrentUser))
const circleActionLabel = computed(() => {
  if (isBlocked.value) {
    if (canUnblock.value) {
      return "Unblock user"
    }

    return "Blocked"
  }

  if (isCircle.value) {
    return "Already in your circles"
  }

  if (hasOutgoingRequestToProfile.value) {
    return "Circle request sent"
  }

  if (hasIncomingRequestFromProfile.value) {
    return "Review in Circles"
  }

  return "Send circle invite"
})
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

const showCircleMessage = (message: string, tone: "success" | "warning" = "success") => {
  circleRequestMessage.value = message
  circleRequestTone.value = tone
  window.setTimeout(() => {
    if (circleRequestMessage.value === message) {
      circleRequestMessage.value = ""
    }
  }, 3500)
}

const loadCircleRelations = async () => {
  if (!currentUser.value || isOwnProfile.value) {
    myCircles.value = []
    incomingCircleRequests.value = []
    outgoingCircleRequests.value = []
    circleRelation.value = null
    return
  }

  const [circles, incoming, outgoing, relation] = await Promise.all([
    sidequestApi.getMyCircles(),
    sidequestApi.getIncomingCircleRequests(),
    sidequestApi.getOutgoingCircleRequests(),
    sidequestApi.getCircleRelation(profile.value?.id ?? 0)
  ])

  myCircles.value = circles
  incomingCircleRequests.value = incoming
  outgoingCircleRequests.value = outgoing
  circleRelation.value = relation
}

const sendCircleRequest = async () => {
  if (!profile.value) {
    return
  }

  isSendingCircleRequest.value = true
  try {
    await sidequestApi.createCircleRequest({recipientId: profile.value.id})
    showCircleMessage("Circle request sent.")
    await loadCircleRelations()
  } catch {
    showCircleMessage("Could not send circle request.", "warning")
  } finally {
    isSendingCircleRequest.value = false
  }
}

const blockProfile = async () => {
  if (!profile.value) {
    return
  }

  try {
    await sidequestApi.blockCircleUser({blockedUserId: profile.value.id})
    showCircleMessage("User blocked.")
    await loadCircleRelations()
  } catch {
    showCircleMessage("Could not block user.", "warning")
  }
}

const unblockProfile = async () => {
  if (!profile.value) {
    return
  }

  try {
    await sidequestApi.unblockCircleUser(profile.value.id)
    showCircleMessage("User unblocked.")
    await loadCircleRelations()
  } catch {
    showCircleMessage("Could not unblock user.", "warning")
  }
}

const handleCircleAction = () => {
  if (isBlocked.value && canUnblock.value) {
    void unblockProfile()
    return
  }

  if (hasIncomingRequestFromProfile.value) {
    void router.push("/circles")
    return
  }

  void sendCircleRequest()
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
    await loadCircleRelations()
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
        <button v-if="isOwnProfile" class="button" type="button" @click="router.push('/quests')">
          Edit profile
        </button>
      </div>
    </div>

    <div v-if="isLoading" class="empty-state">Loading profile...</div>
    <div v-else-if="error" class="alert alert--error">{{ error }}</div>

    <div v-else-if="profile" class="card profile-page">
      <UiStatusBanner :message="circleRequestMessage" :tone="circleRequestTone" />

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

      <div class="profile-page__cta">
        <button
          v-if="isOwnProfile && !isAdmin()"
          class="button button--secondary"
          type="button"
          @click="router.push('/quests')"
        >
          Open edit form
        </button>

        <button
          v-else-if="isBlocked && !canUnblock"
          class="button button--secondary"
          type="button"
          disabled
        >
          Blocked
        </button>

        <button
          v-else
          class="button"
          type="button"
          :disabled="isSendingCircleRequest || isCircle || hasOutgoingRequestToProfile || hasIncomingRequestFromProfile || (isBlocked && !canUnblock)"
          @click="handleCircleAction"
        >
          {{ circleActionLabel }}
        </button>

        <button
          v-if="!isOwnProfile && !isBlocked"
          class="button button--secondary"
          type="button"
          @click="blockProfile"
        >
          Block user
        </button>
      </div>
    </div>
  </div>
</template>
