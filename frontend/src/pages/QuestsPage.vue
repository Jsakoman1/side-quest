<script setup lang="ts">

import {onMounted, ref} from "vue";
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

// Form fields
const title = ref('')
const description = ref('')
const awardAmount = ref('')

// Form Applications
const applicationMessages = ref<Record<number, string>>({})
const proposedPrices = ref<Record<number, string>>({})
const applicationsByQuestId = ref<Record<number, QuestApplication[]>>({})
const openApplicationsQuestIds = ref<Record<number, boolean>>({})

// FETCH QUESTS
const fetchQuests = async () => {
  const res = await axios.get<Quest[]>('http://localhost:8080/quests', {
    headers: authHeader()
  })
  quests.value = res.data
}

// CREATE QUEST
const createQuest = async () => {
  if (!currentUser.value) {
    alert('You must be logged in')
    return
  }

  await axios.post('http://localhost:8080/quests', {
    title: title.value,
    description: description.value,
    awardAmount: Number(awardAmount.value)
  }, {
    headers: authHeader()
  })

  // reset inputs and refresh
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
  if (!currentUser.value) {
    alert('You must be logged in')
    return
  }

  await axios.post(`http://localhost:8080/quests/${questId}/applications`, {
    message: applicationMessages.value[questId] ?? '',
    proposedPrice: Number(proposedPrices.value[questId] ?? 0)
  }, {
    headers: authHeader()
  })

  applicationMessages.value[questId] = ''
  proposedPrices.value[questId] = ''
  await refreshApplicationsForQuest(questId)
  openApplicationsQuestIds.value[questId] = true
}

// REFRESH APPLICATIONS FOR QUEST
const refreshApplicationsForQuest = async (questId: number) => {
  const res = await axios.get<QuestApplication[]>(`http://localhost:8080/quests/${questId}/applications`, {
    headers: authHeader()
  })
  applicationsByQuestId.value[questId] = res.data
}

onMounted(() => {
  fetchQuests()
})
</script>

<template>
  <div>
    <h1>Quests</h1>

    <!-- CREATE QUEST FORM -->
    <div style="margin-bottom: 20px">
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