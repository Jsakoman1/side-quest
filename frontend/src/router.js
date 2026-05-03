import {createRouter, createWebHistory} from "vue-router";
import AppUsersPage from "./pages/AppUsersPage.vue";

const routes = [
    {
        path: '/',
        component: AppUsersPage
    }
]

export const router = createRouter({
    history: createWebHistory(),
    routes
})