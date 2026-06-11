import {ref} from "vue";

export interface AuthUser {
    id: number
    email: string
    username: string
    role: string
    token: string | null
}

const storedUser = localStorage.getItem("user")
const storedToken = localStorage.getItem("token")

export const currentUser = ref<AuthUser | null>(storedUser ? JSON.parse(storedUser) : null)
export const token = ref<string | null>(storedToken)

export const loginUser = (user: AuthUser) => {
    currentUser.value = user
    token.value = user.token
    localStorage.setItem("user", JSON.stringify(user))
    if (user.token) {
        localStorage.setItem("token", user.token)
    }
}

export const logoutUser = () => {
    currentUser.value = null
    token.value = null
    localStorage.removeItem("user")
    localStorage.removeItem("token")
}

export const authHeader = () => {
    if (!token.value) {
        return {}
    } else {
        return {
            Authorization: `Bearer ${token.value}`
        }
    }
}

export const isLoggedIn = () => {
    return token.value !== null
}

export const isAdmin = () => {
    return currentUser.value?.role === "ADMIN"
}
