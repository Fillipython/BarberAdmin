<template>
  <div class="team-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Equipe de Barbeiros</h1>
        <p class="page-subtitle">Profissionais cadastrados e percentuais de comissão.</p>
      </div>

      <button v-if="isMaster" class="btn btn-primary" @click="showModal = true">
        + Adicionar Barbeiro
      </button>
    </div>

    <!-- Restrict Warning -->
    <div v-if="!isMaster" class="clean-panel access-denied-panel">
      <h3>Acesso Restrito ao Barbeiro Master</h3>
      <p>Apenas o administrador master pode cadastrar, alterar comissões ou remover membros da barbearia.</p>
    </div>

    <!-- Barbers List -->
    <div v-else class="team-grid">
      <div 
        v-for="barber in state.users" 
        :key="barber.id" 
        class="clean-panel barber-card"
        :class="{ 'master-card': barber.role === 'MASTER' }"
      >
        <div class="barber-head">
          <div class="barber-info">
            <h3 class="barber-name">{{ barber.name }}</h3>
            <span class="barber-nick">{{ barber.nickname }}</span>
          </div>
          <span class="badge" :class="barber.role === 'MASTER' ? 'badge-gold' : 'badge-blue'">
            {{ barber.role === 'MASTER' ? 'Master' : 'Staff' }}
          </span>
        </div>

        <div class="barber-details">
          <div class="detail-row">
            <span>WhatsApp:</span>
            <span class="detail-val">{{ barber.phone }}</span>
          </div>
          <div class="detail-row">
            <span>Email:</span>
            <span class="detail-val">{{ barber.email }}</span>
          </div>
          <div class="detail-row">
            <span>Comissão:</span>
            <span class="detail-val gold-text bold">{{ barber.commission_percentage }}%</span>
          </div>
        </div>

        <div v-if="isMaster && barber.role !== 'MASTER'" class="card-footer">
          <button class="btn btn-danger btn-sm full-width" @click="handleRemoveBarber(barber)">
            Remover Barbeiro
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL: ADICIONAR BARBEIRO -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">Novo Barbeiro</h2>
          <button class="btn-icon btn-secondary" @click="showModal = false">✕</button>
        </div>

        <form @submit.prevent="handleSaveBarber" class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Nome Completo *</label>
              <input v-model="form.name" required placeholder="Ex: Roberto Alves" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Apelido *</label>
              <input v-model="form.nickname" required placeholder="Ex: Beto Navalha" class="form-input" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">WhatsApp *</label>
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

.access-denied-panel {
  padding: 1.5rem;
  border-left: 3px solid var(--accent-rose);
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.75rem;
}

.barber-card {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.master-card {
  border-left: 2px solid var(--accent-gold);
}

.barber-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.barber-name {
  font-size: 1rem;
  font-weight: 600;
}

.barber-nick {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.barber-details {
  background: #090c13;
  border: 1px solid var(--border-subtle);
  padding: 0.5rem 0.65rem;
  border-radius: var(--radius-xs);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.785rem;
  color: var(--text-secondary);
}

.detail-val {
  color: var(--text-primary);
}

.bold {
  font-weight: 600;
}

.gold-text {
  color: #fbbf24;
}

.card-footer {
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
