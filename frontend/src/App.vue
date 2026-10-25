<template>
  <div class="app-layout">
    <!-- TOP HEADER -->
    <header class="app-header">
      <div class="header-left">
        <div class="brand">
          <span class="brand-icon">💈</span>
          <div class="brand-text">
            <span class="brand-title">Barber<span class="gold-text">Admin</span></span>
            <span class="brand-tag">Gestão Profissional</span>
          </div>
        </div>
      </div>

      <!-- Quick Role Switcher for instant testing -->
      <div class="header-right">
        <div class="role-switcher-card">
          <span class="switch-label">Simular Usuário:</span>
          <select 
            :value="state.currentUser.id" 
            @change="handleUserSwitch($event.target.value)" 
            class="user-select"
          >
            <option v-for="u in state.users" :key="u.id" :value="u.id">
              {{ u.role === 'MASTER' ? '👑' : '✂️' }} {{ u.name }} ({{ u.role }})
            </option>
          </select>
        </div>

        <div class="user-pill">
          <img :src="state.currentUser.avatar_url" class="pill-avatar" />
          <div class="pill-info">
            <span class="pill-name">{{ state.currentUser.nickname || state.currentUser.name }}</span>
            <span class="pill-role" :class="isMaster ? 'gold-role' : 'blue-role'">
              {{ isMaster ? 'Barbeiro Master' : 'Barbeiro' }}
            </span>
          </div>
        </div>
      </div>
    </header>

    <!-- MAIN BODY: SIDEBAR + CONTENT -->
    <div class="app-body">
      <!-- DESKTOP SIDEBAR -->
      <aside class="desktop-sidebar">
        <nav class="nav-menu">
          <router-link to="/" class="nav-item" exact-active-class="active">
            <span class="nav-icon">📊</span>
            <span class="nav-text">Rendimento Diário</span>
          </router-link>

          <router-link to="/appointments" class="nav-item" active-class="active">
            <span class="nav-icon">📅</span>
            <span class="nav-text">Agendamentos</span>
          </router-link>

          <router-link to="/clients" class="nav-item" active-class="active">
            <span class="nav-icon">👥</span>
            <span class="nav-text">Clientes</span>
          </router-link>

          <router-link to="/services" class="nav-item" active-class="active">
            <span class="nav-icon">✂️</span>
            <span class="nav-text">Serviços & Preços</span>
          </router-link>

          <!-- Master Only Navigation Item -->
          <router-link v-if="isMaster" to="/team" class="nav-item" active-class="active">
            <span class="nav-icon">👑</span>
            <span class="nav-text">Equipe & Barbeiros</span>
          </router-link>
        </nav>

        <div class="sidebar-footer">
          <div class="app-status-box">
            <span class="pulse-dot"></span>
            <span>Pronto para App Externo (PWA / Mobile)</span>
          </div>
        </div>
      </aside>

      <!-- MAIN ROUTE VIEW -->
      <main class="main-content">
        <router-view />
      </main>
    </div>

    <!-- MOBILE NATIVE-STYLE BOTTOM BAR -->
    <nav class="mobile-bottom-bar">
      <router-link to="/" class="mobile-nav-item" exact-active-class="active">
        <span class="mobile-nav-icon">📊</span>
        <span class="mobile-nav-label">Rendimento</span>
      </router-link>

      <router-link to="/appointments" class="mobile-nav-item" active-class="active">
        <span class="mobile-nav-icon">📅</span>
        <span class="mobile-nav-label">Agenda</span>
      </router-link>

      <router-link to="/clients" class="mobile-nav-item" active-class="active">
        <span class="mobile-nav-icon">👥</span>
        <span class="mobile-nav-label">Clientes</span>
      </router-link>

      <router-link to="/services" class="mobile-nav-item" active-class="active">
        <span class="mobile-nav-icon">✂️</span>
        <span class="mobile-nav-label">Serviços</span>
      </router-link>

      <router-link v-if="isMaster" to="/team" class="mobile-nav-item" active-class="active">
        <span class="mobile-nav-icon">👑</span>
        <span class="mobile-nav-label">Equipe</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { state, isMaster, switchUser } from './store/state.js';

function handleUserSwitch(userId) {
  switchUser(userId);
}
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* Header */
.app-header {
  height: 68px;
  background: rgba(11, 15, 25, 0.95);
  border-bottom: 1px solid var(--border-color);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.brand-icon {
  font-size: 1.75rem;
}

.brand-title {
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.gold-text {
  color: #fbbf24;
}

.brand-tag {
  font-size: 0.65rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-dim);
  display: block;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.role-switcher-card {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--bg-card);
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.switch-label {
  font-size: 0.75rem;
  color: var(--text-dim);
  font-weight: 600;
}

.user-select {
  background: transparent;
  border: none;
  color: #fbbf24;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  outline: none;
}

.user-select option {
  background: var(--bg-card);
  color: var(--text-main);
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding-left: 0.75rem;
  border-left: 1px solid var(--border-color);
}

.pill-avatar {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 2px solid var(--gold-500);
}

.pill-info {
  display: flex;
  flex-direction: column;
}

.pill-name {
  font-size: 0.85rem;
  font-weight: 700;
  line-height: 1.2;
}

.pill-role {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
}

.gold-role { color: #fbbf24; }
.blue-role { color: #60a5fa; }

/* Body Layout */
.app-body {
  display: flex;
  flex: 1;
}

/* Desktop Sidebar */
.desktop-sidebar {
  width: 250px;
  background: var(--bg-dark);
  border-right: 1px solid var(--border-color);
  padding: 1.5rem 1rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  color: var(--text-muted);
  text-decoration: none;
  font-weight: 600;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-main);
}

.nav-item.active {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(217, 119, 6, 0.25));
  border: 1px solid rgba(245, 158, 11, 0.35);
  color: #fbbf24;
}

.nav-icon {
  font-size: 1.2rem;
}

.sidebar-footer {
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}

.app-status-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.725rem;
  color: var(--text-dim);
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--emerald-500);
  box-shadow: 0 0 8px var(--emerald-500);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 0.4; }
  50% { opacity: 1; }
  100% { opacity: 0.4; }
}

/* Main Content */
.main-content {
  flex: 1;
  padding: 1.75rem 2rem;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* Mobile Bottom Bar (Hidden on Desktop) */
.mobile-bottom-bar {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 64px;
  background: rgba(11, 15, 25, 0.96);
  border-top: 1px solid var(--border-color);
  backdrop-filter: blur(12px);
  z-index: 100;
  padding-bottom: var(--safe-bottom);
  justify-content: space-around;
  align-items: center;
}

.mobile-nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
  color: var(--text-dim);
  text-decoration: none;
  flex: 1;
  padding: 0.35rem 0;
  transition: all 0.2s ease;
}

.mobile-nav-item.active {
  color: #fbbf24;
}

.mobile-nav-icon {
  font-size: 1.25rem;
}

.mobile-nav-label {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
}

/* Mobile Responsive */
@media (max-width: 860px) {
  .desktop-sidebar {
    display: none;
  }

  .mobile-bottom-bar {
    display: flex;
  }

  .main-content {
    padding: 1.25rem 1rem 80px 1rem;
  }

  .role-switcher-card .switch-label {
    display: none;
  }

  .pill-info {
    display: none;
  }
}
</style>
