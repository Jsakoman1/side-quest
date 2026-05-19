<script setup>

import {onMounted, ref} from "vue";
import axios from "axios";

const quests = ref([])
const appUsers = ref([]) // used for AppUsers dropdown in CreateMode

// Form fields
const creatorId = ref('')
const title = ref('')
const description = ref('')
const awardAmount = ref('')

// FETCH QUESTS
const fetchQuests = async () => {
  const res = await axios.get('http://localhost:8080/quests')
  quests.value = res.data
}

// FETCH APP USERS
const fetchAppUsers = async () => {
  const res = await axios.get('http://localhost:8080/app_users')
  appUsers.value = res.data
}

// CREATE QUEST
const createQuest = async () => {
  await axios.post('http://localhost:8080/quests', {
    creatorId: Number(creatorId.value),
    title: title.value,
    description: description.value,
    awardAmount: awardAmount.value
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
      {{ quest.id }} - {{ quest.title }} - {{ quest.description }} - {{ quest.status }} - by {{ quest.creatorUsername }}
    </div>
  </div>

</template>