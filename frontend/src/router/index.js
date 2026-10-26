import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '../views/DashboardView.vue';
import AppointmentsView from '../views/AppointmentsView.vue';
import ClientsView from '../views/ClientsView.vue';
import ServicesView from '../views/ServicesView.vue';
import TeamView from '../views/TeamView.vue';

const routes = [
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

export default router;
