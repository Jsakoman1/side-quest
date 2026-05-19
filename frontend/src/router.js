import {createRouter, createWebHistory} from "vue-router";
import AppUsersPage from "./pages/AppUsersPage.vue";
import QuestsPage from "./pages/QuestsPage.vue";

const routes = [
    {
        path: '/',
        component: AppUsersPage
    },
    {
        path: '/quests',
        component: QuestsPage
    }
]

export const router = createRouter({
    history: createWebHistory(),
    routes
})