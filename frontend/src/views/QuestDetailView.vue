<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import axios from "axios";
import {authHeader, currentUser} from "../auth.ts";

interface Quest {
  id: number
  creatorId: number
  creatorUsername: string
  title: string
  description: string
  awardAmount: number
  status: string
}

const route = useRoute()
const router = useRouter()
const quest = ref<Quest | null>(null)
const error = ref('')

const questId = computed(() => Number(route.params.id))
const isOwner = computed(() => {
  if (!quest.value || !currentUser.value) {
    return false
  }
  return quest.value.creatorId === currentUser.value.id
})

const fetchQuest = async () => {
  error.value = ''
  try {
    const res = await axios.get<Quest>(`http://localhost:8080/quests/${questId.value}`, {
      headers: authHeader()
    })
    quest.value = res.data
  } catch (e) {
    error.value = 'Quest not found'
  }
}

const startQuest = async () => {
  await axios.patch(`http://localhost:8080/quests/${questId.value}/start`, {}, {
    headers: authHeader()
  })
  await fetchQuest()
}

const completeQuest = async () => {
  await axios.patch(`http://localhost:8080/quests/${questId.value}/complete`, {}, {
    headers: authHeader()
  })
  await fetchQuest()
}

const cancelQuest = async () => {
  await axios.patch(`http://localhost:8080/quests/${questId.value}/cancel`, {}, {
    headers: authHeader()
  })
  await fetchQuest()
}

onMounted(() => {
  fetchQuest()
})
</script>

<template>
  <div>
    <button @click="router.push('/quests')">Back to quests</button>

    <p v-if="error">{{ error }}</p>

    <div v-if="quest" style="border: 1px solid #ccc; padding: 16px; margin-top: 16px">
      <h1>{{ quest.title }}</h1>
      <div>ID: {{ quest.id }}</div>
      <div>Creator: {{ quest.creatorUsername }}</div>
      <div>Description: {{ quest.description }}</div>
      <div>Award: {{ quest.awardAmount }}</div>
      <div>Status: {{ quest.status }}</div>

      <div v-if="isOwner" style="margin-top: 16px">
        <button v-if="quest.status === 'ASSIGNED'" @click="startQuest">Start</button>
        <button v-if="quest.status === 'IN_PROGRESS'" @click="completeQuest">Complete</button>
        <button v-if="quest.status !== 'COMPLETED' && quest.status !== 'CANCELLED'" @click="cancelQuest">Cancel</button>
      </div>
    </div>
  </div>
</template>
