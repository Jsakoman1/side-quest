<script setup>
import {onMounted, ref} from 'vue'
import axios from 'axios'

const appUsers = ref([])

// Form
const email = ref('')
const username = ref('')

// FETCH USERS
const fetchAppUsers = async () => {
  const res = await axios.get('http://localhost:8080/app_users')
  appUsers.value = res.data
}

// CREATE APP USER
const createAppUser = async () => {
  await axios.post('http://localhost:8080/app_users', {
    email: email.value,
    username: username.value
  })

  // reset inputs and refresh
  email.value = ''
  username.value = ''
  await fetchAppUsers()
}

onMounted(() => {
  fetchAppUsers()
})
</script>

<template>
  <div>
    <h1>App Users</h1>

    <!-- CREATE APP USER FORM -->
    <div style="margin-bottom: 20px">
      <input v-model="email" placeholder="email"/>
      <input v-model="username" placeholder="username"/>

      <button @click="createAppUser">Create AppUser</button>
    </div>

    <!-- LIST APP USERS -->
    <div v-for="appUser in appUsers" :key="appUser.id">
      {{ appUser.id }} - {{ appUser.email }} - {{ appUser.username }}
    </div>
  </div>
</template>