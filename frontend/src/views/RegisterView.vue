<script setup lang="ts">
import {ref} from "vue";
import axios from "axios";
import {loginUser} from "../auth.ts";

const email = ref('')
const username = ref('')
const password = ref('')
const error = ref('')

const register = async () => {
  error.value = ''
  try {
    const response = await axios.post('http://localhost:8080/auth/register', {
      email: email.value,
      username: username.value,
      password: password.value
    })
    loginUser(response.data)

    email.value = ''
    username.value = ''
    password.value = ''
  } catch (e) {
    error.value = 'Registration failed'
  }
}
</script>

<template>
  <div>
    <h2>Register</h2>
    <input v-model="email" placeholder="Email"/>
    <input v-model="username" placeholder="Username"/>
    <input v-model="password" placeholder="Password" type="password"/>
    <button @click="register">Register</button>

    <p v-if="error">{{ error }}</p>
  </div>
</template>