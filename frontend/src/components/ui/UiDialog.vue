<script setup lang="ts">
defineProps<{
  open: boolean
  title: string
  subtitle?: string
  leading?: string
  position?: "center" | "drawer"
  size?: "sm" | "md" | "lg" | "xl"
}>()

defineEmits<{
  (event: "close"): void
}>()
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      :class="['dialog-backdrop', { 'dialog-backdrop--drawer': position === 'drawer' }]"
      @click.self="$emit('close')"
    >
      <div :class="['dialog-panel card', { 'dialog-panel--drawer': position === 'drawer' }, size ? `dialog-panel--${size}` : '']">
        <div v-if="title || subtitle || $slots.actions" class="card__header u-row-between u-items-start u-gap-12">
          <div class="card__header-main">
            <span v-if="leading" class="card__header-leading">{{ leading }}</span>
            <h2 v-if="title" class="card__title card__title--dialog">{{ title }}</h2>
            <p v-if="subtitle" class="muted mt-2">{{ subtitle }}</p>
          </div>

          <div v-if="$slots.actions" class="card__header-actions">
            <slot name="actions" />
          </div>
        </div>

        <slot />
      </div>
    </div>
  </Teleport>
</template>
