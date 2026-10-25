<template>
  <div class="clients-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Clientes</h1>
        <p class="page-subtitle">Carteira de clientes cadastrados, histórico de frequência e preferências.</p>
      </div>

      <button class="btn btn-primary" @click="showModal = true">
        <span>👤</span> Novo Cliente
      </button>
    </div>

    <!-- Search & Stats Bar -->
    <div class="glass-card header-bar">
      <div class="search-input-wrap">
        <span class="search-icon">🔍</span>
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="Buscar por nome ou WhatsApp..." 
          class="form-input search-input" 
        />
      </div>

      <div class="total-clients-counter">
        <span class="counter-label">Total Cadastrado:</span>
        <span class="counter-value">{{ state.clients.length }} clientes</span>
      </div>
    </div>

    <!-- Clients Grid -->
    <div class="clients-grid">
      <div 
        v-for="client in filteredClients" 
        :key="client.id" 
        class="glass-card client-card glass-card-hover"
      >
        <div class="client-card-top">
          <div class="avatar-initials">
            {{ getInitials(client.name) }}
          </div>
          <div class="client-meta">
            <h3 class="client-name">{{ client.name }}</h3>
            <span class="client-phone">📞 {{ client.phone }}</span>
            <span v-if="client.email" class="client-email">✉️ {{ client.email }}</span>
          </div>
        </div>

        <div v-if="client.notes" class="client-notes">
          <span class="notes-label">Preferência / Obs:</span>
          <p class="notes-text">"{{ client.notes }}"</p>
        </div>

        <div class="client-stats">
          <div class="stat-mini">
            <span class="stat-mini-label">Visitas</span>
            <span class="stat-mini-val">{{ client.total_visits }}</span>
          </div>
          <div class="stat-mini">
            <span class="stat-mini-label">Total Consumido</span>
            <span class="stat-mini-val gold-text">{{ formatCurrency(client.total_spent_cents) }}</span>
          </div>
        </div>

        <div class="client-actions">
          <router-link to="/appointments" class="btn btn-secondary btn-sm full-width">
            Agendar Atendimento
          </router-link>
        </div>
      </div>
    </div>

    <!-- MODAL: NOVO CLIENTE -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">Cadastrar Novo Cliente</h2>
          <button class="btn-icon btn-secondary" @click="showModal = false">✕</button>
        </div>

        <form @submit.prevent="handleSaveClient" class="modal-body">
          <div class="form-group">
            <label class="form-label">Nome Completo *</label>
            <input v-model="form.name" required placeholder="Ex: Lucas Gabriel" class="form-input" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Telefone / WhatsApp *</label>
              <input v-model="form.phone" required placeholder="Ex: 11988887777" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Email (Opcional)</label>
              <input v-model="form.email" type="email" placeholder="cliente@email.com" class="form-input" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Preferências / Observações do Corte</label>
            <textarea 
              v-model="form.notes" 
              rows="3" 
              placeholder="Ex: Não gosta de navalha na nuca, usa barba desenhada..." 
              class="form-textarea"
            ></textarea>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="showModal = false">Cancelar</button>
            <button type="submit" class="btn btn-primary">Salvar Cliente</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { state, formatCurrency, addClient } from '../store/state.js';

const showModal = ref(false);
const searchQuery = ref('');

const form = ref({
  name: '',
  phone: '',
  email: '',
  notes: ''
});

const filteredClients = computed(() => {
  if (!searchQuery.value) return state.clients;
  const q = searchQuery.value.toLowerCase();
  return state.clients.filter(c => 
    c.name.toLowerCase().includes(q) || 
    c.phone.includes(q)
  );
});

function getInitials(name) {
  if (!name) return 'CL';
  const parts = name.trim().split(' ');
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function handleSaveClient() {
  addClient({
    name: form.value.name,
    phone: form.value.phone,
    email: form.value.email,
    notes: form.value.notes
  });

  form.value = { name: '', phone: '', email: '', notes: '' };
  showModal.value = false;
}
</script>

<style scoped>
.clients-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-title {
  font-size: 1.75rem;
}

.page-subtitle {
  color: var(--text-muted);
  font-size: 0.95rem;
}

.header-bar {
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.search-input-wrap {
  position: relative;
  flex: 1;
  min-width: 260px;
}

.search-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-dim);
}

.search-input {
  padding-left: 2.75rem;
}

.total-clients-counter {
  font-size: 0.875rem;
  color: var(--text-muted);
}

.counter-value {
  color: #fbbf24;
  font-weight: 700;
  margin-left: 0.35rem;
}

/* Clients Grid */
.clients-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(310px, 1fr));
  gap: 1.25rem;
}

.client-card {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.client-card-top {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.avatar-initials {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2), rgba(217, 119, 6, 0.4));
  border: 1px solid rgba(245, 158, 11, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1rem;
  color: #fbbf24;
  flex-shrink: 0;
}

.client-meta {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-size: 1.1rem;
  color: var(--text-main);
}

.client-phone, .client-email {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.client-notes {
  background: var(--bg-dark);
  padding: 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
}

.notes-label {
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  font-size: 0.7rem;
  display: block;
  margin-bottom: 0.2rem;
}

.notes-text {
  color: var(--text-muted);
  font-style: italic;
}

.client-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color);
  padding: 0.75rem;
  border-radius: var(--radius-sm);
  text-align: center;
}

.stat-mini-label {
  font-size: 0.725rem;
  text-transform: uppercase;
  color: var(--text-dim);
  display: block;
}

.stat-mini-val {
  font-size: 1.1rem;
  font-weight: 700;
}

.gold-text {
  color: #fbbf24;
}

.client-actions {
  margin-top: auto;
}

.full-width {
  width: 100%;
}

/* Modal */
.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1.25rem;
}

.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}
</style>
