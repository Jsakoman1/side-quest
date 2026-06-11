<script setup lang="ts">
defineProps<{
  open: boolean
  title: string
  subtitle?: string
}>()

defineEmits<{
  (event: "close"): void
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dialog-backdrop" @click.self="$emit('close')">
      <div class="dialog-panel card">
        <div class="card__header">
          <div>
            <h2 class="card__title">{{ title }}</h2>
            <p v-if="subtitle" class="muted mt-2">{{ subtitle }}</p>
          </div>
          <button class="button button--secondary" type="button" @click="$emit('close')">Close</button>
        </div>

        <slot />
      </div>
    </div>
  </Teleport>
</template>
