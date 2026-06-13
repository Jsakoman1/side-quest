import {ref} from "vue"
import type {AppUserRole} from "../shared/sidequestDomain.ts"

export interface SessionUser {
  id: number
  email: string
  username: string
  role: AppUserRole
  token: string | null
}

const loadUser = () => {
  const storedUser = localStorage.getItem("user")
  if (!storedUser) {
    return null
  }

  try {
    return JSON.parse(storedUser) as SessionUser
  } catch {
    return null
  }
}

const loadToken = () => {
  return localStorage.getItem("token")
}

export const currentUser = ref<SessionUser | null>(loadUser())
export const token = ref<string | null>(loadToken())

export const saveSession = (user: SessionUser) => {
  currentUser.value = user
  token.value = user.token
  localStorage.setItem("user", JSON.stringify(user))

  if (user.token) {
    localStorage.setItem("token", user.token)
    return
  }

  localStorage.removeItem("token")
}

export const clearSession = () => {
  currentUser.value = null
  token.value = null
  localStorage.removeItem("user")
  localStorage.removeItem("token")
}
