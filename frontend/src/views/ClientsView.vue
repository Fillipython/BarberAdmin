<template>
  <div class="clients-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Clientes</h1>
        <p class="page-subtitle">Carteira de clientes e histórico de visitas.</p>
      </div>

      <button class="btn btn-primary" @click="showModal = true">
        + Novo Cliente
      </button>
    </div>

    <!-- Search & Counter -->
    <div class="clean-panel header-bar">
      <div class="search-input-wrap">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="Buscar por nome ou WhatsApp..." 
          class="form-input" 
        />
      </div>

      <div class="counter-text">
        <span>Total:</span>
        <strong>{{ state.clients.length }}</strong>
      </div>
    </div>

    <!-- Clients Grid -->
    <div class="clients-grid">
      <div 
        v-for="client in filteredClients" 
        :key="client.id" 
        class="clean-panel client-card"
      >
        <div class="client-card-top">
          <div class="avatar-box">
            {{ getInitials(client.name) }}
          </div>
          <div class="client-meta">
            <h3 class="client-name">{{ client.name }}</h3>
            <span class="client-phone">{{ client.phone }}</span>
            <span v-if="client.email" class="client-email">{{ client.email }}</span>
          </div>
        </div>

        <div v-if="client.notes" class="client-notes">
          <span class="notes-label">Preferência:</span>
          <p class="notes-text">"{{ client.notes }}"</p>
        </div>

        <div class="client-stats">
          <div class="stat-mini">
            <span class="stat-mini-label">Visitas</span>
            <span class="stat-mini-val">{{ client.total_visits }}</span>
          </div>
          <div class="stat-mini">
            <span class="stat-mini-label">Total Gasto</span>
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
          <h2 class="modal-title">Novo Cliente</h2>
          <button class="btn-icon btn-secondary" @click="showModal = false">✕</button>
        </div>

        <form @submit.prevent="handleSaveClient" class="modal-body">
          <div class="form-group">
            <label class="form-label">Nome Completo *</label>
            <input v-model="form.name" required placeholder="Ex: Roberto Silva" class="form-input" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">WhatsApp *</label>
              <input v-model="form.phone" required placeholder="Ex: 11988887777" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Email</label>
              <input v-model="form.email" type="email" placeholder="cliente@email.com" class="form-input" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Observações / Preferências</label>
            <textarea 
              v-model="form.notes" 
              rows="2" 
              placeholder="Ex: Não passar navalha no pescoço..." 
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
  gap: 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 600;
}

.page-subtitle {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.header-bar {
  padding: 0.75rem 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.search-input-wrap {
  flex: 1;
  max-width: 380px;
}

.counter-text {
  font-size: 0.8rem;
  color: var(--text-secondary);
  display: flex;
  gap: 0.35rem;
}

.clients-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.75rem;
}

.client-card {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.client-card-top {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.avatar-box {
  width: 36px;
  height: 36px;
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xs);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  color: #fbbf24;
  flex-shrink: 0;
}

.client-meta {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-size: 0.95rem;
  font-weight: 600;
}

.client-phone, .client-email {
  font-size: 0.725rem;
  color: var(--text-secondary);
}

.client-notes {
  background: #090c13;
  padding: 0.5rem 0.65rem;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-subtle);
}

.notes-label {
  font-size: 0.65rem;
  text-transform: uppercase;
  color: var(--text-tertiary);
  font-weight: 600;
  display: block;
}

.notes-text {
  font-size: 0.75rem;
  color: var(--text-secondary);
  font-style: italic;
}

.client-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #090c13;
  border: 1px solid var(--border-subtle);
  padding: 0.5rem;
  border-radius: var(--radius-xs);
  text-align: center;
}

.stat-mini-label {
  font-size: 0.65rem;
  text-transform: uppercase;
  color: var(--text-tertiary);
  display: block;
}

.stat-mini-val {
  font-size: 0.95rem;
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

.modal-header {
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1rem;
  font-weight: 600;
}

.modal-body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-subtle);
}
</style>
