<template>
  <div class="app-layout">
    <!-- COMPACT TOP BAR (Oculto na tela de login) -->
    <header v-if="route.name !== 'login' && state.currentUser" class="app-topbar">
      <!-- Upper minimal brand & profile row -->
      <div class="top-row">
        <div class="brand">
          <span class="brand-text">Barber<span class="gold-accent">Admin</span></span>
        </div>

        <div class="user-actions">
          <!-- AVATAR DO PROFISSIONAL LOGADO (EDITÁVEL PELO PRÓPRIO) -->
          <div class="user-avatar-wrapper" title="Alterar minha foto de perfil">
            <img :src="state.currentUser.avatar_url" :alt="state.currentUser.name" class="topbar-avatar" />
            <label class="avatar-upload-overlay" :class="{ 'is-uploading': uploadingAvatar }">
              <input 
                type="file" 
                accept="image/png,image/jpeg,image/webp" 
                @change="handleAvatarFileChange" 
                style="display:none;" 
                :disabled="uploadingAvatar"
              />
              <svg v-if="!uploadingAvatar" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" class="cam-icon">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
              <span v-else class="cam-spinner">⏳</span>
            </label>
          </div>

          <div class="profile-switch">
            <select 
              :value="state.currentUser.id" 
              @change="handleUserSwitch($event.target.value)" 
              class="profile-select"
            >
              <option v-for="u in state.users" :key="u.id" :value="u.id">
                {{ u.nickname || u.name }} ({{ u.role === 'MASTER' ? 'Master' : 'Barbeiro' }})
              </option>
            </select>
          </div>

          <button class="logout-btn" @click="handleLogout" title="Sair da conta">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="logout-icon">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            <span class="logout-label">Sair</span>
          </button>
        </div>
      </div>

      <!-- ICON-ONLY SLEEK FIXED TOP TABS (100% WIDTH, NO SCROLLING REQUIRED) -->
      <nav class="icon-nav-bar">
        <router-link to="/" class="nav-btn" exact-active-class="active" title="Rendimento Diário">
          <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="20" x2="12" y2="10"/>
            <line x1="18" y1="20" x2="18" y2="4"/>
            <line x1="6" y1="20" x2="6" y2="16"/>
          </svg>
          <span class="nav-caption">Rendimento</span>
        </router-link>

        <router-link to="/appointments" class="nav-btn" active-class="active" title="Agenda & Atendimentos">
          <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
            <line x1="16" y1="2" x2="16" y2="6"/>
            <line x1="8" y1="2" x2="8" y2="6"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
          <span class="nav-caption">Agenda</span>
        </router-link>

        <router-link to="/clients" class="nav-btn" active-class="active" title="Clientes">
          <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <span class="nav-caption">Clientes</span>
        </router-link>

        <router-link to="/services" class="nav-btn" active-class="active" title="Serviços & Preços">
          <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="6" cy="6" r="3"/>
            <circle cx="6" cy="18" r="3"/>
            <line x1="20" y1="4" x2="8.12" y2="15.88"/>
            <line x1="14.47" y1="14.48" x2="20" y2="20"/>
            <line x1="8.12" y1="8.12" x2="12" y2="12"/>
          </svg>
          <span class="nav-caption">Serviços</span>
        </router-link>

        <router-link v-if="isMaster" to="/team" class="nav-btn" active-class="active" title="Equipe">
          <svg class="nav-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2l3 7h7l-5.5 4.5 2 7.5-6.5-5-6.5 5 2-7.5-5.5-4.5h7z"/>
          </svg>
          <span class="nav-caption">Equipe</span>
        </router-link>
      </nav>
    </header>

    <!-- CONTENT -->
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { state, isMaster, switchUser, logout, uploadBarberAvatar } from './store/state.js';

const route = useRoute();
const router = useRouter();
const uploadingAvatar = ref(false);

function handleUserSwitch(userId) {
  switchUser(userId);
}

function handleLogout() {
  logout();
  router.push({ name: 'login' });
}

async function handleAvatarFileChange(e) {
  const file = e.target.files?.[0];
  if (!file) return;

  // Validação de tamanho no cliente (máx 5MB)
  if (file.size > 5 * 1024 * 1024) {
    alert('A imagem deve ter no máximo 5MB.');
    e.target.value = '';
    return;
  }

  try {
    uploadingAvatar.value = true;
    await uploadBarberAvatar(state.currentUser.id, file);
  } catch (err) {
    alert('Falha ao enviar foto: ' + (err.message || 'Erro desconhecido'));
  } finally {
    uploadingAvatar.value = false;
    e.target.value = '';
  }
}
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: var(--bg-app);
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

/* User Avatar Wrapper with Camera Overlay */
.user-avatar-wrapper {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  border: 1.5px solid var(--accent-gold);
  background: #1e293b;
  flex-shrink: 0;
  cursor: pointer;
}

.topbar-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  cursor: pointer;
}

.user-avatar-wrapper:hover .avatar-upload-overlay,
.avatar-upload-overlay.is-uploading {
  opacity: 1;
}

.cam-icon {
  width: 15px;
  height: 15px;
  color: #fbbf24;
}

.cam-spinner {
  font-size: 0.75rem;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.25);
  color: #fca5a5;
  padding: 0.35rem 0.65rem;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.4);
  color: #ffffff;
}

.logout-icon {
  width: 14px;
  height: 14px;
}

.app-topbar {
  position: sticky;
  top: 0;
  z-index: 300;
  background: #0b0e14;
  border-bottom: 1px solid var(--border-subtle);
}

.top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}

.brand-text {
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.gold-accent {
  color: var(--accent-gold);
}

.profile-select {
  background: #121722;
  border: 1px solid var(--border-subtle);
  color: #fbbf24;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-xs);
  outline: none;
  cursor: pointer;
}

.profile-select option {
  background: #121722;
  color: #f1f5f9;
}

/* FIXED GRID NAVIGATION: FITS ALL SCREENS WITHOUT SCROLLING */
.icon-nav-bar {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
  padding: 0 0.25rem;
}

.nav-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
  padding: 0.5rem 0.25rem;
  color: var(--text-tertiary);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: all 0.15s ease;
}

.nav-btn:hover {
  color: var(--text-secondary);
}

.nav-btn.active {
  color: var(--accent-gold);
  border-bottom-color: var(--accent-gold);
}

.nav-svg {
  width: 18px;
  height: 18px;
  stroke-width: 2.2;
}

.nav-caption {
  font-size: 0.65rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.app-main {
  flex: 1;
  padding: 1rem 0.85rem;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}

@media (max-width: 480px) {
  .top-row {
    padding: 0.4rem 0.75rem;
  }
  .nav-btn {
    padding: 0.45rem 0.15rem;
  }
  .nav-svg {
    width: 17px;
    height: 17px;
  }
  .nav-caption {
    font-size: 0.62rem;
  }
}
</style>
