<script setup lang="ts">
interface AppUser {
  id: number
  email: string
  username: string
  role: string
}

defineProps<{
  users: AppUser[]
  editingUserId: number | null
  editEmail: string
  editUsername: string
}>()

defineEmits<{
  (event: "edit", user: AppUser): void
  (event: "delete", id: number): void
  (event: "save"): void
  (event: "cancel"): void
  (event: "update:editEmail", value: string): void
  (event: "update:editUsername", value: string): void
}>()
</script>

<template>
  <div class="quest-list">
    <article v-for="user in users" :key="user.id" class="card">
      <div v-if="editingUserId !== user.id" class="split-actions">
        <div>
          <strong>{{ user.username }}</strong>
          <div class="muted mt-1">{{ user.email }}</div>
          <div class="badge badge--accent mt-2">{{ user.role }}</div>
        </div>

        <div class="button-row">
          <button class="button button--secondary" type="button" @click="$emit('edit', user)">Edit</button>
          <button class="button button--danger" type="button" @click="$emit('delete', user.id)">Delete</button>
        </div>
      </div>

      <form v-else class="stack" @submit.prevent="$emit('save')">
        <div class="grid grid--two">
          <label class="field">
            <span class="label">Email</span>
            <input
              :value="editEmail"
              class="input"
              @input="$emit('update:editEmail', ($event.target as HTMLInputElement).value)"
            />
          </label>

          <label class="field">
            <span class="label">Username</span>
            <input
              :value="editUsername"
              class="input"
              @input="$emit('update:editUsername', ($event.target as HTMLInputElement).value)"
            />
          </label>
        </div>

        <div class="button-row">
          <button class="button" type="submit">Save</button>
          <button class="button button--secondary" type="button" @click="$emit('cancel')">Cancel</button>
        </div>
      </form>
    </article>
  </div>
</template>
