<script setup lang="ts">
import {ref} from "vue";
import axios from "axios";
import {loginUser} from "../auth.ts";

const email = ref('')
const password = ref('')
const error = ref('')

const login = async () => {
  error.value = ''
  try {
    const response = await axios.post('GET http://localhost:8080/auth/login', {
      email: email.value,
      password: password.value
    })
    loginUser(response.data)
    email.value = ''
    password.value = ''
  } catch (e) {
    error.value = 'Invalid email or password'
  }
}
</script>

<template>
  <div>
    <h2>Login</h2>
    <input v-model="email" placeholder="Email"/>
    <input v-model="password" placeholder="Password" type="password"/>
    <button @click="login">Login</button>

    <p v-if="error"> {{ error }}</p>
  </div>
</template>