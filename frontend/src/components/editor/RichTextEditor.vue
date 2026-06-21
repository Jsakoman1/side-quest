<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from "vue"
import {richTextHasContent, sanitizeRichTextHtml} from "../../shared/richText.ts"
import {renderProfileText} from "../../shared/profileFormatting.ts"
import {compressImageFile} from "../../shared/imageCompression.ts"

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  toolbarLabel?: string
}>(), {
  modelValue: "",
  placeholder: "Write something...",
  toolbarLabel: "Text tools",
})

const emit = defineEmits<{
  (event: "update:modelValue", value: string): void
}>()

const editorRef = ref<HTMLElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)
const isFocused = ref(false)
const savedRange = ref<Range | null>(null)

const isEmpty = computed(() => !richTextHasContent(props.modelValue))

const syncEditor = () => {
  if (!editorRef.value) {
    return
  }

  const html = props.modelValue.trim() ? renderProfileText(props.modelValue) : ""
  if (editorRef.value.innerHTML !== html) {
    editorRef.value.innerHTML = html
  }
}

const saveSelection = () => {
  const selection = window.getSelection()
  const editor = editorRef.value
  if (!selection || !editor || selection.rangeCount === 0) {
    return
  }

  const range = selection.getRangeAt(0)
  if (!editor.contains(range.commonAncestorContainer)) {
    return
  }

  savedRange.value = range.cloneRange()
}

const restoreSelection = () => {
  const editor = editorRef.value
  const range = savedRange.value
  if (!editor || !range) {
    return false
  }

  const selection = window.getSelection()
  if (!selection) {
    return false
  }

  selection.removeAllRanges()
  selection.addRange(range)
  editor.focus()
  return true
}

const emitValue = () => {
  if (!editorRef.value) {
    return
  }

  const html = sanitizeRichTextHtml(editorRef.value.innerHTML)
  if (richTextHasContent(html)) {
    emit("update:modelValue", html)
    return
  }

  editorRef.value.innerHTML = ""
  emit("update:modelValue", "")
}

const execute = (command: string, value?: string) => {
  restoreSelection()
  document.execCommand(command, false, value)
  emitValue()
  saveSelection()
}

const promptForLink = () => {
  const href = window.prompt("Link URL", "https://")
  if (!href) {
    return
  }

  restoreSelection()
  document.execCommand("createLink", false, href.trim())
  emitValue()
  saveSelection()
}

const insertImage = async (file: File | null) => {
  if (!file) {
    return
  }

  try {
    const imageDataUrl = await compressImageFile(file, 1400, 0.86)
    restoreSelection()
    document.execCommand("insertHTML", false, `<img src="${imageDataUrl}" alt="${file.name.replaceAll("\"", "&quot;")}" loading="lazy">`)
    emitValue()
    saveSelection()
  } catch {
    // Keep the editor untouched on image failures.
  }
}

const handleInput = () => {
  emitValue()
}

const handlePaste = (event: ClipboardEvent) => {
  event.preventDefault()
  const text = event.clipboardData?.getData("text/plain") ?? ""
  document.execCommand("insertText", false, text)
  emitValue()
}

const handleToolbarMouseDown = () => {
  saveSelection()
}

const handleSelectionChange = () => {
  if (!isFocused.value) {
    return
  }

  saveSelection()
}

watch(() => props.modelValue, () => {
  if (isFocused.value) {
    return
  }

  void nextTick(syncEditor)
}, {immediate: true})

onMounted(() => {
  syncEditor()
  document.addEventListener("selectionchange", handleSelectionChange)
})

onBeforeUnmount(() => {
  document.removeEventListener("selectionchange", handleSelectionChange)
})
</script>

<template>
  <div class="rich-text-editor">
    <div class="rich-text-editor__toolbar" :aria-label="toolbarLabel">
      <button class="rich-text-editor__button" type="button" @mousedown.prevent="handleToolbarMouseDown" @click="execute('bold')">
        Bold
      </button>
      <button class="rich-text-editor__button" type="button" @mousedown.prevent="handleToolbarMouseDown" @click="execute('italic')">
        Italic
      </button>
      <button class="rich-text-editor__button" type="button" @mousedown.prevent="handleToolbarMouseDown" @click="execute('underline')">
        Underline
      </button>
      <button class="rich-text-editor__button" type="button" @mousedown.prevent="handleToolbarMouseDown" @click="execute('insertUnorderedList')">
        Bullets
      </button>
      <button class="rich-text-editor__button" type="button" @mousedown.prevent="handleToolbarMouseDown" @click="execute('insertOrderedList')">
        Numbered
      </button>
      <button class="rich-text-editor__button" type="button" @mousedown.prevent="handleToolbarMouseDown" @click="promptForLink">
        Link
      </button>
      <button class="rich-text-editor__button" type="button" @mousedown.prevent="handleToolbarMouseDown" @click="imageInputRef?.click()">
        Image
      </button>
    </div>

    <div
      ref="editorRef"
      class="rich-text-editor__surface"
      :class="{ 'rich-text-editor__surface--empty': isEmpty }"
      contenteditable="true"
      spellcheck="true"
      :data-placeholder="placeholder"
      @focus="isFocused = true"
      @blur="isFocused = false"
      @mouseup="saveSelection"
      @keyup="saveSelection"
      @input="handleInput"
      @paste="handlePaste"
    />

    <input
      ref="imageInputRef"
      type="file"
      accept="image/*"
      class="visually-hidden"
      @change="insertImage(($event.target as HTMLInputElement).files?.[0] ?? null); ($event.target as HTMLInputElement).value = ''"
    >
  </div>
</template>
