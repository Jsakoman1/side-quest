<script setup lang="ts">

import {onMounted, ref} from "vue";
import axios from "axios";

interface Quest {
  id: number
  creatorId: number
  creatorUsername: string
  title: string
  description: string
  awardAmount: number
  status: string
}

interface AppUser {
  id: number
  email: string
  username: string
}

const quests = ref<Quest[]>([])
const appUsers = ref<AppUser[]>([]) // used for AppUsers dropdown in CreateMode

// Form fields
const creatorId = ref<number | ''>('')
const title = ref('')
const description = ref('')
const awardAmount = ref('')

// FETCH QUESTS
const fetchQuests = async () => {
  const res = await axios.get<Quest[]>('http://localhost:8080/quests')
  quests.value = res.data
}

// FETCH APP USERS
const fetchAppUsers = async () => {
  const res = await axios.get<AppUser[]>('http://localhost:8080/app_users')
  appUsers.value = res.data
}

// CREATE QUEST
const createQuest = async () => {
  if (creatorId.value === '') {
    alert('Please select creator')
    return
  }

  await axios.post('http://localhost:8080/quests', {
    creatorId: creatorId.value,
    title: title.value,
    description: description.value,
    awardAmount: Number(awardAmount.value)
  })

  // reset inputs and refresh
  creatorId.value = ''
  title.value = ''
  description.value = ''
  awardAmount.value = ''
  await fetchQuests()
}

onMounted(() => {
  fetchQuests()
  fetchAppUsers()
})
</script>

<template>
  <div>
    <h1>Quests</h1>

    <!-- CREATE QUEST FORM -->
    <div style="margin-bottom: 20px">
      <select v-model="creatorId">
        <option disabled value="">Select Creator</option>
        <option
            v-for="appUser in appUsers"
            :key="appUser.id"
            :value="appUser.id"
        >{{ appUser.username }} - {{ appUser.email }}
        </option>
      </select>
      <input v-model="title" placeholder="title"/>
      <input v-model="description" placeholder="description"/>
      <input v-model="awardAmount" placeholder="award amount"/>

      <button @click="createQuest">Create Quest</button>
    </div>

    <!-- LIST QUESTS -->
    <div v-for="quest in quests" :key="quest.id">
      {{ quest.id }} -
      {{ quest.title }} -
      {{ quest.description }} -
      {{ quest.awardAmount }} -
      {{ quest.status }}-
      creator: {{ quest.creatorUsername }}
    </div>
  </div>

</template>