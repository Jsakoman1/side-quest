import {ref} from "vue"
import axios from "axios"
import {authHeader, isAdmin} from "../auth.ts"
import {buildRequestDebugInfo, formatDebugInfo} from "../httpDebug.ts"

interface AppUser {
  id: number
  email: string
  username: string
  role: string
}

export const useAppUsersPage = () => {
  const appUsers = ref<AppUser[]>([])
  const isLoadingUsers = ref(false)
  const pageError = ref("")
  const pageErrorDetails = ref<string[]>([])
  const copiedDebug = ref(false)
  const feedback = ref("")
  const feedbackType = ref<"error" | "success">("success")
  const isCreateUserDialogOpen = ref(false)

  const email = ref("")
  const username = ref("")

  const editingAppUserId = ref<number | null>(null)
  const editAppUserEmail = ref("")
  const editAppUserUsername = ref("")

  const showFeedback = (message: string, type: "error" | "success") => {
    feedback.value = message
    feedbackType.value = type
  }

  const openCreateUserDialog = () => {
    isCreateUserDialogOpen.value = true
  }

  const closeCreateUserDialog = () => {
    isCreateUserDialogOpen.value = false
  }

  const copyDebugInfo = async () => {
    if (!pageErrorDetails.value.length) {
      return
    }

    await navigator.clipboard.writeText(formatDebugInfo(pageErrorDetails.value))
    copiedDebug.value = true
    window.setTimeout(() => {
      copiedDebug.value = false
    }, 1500)
  }

  const fetchAppUsers = async () => {
    isLoadingUsers.value = true
    pageError.value = ""
    pageErrorDetails.value = []

    try {
      const response = await axios.get<AppUser[]>("http://localhost:8080/app_users", {
        headers: authHeader()
      })
      appUsers.value = response.data
    } catch (error) {
      pageError.value = "Could not load users."
      pageErrorDetails.value = buildRequestDebugInfo("http://localhost:8080/app_users", "GET", error)
    } finally {
      isLoadingUsers.value = false
    }
  }

  const createAppUser = async () => {
    try {
      await axios.post("http://localhost:8080/app_users", {
        email: email.value.trim(),
        username: username.value.trim()
      }, {
        headers: authHeader()
      })

      email.value = ""
      username.value = ""
      showFeedback("User created.", "success")
      closeCreateUserDialog()
      await fetchAppUsers()
    } catch (error) {
      showFeedback("Could not create user.", "error")
    }
  }

  const deleteAppUser = async (id: number) => {
    try {
      await axios.delete(`http://localhost:8080/app_users/${id}`, {
        headers: authHeader()
      })
      showFeedback("User deleted.", "success")
      await fetchAppUsers()
    } catch (error) {
      showFeedback("Could not delete user.", "error")
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this user?")) {
      return
    }

    await deleteAppUser(id)
  }

  const startEdit = (appUser: AppUser) => {
    editingAppUserId.value = appUser.id
    editAppUserEmail.value = appUser.email
    editAppUserUsername.value = appUser.username
  }

  const updateAppUser = async () => {
    if (editingAppUserId.value === null) {
      return
    }

    try {
      await axios.put(`http://localhost:8080/app_users/${editingAppUserId.value}`, {
        email: editAppUserEmail.value.trim(),
        username: editAppUserUsername.value.trim()
      }, {
        headers: authHeader()
      })

      editingAppUserId.value = null
      editAppUserEmail.value = ""
      editAppUserUsername.value = ""
      showFeedback("User updated.", "success")
      await fetchAppUsers()
    } catch (error) {
      showFeedback("Could not update user.", "error")
    }
  }

  const cancelEdit = () => {
    editingAppUserId.value = null
  }

  const init = async () => {
    if (isAdmin()) {
      await fetchAppUsers()
    }
  }

  return {
    appUsers,
    isLoadingUsers,
    pageError,
    pageErrorDetails,
    copiedDebug,
    feedback,
    feedbackType,
    isCreateUserDialogOpen,
    email,
    username,
    editingAppUserId,
    editAppUserEmail,
    editAppUserUsername,
    showFeedback,
    openCreateUserDialog,
    closeCreateUserDialog,
    copyDebugInfo,
    fetchAppUsers,
    createAppUser,
    deleteAppUser,
    handleDelete,
    startEdit,
    updateAppUser,
    cancelEdit,
    init,
    isAdmin
  }
}
