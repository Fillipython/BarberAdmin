import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '../views/DashboardView.vue';
import AppointmentsView from '../views/AppointmentsView.vue';
import ClientsView from '../views/ClientsView.vue';
import ServicesView from '../views/ServicesView.vue';
import TeamView from '../views/TeamView.vue';
import LoginView from '../views/LoginView.vue';
import { isUserAuthenticated } from '../store/state.js';

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/', name: 'dashboard', component: DashboardView },
  { path: '/appointments', name: 'appointments', component: AppointmentsView },
  { path: '/clients', name: 'clients', component: ClientsView },
  { path: '/services', name: 'services', component: ServicesView },
  { path: '/team', name: 'team', component: TeamView },
  { path: '/:pathMatch(.*)*', redirect: '/' }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
});

// Trava de navegação de segurança: exige login
router.beforeEach((to, from, next) => {
  const logged = isUserAuthenticated();

  if (!to.meta.public && !logged) {
    next({ name: 'login' });
  } else if (to.name === 'login' && logged) {
    next({ name: 'dashboard' });
  } else {
    next();
  }
});

export default router;
