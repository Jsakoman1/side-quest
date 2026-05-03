<script setup>
import {onMounted, ref} from 'vue'
import axios from 'axios'

const users = ref([])

onMounted(async () => {
  const res = await axios.get('http://localhost:8080/app_users')
  users.value = res.data
})
</script>

<template>
  <div>
    <h1>Users</h1>

    <div v-for="user in users" :key="user.id"> {{ user.id }} - {{ user.email }} - {{ user.username }}</div>
  </div>
</template>