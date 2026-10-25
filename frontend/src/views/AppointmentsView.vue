<template>
  <div class="appointments-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Agendamentos</h1>
        <p class="page-subtitle">
          {{ isMaster 
              ? 'Agenda geral de todos os profissionais da barbearia.' 
              : 'Seus atendimentos agendados.' }}
        </p>
      </div>

      <button class="btn btn-primary" @click="openNewModal">
        + Novo Agendamento
      </button>
    </div>

    <!-- Filters Bar -->
    <div class="clean-panel filters-bar">
      <!-- Master: Barber Filter -->
      <div v-if="isMaster" class="filter-group">
        <label class="filter-label">Barbeiro:</label>
        <select v-model="selectedBarberFilter" class="form-select">
          <option value="ALL">Todos os Barbeiros</option>
          <option v-for="b in state.users" :key="b.id" :value="b.id">
            {{ b.name }} ({{ b.nickname }})
          </option>
        </select>
      </div>

      <!-- Date Filter -->
      <div class="filter-group">
        <label class="filter-label">Data:</label>
        <input type="date" v-model="filterDate" class="form-input" />
      </div>

      <!-- Status Filter -->
      <div class="filter-group">
        <label class="filter-label">Status:</label>
        <select v-model="filterStatus" class="form-select">
          <option value="ALL">Todos os Status</option>
          <option value="CONFIRMED">Agendados</option>
          <option value="COMPLETED">Concluídos</option>
          <option value="CANCELLED">Cancelados</option>
        </select>
      </div>
    </div>

    <!-- Appointments List -->
    <div class="appointments-container">
      <div v-if="filteredAppointments.length === 0" class="clean-panel empty-state">
        <p class="empty-text">Nenhum agendamento encontrado para esta data.</p>
        <button class="btn btn-secondary btn-sm" @click="openNewModal">Criar Agendamento</button>
      </div>

      <div v-else class="appointments-grid">
        <div 
          v-for="app in filteredAppointments" 
          :key="app.id" 
          class="clean-panel appointment-card"
          :class="{ 'card-completed': app.status === 'COMPLETED' }"
        >
          <div class="app-card-header">
            <div class="time-block">
              <span class="time-text">{{ app.scheduled_time || '09:00' }}</span>
              <span class="date-text">{{ app.scheduled_date }}</span>
            </div>

            <span class="badge" :class="getStatusBadgeClass(app.status)">
              {{ formatStatus(app.status) }}
            </span>
          </div>

          <div class="app-body">
            <div class="client-row">
              <span class="client-name">{{ app.client_name }}</span>
              <span class="client-phone">{{ app.client_phone }}</span>
            </div>

            <div class="service-strip">
              <div class="service-left">
                <span class="service-name">{{ app.service_name }}</span>
                <span class="assigned-barber">{{ app.barber_name }}</span>
              </div>
              <span class="service-val gold-text">{{ formatCurrency(app.price_cents) }}</span>
            </div>

            <div v-if="app.payment_method" class="payment-note">
              Pago via {{ app.payment_method }}
            </div>
          </div>

          <div class="app-card-actions">
            <button 
              v-if="app.status === 'CONFIRMED'" 
              class="btn btn-primary btn-sm full-width"
              @click="openCompleteModal(app)"
            >
              Concluir & Receber
            </button>
            <button 
              v-if="app.status === 'CONFIRMED'" 
              class="btn btn-secondary btn-sm"
              @click="cancelAppointment(app.id)"
            >
              Cancelar
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL: NOVO AGENDAMENTO -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">Novo Agendamento</h2>
          <button class="btn-icon btn-secondary" @click="showModal = false">✕</button>
        </div>

        <form @submit.prevent="handleSaveAppointment" class="modal-body">
          <div class="form-group">
            <label class="form-label">Cliente Cadastrado</label>
            <select v-model="selectedClientId" @change="onClientSelect" class="form-select">
              <option value="">-- Selecione ou preencha abaixo --</option>
              <option v-for="c in state.clients" :key="c.id" :value="c.id">
                {{ c.name }} ({{ c.phone }})
              </option>
            </select>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Nome do Cliente *</label>
              <input v-model="form.client_name" required placeholder="Ex: Felipe Silva" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Telefone / WhatsApp *</label>
              <input v-model="form.client_phone" required placeholder="Ex: 11999998888" class="form-input" />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Barbeiro Designado *</label>
            <select v-model="form.barber_id" required class="form-select" :disabled="!isMaster">
              <option v-for="b in state.users" :key="b.id" :value="b.id">
                {{ b.name }} ({{ b.nickname }})
              </option>
            </select>
          </div>

          <!-- Service Selection with Dynamic Price Auto-fill and Override -->
          <div class="form-group highlight-box">
            <label class="form-label">Serviço Selecionado *</label>
            <select v-model="form.service_id" @change="onServiceSelect" required class="form-select">
              <option value="">Selecione o serviço...</option>
              <option v-for="s in state.services.filter(s => s.is_active)" :key="s.id" :value="s.id">
                {{ s.name }} — {{ formatCurrency(s.price_cents) }} ({{ s.duration_minutes }} min)
              </option>
            </select>

            <div class="price-override-wrap">
              <label class="form-label">Valor Cobrado (R$) *</label>
              <input 
                type="number" 
                step="0.50" 
                v-model.number="formPriceDisplay" 
                required 
                class="form-input price-input" 
              />
              <span class="hint-text">Valor ajustável livremente para este atendimento.</span>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Data *</label>
              <input type="date" v-model="form.scheduled_date" required class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Horário *</label>
              <input type="time" v-model="form.scheduled_time" required class="form-input" />
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="showModal = false">Cancelar</button>
            <button type="submit" class="btn btn-primary">Confirmar Agendamento</button>
          </div>
        </form>
      </div>
    </div>

    <!-- MODAL: CONCLUIR ATENDIMENTO & PAGAMENTO -->
    <div v-if="completingApp" class="modal-backdrop" @click.self="completingApp = null">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">Concluir Atendimento</h2>
          <button class="btn-icon btn-secondary" @click="completingApp = null">✕</button>
        </div>

        <div class="modal-body">
          <div class="finish-summary">
            <div class="summary-line">
              <span>Cliente:</span>
              <strong>{{ completingApp.client_name }}</strong>
            </div>
            <div class="summary-line">
              <span>Serviço:</span>
              <strong>{{ completingApp.service_name }}</strong>
            </div>
            <div class="summary-line total-line">
              <span>Valor Total:</span>
              <span class="gold-text bold">{{ formatCurrency(completingApp.price_cents) }}</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Forma de Pagamento</label>
            <div class="payment-options">
              <label class="payment-option" :class="{ selected: completePaymentMethod === 'PIX' }">
                <input type="radio" v-model="completePaymentMethod" value="PIX" />
                <span>PIX</span>
              </label>
              <label class="payment-option" :class="{ selected: completePaymentMethod === 'DINHEIRO' }">
                <input type="radio" v-model="completePaymentMethod" value="DINHEIRO" />
                <span>Dinheiro</span>
              </label>
              <label class="payment-option" :class="{ selected: completePaymentMethod === 'CARTAO_CREDITO' }">
                <input type="radio" v-model="completePaymentMethod" value="CARTAO_CREDITO" />
                <span>Crédito</span>
              </label>
              <label class="payment-option" :class="{ selected: completePaymentMethod === 'CARTAO_DEBITO' }">
                <input type="radio" v-model="completePaymentMethod" value="CARTAO_DEBITO" />
                <span>Débito</span>
              </label>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="completingApp = null">Voltar</button>
            <button class="btn btn-primary" @click="confirmComplete">Receber & Finalizar</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { 
  state, 
  isMaster, 
  formatCurrency, 
  addAppointment, 
  updateAppointmentStatus 
} from '../store/state.js';

const showModal = ref(false);
const completingApp = ref(null);
const completePaymentMethod = ref('PIX');

const selectedBarberFilter = ref('ALL');
const filterDate = ref(new Date().toISOString().split('T')[0]);
const filterStatus = ref('ALL');

const selectedClientId = ref('');
const formPriceDisplay = ref(35.00);

const form = ref({
  client_name: '',
  client_phone: '',
  barber_id: state.currentUser.id,
  service_id: '',
  scheduled_date: new Date().toISOString().split('T')[0],
  scheduled_time: '14:00'
});

const filteredAppointments = computed(() => {
  return state.appointments.filter(app => {
    if (!isMaster.value && app.barber_id !== state.currentUser.id) return false;
    if (isMaster.value && selectedBarberFilter.value !== 'ALL' && app.barber_id !== selectedBarberFilter.value) return false;
    if (filterDate.value && app.scheduled_date !== filterDate.value) return false;
    if (filterStatus.value !== 'ALL' && app.status !== filterStatus.value) return false;
    return true;
  });
});

function openNewModal() {
  form.value = {
    client_name: '',
    client_phone: '',
    barber_id: state.currentUser.id,
    service_id: state.services[0]?.id || '',
    scheduled_date: filterDate.value || new Date().toISOString().split('T')[0],
    scheduled_time: '14:00'
  };
  onServiceSelect();
  showModal.value = true;
}

function onClientSelect() {
  const client = state.clients.find(c => c.id === selectedClientId.value);
  if (client) {
    form.value.client_name = client.name;
    form.value.client_phone = client.phone;
  }
}

function onServiceSelect() {
  const service = state.services.find(s => s.id === form.value.service_id);
  if (service) {
    formPriceDisplay.value = service.price_cents / 100;
  }
}

function handleSaveAppointment() {
  const barber = state.users.find(u => u.id === form.value.barber_id);
  const service = state.services.find(s => s.id === form.value.service_id);

  addAppointment({
    client_name: form.value.client_name,
    client_phone: form.value.client_phone,
    barber_id: form.value.barber_id,
    barber_name: barber ? barber.name : 'Barbeiro',
    service_id: form.value.service_id,
    service_name: service ? service.name : 'Serviço',
    price_cents: Math.round(formPriceDisplay.value * 100),
    scheduled_date: form.value.scheduled_date,
    scheduled_time: form.value.scheduled_time
  });

  showModal.value = false;
}

function openCompleteModal(app) {
  completingApp.value = app;
  completePaymentMethod.value = 'PIX';
}

function confirmComplete() {
  if (completingApp.value) {
    updateAppointmentStatus(completingApp.value.id, 'COMPLETED', completePaymentMethod.value);
    completingApp.value = null;
  }
}

function cancelAppointment(id) {
  if (confirm('Deseja realmente cancelar este agendamento?')) {
    updateAppointmentStatus(id, 'CANCELLED');
  }
}

function formatStatus(status) {
  const map = {
    CONFIRMED: 'Agendado',
    COMPLETED: 'Concluído',
    CANCELLED: 'Cancelado'
  };
  return map[status] || status;
}

function getStatusBadgeClass(status) {
  const map = {
    CONFIRMED: 'badge-blue',
    COMPLETED: 'badge-emerald',
    CANCELLED: 'badge-rose'
  };
  return map[status] || 'badge-muted';
}
</script>

<style scoped>
.appointments-page {
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

.filters-bar {
  padding: 0.75rem 1rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 160px;
  flex: 1;
}

.filter-label {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.appointments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.75rem;
}

.appointment-card {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border-left: 2px solid var(--border-subtle);
}

.card-completed {
  border-left-color: var(--accent-emerald);
}

.app-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-block {
  display: flex;
  align-items: baseline;
  gap: 0.4rem;
}

.time-text {
  font-size: 1rem;
  font-weight: 700;
  color: #fbbf24;
  font-family: var(--font-mono);
}

.date-text {
  font-size: 0.725rem;
  color: var(--text-tertiary);
}

.app-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.client-row {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
}

.client-phone {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.service-strip {
  background: #090c13;
  border: 1px solid var(--border-subtle);
  padding: 0.5rem 0.65rem;
  border-radius: var(--radius-xs);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.service-left {
  display: flex;
  flex-direction: column;
}

.service-name {
  font-size: 0.8rem;
  font-weight: 500;
}

.assigned-barber {
  font-size: 0.7rem;
  color: var(--text-tertiary);
}

.service-val {
  font-size: 0.95rem;
  font-weight: 700;
}

.gold-text {
  color: #fbbf24;
}

.payment-note {
  font-size: 0.7rem;
  color: var(--accent-emerald);
  font-weight: 500;
}

.app-card-actions {
  display: flex;
  gap: 0.4rem;
  margin-top: auto;
}

.full-width {
  flex: 1;
}

.empty-state {
  padding: 2.5rem 1rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.empty-text {
  color: var(--text-tertiary);
  font-size: 0.85rem;
}

/* Modals */
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

.highlight-box {
  background: rgba(217, 151, 38, 0.04);
  border: 1px solid rgba(217, 151, 38, 0.2);
  padding: 0.75rem;
  border-radius: var(--radius-xs);
}

.price-override-wrap {
  margin-top: 0.5rem;
}

.price-input {
  font-size: 1.15rem;
  font-weight: 700;
  color: #fbbf24;
}

.hint-text {
  font-size: 0.7rem;
  color: var(--text-tertiary);
  display: block;
  margin-top: 0.25rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-subtle);
}

.finish-summary {
  background: #090c13;
  border: 1px solid var(--border-subtle);
  padding: 0.75rem;
  border-radius: var(--radius-xs);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.summary-line {
  display: flex;
  justify-content: space-between;
  font-size: 0.825rem;
}

.total-line {
  font-size: 1rem;
  border-top: 1px solid var(--border-subtle);
  padding-top: 0.4rem;
  margin-top: 0.25rem;
}

.bold {
  font-weight: 700;
}

.payment-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  margin-top: 0.35rem;
}

.payment-option {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.55rem 0.65rem;
  background: #090c13;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xs);
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 500;
}

.payment-option.selected {
  border-color: var(--accent-gold);
  background: var(--accent-gold-dim);
}
</style>
