<template>
  <div class="team-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Equipe de Barbeiros</h1>
        <p class="page-subtitle">Gestão de profissionais, regras de comissão e controle de acesso.</p>
      </div>

      <button v-if="isMaster" class="btn btn-primary" @click="showModal = true">
        <span>💈</span> Adicionar Barbeiro
      </button>
    </div>

    <!-- Permission Warning if not master -->
    <div v-if="!isMaster" class="glass-card access-denied-card">
      <span class="warning-icon">🔒</span>
      <div>
        <h3>Acesso Restrito ao Barbeiro Master</h3>
        <p>Apenas o administrador master pode cadastrar, alterar comissões ou remover membros da barbearia.</p>
      </div>
    </div>

    <!-- Barbers List (Visible to Master) -->
    <div v-else class="team-grid">
      <div 
        v-for="barber in state.users" 
        :key="barber.id" 
        class="glass-card barber-card glass-card-hover"
        :class="{ 'master-card': barber.role === 'MASTER' }"
      >
        <div class="card-role-badge">
          <span class="badge" :class="barber.role === 'MASTER' ? 'badge-gold' : 'badge-blue'">
            {{ barber.role === 'MASTER' ? '👑 Barbeiro Master' : '✂️ Barbeiro Equipe' }}
          </span>
        </div>

        <div class="barber-profile">
          <img :src="barber.avatar_url" class="barber-avatar" />
          <h3 class="barber-name">{{ barber.name }}</h3>
          <span class="barber-nick">{{ barber.nickname }}</span>
        </div>

        <div class="barber-info-list">
          <div class="info-item">
            <span class="info-label">WhatsApp:</span>
            <span class="info-val">{{ barber.phone }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Email:</span>
            <span class="info-val">{{ barber.email }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Comissão Atual:</span>
            <span class="info-val gold-text bold">{{ barber.commission_percentage }}%</span>
          </div>
        </div>

        <div v-if="barber.bio" class="barber-bio">
          <p>{{ barber.bio }}</p>
        </div>

        <!-- Master Actions -->
        <div v-if="isMaster && barber.role !== 'MASTER'" class="card-footer">
          <button class="btn btn-danger btn-sm full-width" @click="handleRemoveBarber(barber)">
            🗑️ Remover da Barbearia
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL: ADICIONAR BARBEIRO -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">Novo Membro da Equipe</h2>
          <button class="btn-icon btn-secondary" @click="showModal = false">✕</button>
        </div>

        <form @submit.prevent="handleSaveBarber" class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Nome Completo *</label>
              <input v-model="form.name" required placeholder="Ex: Roberto Alves" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Apelido Profissional *</label>
              <input v-model="form.nickname" required placeholder="Ex: Beto Navalha" class="form-input" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">WhatsApp / Telefone *</label>
              <input v-model="form.phone" required placeholder="Ex: 11977772222" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Comissão (% sobre o corte) *</label>
              <input 
                type="number" 
                v-model.number="form.commission_percentage" 
                min="0" 
                max="100" 
                required 
                class="form-input" 
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Email de Acesso (Login) *</label>
            <input v-model="form.email" type="email" required placeholder="barbeiro@barbearia.com" class="form-input" />
          </div>

          <div class="form-group">
            <label class="form-label">Senha Inicial *</label>
            <input v-model="form.password" type="password" required placeholder="Senha de acesso..." class="form-input" />
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="showModal = false">Cancelar</button>
            <button type="submit" class="btn btn-primary">Adicionar Barbeiro</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { state, isMaster, addBarber, removeBarber } from '../store/state.js';

const showModal = ref(false);

const form = ref({
  name: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  commission_percentage: 50
});

function handleSaveBarber() {
  addBarber({
    name: form.value.name,
    nickname: form.value.nickname,
    phone: form.value.phone,
    email: form.value.email,
    commission_percentage: form.value.commission_percentage
  });

  form.value = {
    name: '',
    nickname: '',
    phone: '',
    email: '',
    password: '',
    commission_percentage: 50
  };
  showModal.value = false;
}

function handleRemoveBarber(barber) {
  if (confirm(`Tem certeza que deseja remover ${barber.name} (${barber.nickname}) da barbearia?`)) {
    removeBarber(barber.id);
  }
}
</script>

<style scoped>
.team-page {
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

.access-denied-card {
  padding: 2rem;
  display: flex;
  align-items: center;
  gap: 1.25rem;
  border-left: 4px solid var(--rose-500);
}

.warning-icon {
  font-size: 2rem;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.barber-card {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  position: relative;
}

.master-card {
  border-color: rgba(245, 158, 11, 0.4);
}

.card-role-badge {
  display: flex;
  justify-content: flex-end;
}

.barber-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.barber-avatar {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 3px solid var(--gold-500);
  margin-bottom: 0.5rem;
}

.barber-name {
  font-size: 1.2rem;
  color: var(--text-main);
}

.barber-nick {
  font-size: 0.85rem;
  color: #fbbf24;
}

.barber-info-list {
  background: var(--bg-dark);
  padding: 0.85rem;
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.825rem;
}

.info-label {
  color: var(--text-dim);
}

.info-val {
  color: var(--text-muted);
}

.bold {
  font-weight: 700;
}

.gold-text {
  color: #fbbf24;
}

.barber-bio {
  font-size: 0.8rem;
  color: var(--text-dim);
  font-style: italic;
  text-align: center;
}

.card-footer {
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
