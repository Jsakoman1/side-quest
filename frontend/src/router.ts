import {createRouter, createWebHistory} from "vue-router";
import AdminPage from "./pages/AdminPage.vue";
import QuestsPage from "./pages/QuestsPage.vue";
import QuestDetailView from "./views/QuestDetailView.vue";
import LoginView from "./views/LoginView.vue";
import RegisterView from "./views/RegisterView.vue";
import {isAdmin, isLoggedIn} from "./auth.ts";


const routes = [
    {
        path: '/',
        redirect: '/quests'
    },
    {
        path: '/login',
        component: LoginView
    },
    {
        path: '/register',
        component: RegisterView
    },
    {
        path: '/quests',
        component: QuestsPage,
        meta: {requiresAuth: true}
    },
    {
        path: '/quests/:id',
        component: QuestDetailView,
        meta: {requiresAuth: true}
    },
    {
        path: '/admin',
        component: AdminPage,
        meta: {requiresAuth: true, requiresAdmin: true}
    },
    {
        path: '/app-users',
        redirect: '/admin'
    }
];

export const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to) => {
    if (to.meta.requiresAuth && !isLoggedIn()) {
        return '/login';
    }

    if (to.meta.requiresAdmin && !isAdmin()) {
        return '/quests';
    }

    if (isLoggedIn() && (to.path === '/login' || to.path === '/register')) {
        return '/quests';
    }
})
