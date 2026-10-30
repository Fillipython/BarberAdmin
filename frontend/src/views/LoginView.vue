<template>
  <div class="login-page">
    <div class="login-card">
      <!-- Brand Header -->
      <div class="brand-header">
        <div class="logo-badge">
          <svg class="logo-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="6" cy="6" r="3"/>
            <circle cx="6" cy="18" r="3"/>
            <line x1="20" y1="4" x2="8.12" y2="15.88"/>
            <line x1="14.47" y1="14.48" x2="20" y2="20"/>
          </svg>
        </div>
        <h1 class="brand-title">Barber<span class="gold-accent">Admin</span></h1>
        <p class="brand-subtitle">Acesso Exclusivo para Profissionais</p>
      </div>

      <!-- Error Alert -->
      <div v-if="errorMessage" class="error-banner">
        <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- Login Form (Sem valores pré-preenchidos na tela inicial) -->
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label class="input-label" for="email">E-mail do Profissional</label>
          <div class="input-wrapper">
            <svg class="field-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
              <polyline points="22,6 12,13 2,6"/>
            </svg>
            <input 
              id="email"
              type="email" 
              v-model="email" 
              placeholder="Digite seu e-mail..."
              required 
              autocomplete="username"
              class="text-input"
            />
          </div>
        </div>

        <div class="form-group">
          <label class="input-label" for="password">Senha</label>
          <div class="input-wrapper">
            <svg class="field-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <input 
              id="password"
              :type="showPassword ? 'text' : 'password'" 
              v-model="password" 
              placeholder="Digite sua senha..."
              required 
              autocomplete="current-password"
              class="text-input"
            />
            <button 
              type="button" 
              class="eye-btn" 
              @click="showPassword = !showPassword"
              title="Mostrar/ocultar senha"
            >
              <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="eye-icon">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="eye-icon">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <span v-else>Entrar no Sistema</span>
        </button>
      </form>

      <!-- Security footer -->
      <div class="login-footer">
        <span>🔒 Conexão Segura SSL • Integrado ao BarberNotify</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { login } from '../store/state.js';

const router = useRouter();

const email = ref('');
const password = ref('');
const showPassword = ref(false);
const loading = ref(false);
const errorMessage = ref('');

async function handleLogin() {
  loading.value = true;
  errorMessage.value = '';
  try {
    await login(email.value, password.value);
    router.push({ name: 'dashboard' });
  } catch (err) {
    errorMessage.value = err.message || 'E-mail ou senha incorretos.';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: radial-gradient(circle at 50% 10%, rgba(212, 175, 55, 0.08) 0%, #090a0f 65%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: rgba(18, 20, 29, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 36px 32px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7), 0 0 30px rgba(212, 175, 55, 0.05);
  backdrop-filter: blur(16px);
}

.brand-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.2), rgba(212, 175, 55, 0.05));
  border: 1px solid rgba(212, 175, 55, 0.3);
  margin-bottom: 14px;
}

.logo-icon {
  width: 26px;
  height: 26px;
  color: #d4af37;
}

.brand-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #f8fafc;
  margin: 0 0 6px 0;
  letter-spacing: -0.02em;
}

.gold-accent {
  color: #d4af37;
}

.brand-subtitle {
  font-size: 0.85rem;
  color: #94a3b8;
  margin: 0;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #fca5a5;
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 0.85rem;
  margin-bottom: 20px;
}

.error-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: #ef4444;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #cbd5e1;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.field-icon {
  position: absolute;
  left: 14px;
  width: 18px;
  height: 18px;
  color: #64748b;
  pointer-events: none;
}

.text-input {
  width: 100%;
  background: rgba(10, 12, 18, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  padding: 12px 42px 12px 42px;
  color: #f8fafc;
  font-size: 0.95rem;
  outline: none;
  transition: all 0.2s ease;
}

.text-input:focus {
  border-color: #d4af37;
  box-shadow: 0 0 0 3px rgba(212, 175, 55, 0.15);
  background: rgba(10, 12, 18, 0.95);
}

.eye-btn {
  position: absolute;
  right: 12px;
  background: transparent;
  border: none;
  color: #64748b;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}

.eye-btn:hover {
  color: #cbd5e1;
}

.eye-icon {
  width: 18px;
  height: 18px;
}

.submit-btn {
  background: linear-gradient(135deg, #d4af37 0%, #b89728 100%);
  color: #090a0f;
  font-weight: 700;
  font-size: 0.95rem;
  padding: 14px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  margin-top: 6px;
  box-shadow: 0 4px 14px rgba(212, 175, 55, 0.25);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(212, 175, 55, 0.35);
  filter: brightness(1.05);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(9, 10, 15, 0.3);
  border-top-color: #090a0f;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 0.75rem;
  color: #475569;
}
</style>
