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

const deleteAppUser = async (id) => {
  await axios.delete(`http://localhost:8080/app_users/${id}`)
  // refresh
  await fetchAppUsers()
}

// helper confirmDelete
const handleDelete = async (id) => {
  if (confirm('Delete AppUser?'))
    return await deleteAppUser(id)
}

// EditMode
const editingAppUserId = ref(null)
const editAppUserEmail = ref('')
const editAppUserUsername = ref('')

const startEdit = (appUser) => {
  editingAppUserId.value = appUser.id
  editAppUserEmail.value = appUser.email
  editAppUserUsername.value = appUser.username
}

const updateAppUser = async () => {
  await axios.put(`http://localhost:8080/app_users/${editingAppUserId.value}`, {
    email: editAppUserEmail.value,
    username: editAppUserUsername.value,
  })

  // reset fields and refresh
  editingAppUserId.value = null
  editAppUserEmail.value = ''
  editAppUserUsername.value = ''
  await fetchAppUsers()
}

const cancelEdit = () => {
  editingAppUserId.value = null
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

      <!-- NORMAL VIEW -->
      <div v-if="editingAppUserId !== appUser.id">
        {{ appUser.id }} - {{ appUser.email }} - {{ appUser.username }}
        <button @click="startEdit(appUser)">Edit</button>
        <button @click="handleDelete(appUser.id)">Delete AppUser</button>
      </div>

      <!-- EDIT MODE -->
      <div v-else>
        <input v-model="editAppUserEmail"/>
        <input v-model="editAppUserUsername"/>

        <button @click="updateAppUser">Save</button>
        <button @click="cancelEdit">Cancel</button>
      </div>
    </div>
  </div>
</template>