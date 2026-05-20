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

interface QuestApplication {
  id: number
  questId: number
  questTitle: string
  applicantId: number
  applicantUsername: string
  message: string
  proposedPrice: number
  status: string
  createdAt: string
}

const quests = ref<Quest[]>([])
const appUsers = ref<AppUser[]>([]) // used for AppUsers dropdown in CreateMode

// Form fields
const creatorId = ref<number | ''>('')
const title = ref('')
const description = ref('')
const awardAmount = ref('')

// Form Applications
const selectedApplicantIds = ref<Record<number, number | ''>>({})
const applicationMessages = ref<Record<number, string>>({})
const proposedPrices = ref<Record<number, string>>({})
const applicationsByQuestId = ref<Record<number, QuestApplication[]>>({})
const openApplicationsQuestIds = ref<Record<number, boolean>>({})

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

// SHOW QUEST APPLICATIONS
const toggleApplicationsForQuest = async (questId: number) => {
  if (openApplicationsQuestIds.value[questId]) {
    openApplicationsQuestIds.value[questId] = false
    return
  }

  await refreshApplicationsForQuest(questId)
  openApplicationsQuestIds.value[questId] = true
}

// APPLY FOR QUEST
const applyForQuest = async (questId: number) => {
  const applicantId = selectedApplicantIds.value[questId]

  if (applicantId === '' || applicantId === undefined) {
    alert('Please select applicant')
    return
  }

  await axios.post(`http://localhost:8080/quests/${questId}/applications`, {
    applicantId: applicantId,
    message: applicationMessages.value[questId] ?? '',
    proposedPrice: Number(proposedPrices.value[questId] ?? 0)
  })

  selectedApplicantIds.value[questId] = ''
  applicationMessages.value[questId] = ''
  proposedPrices.value[questId] = ''
  await refreshApplicationsForQuest(questId)
  openApplicationsQuestIds.value[questId] = true
}

// REFRESH APPLICATIONS FOR QUEST
const refreshApplicationsForQuest = async (questId: number) => {
  const res = await axios.get<QuestApplication[]>(`http://localhost:8080/quests/${questId}/applications`)
  applicationsByQuestId.value[questId] = res.data
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
    <div v-for="quest in quests" :key="quest.id" style="border: 1px solid #ccc; padding: 12px; margin-bottom: 12px">
      <div>
        <strong>{{ quest.id }} - {{ quest.title }} </strong>
      </div>
      <div>
        {{ quest.description }}
      </div>
      <div>
        Award: {{ quest.awardAmount }}
      </div>
      <div>
        Status: {{ quest.status }}
      </div>
      <div>
        Creator: {{ quest.creatorUsername }}
      </div>
      <hr/>

      <h4>Apply for this quest</h4>
      <select v-model="selectedApplicantIds[quest.id]">
        <option disabled value="">Select Applicant</option>
        <option v-for="appUser in appUsers" :key="appUser.id" :value="appUser.id">
          {{ appUser.username }} - {{ appUser.email }}
        </option>
      </select>
      <input v-model="applicationMessages[quest.id]" placeholder="application message"/>
      <input v-model="proposedPrices[quest.id]" placeholder="proposedPrice"/>
      <button @click="applyForQuest(quest.id)">Apply</button>

      <button @click="toggleApplicationsForQuest(quest.id)">
        {{ openApplicationsQuestIds[quest.id] ? 'Hide Applications' : 'Show Applications' }}
      </button>
      <div v-if="openApplicationsQuestIds[quest.id]">
        <h4>Applications</h4>

        <div v-if="!applicationsByQuestId[quest.id]?.length">
          No applications yet.
        </div>

        <div v-for="application in applicationsByQuestId[quest.id]" :key="application.id"
             style="border-left: 4px solid #42b883; background: #f7fff9; padding: 8px 12px; margin-top: 8px">
          <div>Applicant: {{ application.applicantUsername }}</div>
          <div>Message: {{ application.message }}</div>
          <div>Proposed Price: {{ application.proposedPrice }}</div>
          <div>Status: {{ application.status }}</div>
        </div>
      </div>
      <div>
      </div>
    </div>
  </div>
</template>