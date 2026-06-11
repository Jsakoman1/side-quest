<script setup lang="ts">
defineProps<{
  email: string
  username: string
}>()

defineEmits<{
  (event: "update:email", value: string): void
  (event: "update:username", value: string): void
  (event: "submit"): void
}>()
</script>

<template>
  <form class="stack" @submit.prevent="$emit('submit')">
    <div class="grid grid--two">
      <label class="field">
        <span class="label">Email</span>
        <input :value="email" class="input" @input="$emit('update:email', ($event.target as HTMLInputElement).value)" />
      </label>

      <label class="field">
        <span class="label">Username</span>
        <input :value="username" class="input" @input="$emit('update:username', ($event.target as HTMLInputElement).value)" />
      </label>
    </div>

    <div class="button-row">
      <button class="button" type="submit">Create user</button>
    </div>
  </form>
</template>
