<template>
  <div class="appointments-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1 class="page-title">Agendamentos & Atendimentos</h1>
        <p class="page-subtitle">
          {{ isMaster 
              ? 'Visualize a agenda de todos os profissionais e gerencie os atendimentos.' 
              : 'Seus atendimentos agendados e histórico de cortes.' }}
        </p>
      </div>

      <button class="btn btn-primary" @click="openNewModal">
        <span>✂️</span> Novo Atendimento
      </button>
    </div>

    <!-- Filters Bar -->
    <div class="glass-card filters-bar">
      <!-- Master: Barber Filter -->
      <div v-if="isMaster" class="filter-group">
        <label class="filter-label">Filtrar por Barbeiro:</label>
        <select v-model="selectedBarberFilter" class="form-select filter-select">
          <option value="ALL">👥 Todos os Barbeiros</option>
          <option v-for="b in state.users" :key="b.id" :value="b.id">
            {{ b.name }} ({{ b.nickname }})
          </option>
        </select>
      </div>

      <!-- Date Filter -->
      <div class="filter-group">
        <label class="filter-label">Data:</label>
        <input type="date" v-model="filterDate" class="form-input filter-date" />
      </div>

      <!-- Status Filter -->
      <div class="filter-group">
        <label class="filter-label">Status:</label>
        <select v-model="filterStatus" class="form-select filter-select">
          <option value="ALL">Todos os Status</option>
          <option value="CONFIRMED">Agendados</option>
          <option value="COMPLETED">Concluídos</option>
          <option value="CANCELLED">Cancelados</option>
        </select>
      </div>
    </div>

    <!-- Appointments List -->
    <div class="appointments-container">
      <div v-if="filteredAppointments.length === 0" class="glass-card empty-state">
        <span class="empty-icon">📅</span>
        <h3>Nenhum agendamento encontrado</h3>
        <p>Não há atendimentos para os filtros selecionados nesta data.</p>
        <button class="btn btn-primary btn-sm" @click="openNewModal">Criar Primeiro Agendamento</button>
      </div>

      <div v-else class="appointments-grid">
        <div 
          v-for="app in filteredAppointments" 
          :key="app.id" 
          class="glass-card appointment-card glass-card-hover"
          :class="{ 'card-completed': app.status === 'COMPLETED' }"
        >
          <div class="app-card-header">
            <div class="app-time-badge">
              <span class="time-text">{{ app.scheduled_time || '09:00' }}</span>
              <span class="date-text">{{ app.scheduled_date }}</span>
            </div>

            <span class="badge" :class="getStatusBadgeClass(app.status)">
              {{ formatStatus(app.status) }}
            </span>
          </div>

          <div class="app-client-info">
            <h3 class="client-name">{{ app.client_name }}</h3>
            <span class="client-phone">📞 {{ app.client_phone }}</span>
          </div>

          <div class="app-service-info">
            <div class="service-row">
              <span class="service-name">✂️ {{ app.service_name }}</span>
              <span class="service-price gold-text">{{ formatCurrency(app.price_cents) }}</span>
            </div>
            <div class="barber-assigned">
              <span class="assigned-label">Atendido por:</span>
              <span class="assigned-name">{{ app.barber_name }}</span>
            </div>
            <div v-if="app.payment_method" class="payment-badge-wrap">
              <span class="badge badge-emerald">Pago via {{ app.payment_method }}</span>
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
          <!-- Client Selection or typing -->
          <div class="form-group">
            <label class="form-label">Cliente Cadastrado</label>
            <select v-model="selectedClientId" @change="onClientSelect" class="form-select">
              <option value="">-- Selecione ou digite abaixo --</option>
              <option v-for="c in state.clients" :key="c.id" :value="c.id">
                {{ c.name }} ({{ c.phone }})
              </option>
            </select>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Nome do Cliente *</label>
              <input v-model="form.client_name" required placeholder="Ex: Lucas Silva" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">WhatsApp / Telefone *</label>
              <input v-model="form.client_phone" required placeholder="Ex: 11999998888" class="form-input" />
            </div>
          </div>

          <!-- Barber Selection -->
          <div class="form-group">
            <label class="form-label">Barbeiro Designado *</label>
            <select v-model="form.barber_id" required class="form-select" :disabled="!isMaster">
              <option v-for="b in state.users" :key="b.id" :value="b.id">
                {{ b.name }} ({{ b.nickname }})
              </option>
            </select>
            <small v-if="!isMaster" class="hint-text">Barbeiros comuns agendam em seu próprio nome.</small>
          </div>

          <!-- Service Selection with Dynamic Price Auto-fill and Override -->
          <div class="form-group highlight-box">
            <label class="form-label">Tipo de Serviço *</label>
            <select v-model="form.service_id" @change="onServiceSelect" required class="form-select">
              <option value="">Selecione o serviço...</option>
              <option v-for="s in state.services.filter(s => s.is_active)" :key="s.id" :value="s.id">
                {{ s.name }} — {{ formatCurrency(s.price_cents) }} ({{ s.duration_minutes }} min)
              </option>
            </select>

            <div class="price-override-wrap">
              <label class="form-label">Valor do Atendimento (R$) *</label>
              <p class="hint-text">Preenchido automaticamente pelo serviço, mas você pode mudar o valor livremente para este atendimento:</p>
              <input 
                type="number" 
                step="0.50" 
                v-model.number="formPriceDisplay" 
                required 
                class="form-input price-input" 
              />
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
            <p><strong>Cliente:</strong> {{ completingApp.client_name }}</p>
            <p><strong>Serviço:</strong> {{ completingApp.service_name }}</p>
            <p><strong>Barbeiro:</strong> {{ completingApp.barber_name }}</p>
            <div class="total-highlight">
              <span>Valor a Receber:</span>
              <span class="gold-text bold-text">{{ formatCurrency(completingApp.price_cents) }}</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Forma de Pagamento</label>
            <div class="payment-options">
              <label class="payment-option" :class="{ selected: completePaymentMethod === 'PIX' }">
                <input type="radio" v-model="completePaymentMethod" value="PIX" />
                <span>⚡ PIX</span>
              </label>
              <label class="payment-option" :class="{ selected: completePaymentMethod === 'DINHEIRO' }">
                <input type="radio" v-model="completePaymentMethod" value="DINHEIRO" />
                <span>💵 Dinheiro</span>
              </label>
              <label class="payment-option" :class="{ selected: completePaymentMethod === 'CARTAO_CREDITO' }">
                <input type="radio" v-model="completePaymentMethod" value="CARTAO_CREDITO" />
                <span>💳 Crédito</span>
              </label>
              <label class="payment-option" :class="{ selected: completePaymentMethod === 'CARTAO_DEBITO' }">
                <input type="radio" v-model="completePaymentMethod" value="CARTAO_DEBITO" />
                <span>💳 Débito</span>
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

// Filter appointments based on user role + active filters
const filteredAppointments = computed(() => {
  return state.appointments.filter(app => {
    // 1. Role Check
    if (!isMaster.value && app.barber_id !== state.currentUser.id) {
      return false;
    }

    // 2. Master Barber Filter
    if (isMaster.value && selectedBarberFilter.value !== 'ALL' && app.barber_id !== selectedBarberFilter.value) {
      return false;
    }

    // 3. Date Filter
    if (filterDate.value && app.scheduled_date !== filterDate.value) {
      return false;
    }

    // 4. Status Filter
    if (filterStatus.value !== 'ALL' && app.status !== filterStatus.value) {
      return false;
    }

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

// When service is picked, automatically set price to default service price, while allowing edits
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

.filters-bar {
  padding: 1.25rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1.25rem;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 200px;
  flex: 1;
}

.filter-label {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-dim);
}

.filter-select, .filter-date {
  padding: 0.55rem 0.85rem;
  font-size: 0.875rem;
}

/* Appointments Grid */
.appointments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.25rem;
}

.appointment-card {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card-completed {
  border-left: 4px solid var(--emerald-500);
}

.app-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.app-time-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.time-text {
  font-size: 1.15rem;
  font-weight: 800;
  color: #fbbf24;
  font-family: var(--font-display);
}

.date-text {
  font-size: 0.8rem;
  color: var(--text-dim);
}

.app-client-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.client-name {
  font-size: 1.1rem;
  color: var(--text-main);
}

.client-phone {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.app-service-info {
  background: var(--bg-dark);
  padding: 0.85rem;
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.service-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.gold-text {
  color: #fbbf24;
}

.barber-assigned {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: var(--text-dim);
}

.app-card-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: auto;
}

.full-width {
  flex: 1;
}

.empty-state {
  padding: 3rem 1.5rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.empty-icon {
  font-size: 3rem;
}

/* Modals */
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

.highlight-box {
  background: rgba(245, 158, 11, 0.05);
  border: 1px dashed rgba(245, 158, 11, 0.3);
  padding: 1rem;
  border-radius: var(--radius-md);
}

.price-override-wrap {
  margin-top: 0.75rem;
}

.price-input {
  font-size: 1.25rem;
  font-weight: 700;
  color: #fbbf24;
}

.hint-text {
  font-size: 0.75rem;
  color: var(--text-dim);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}

/* Finish modal */
.finish-summary {
  background: var(--bg-dark);
  padding: 1rem;
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.total-highlight {
  display: flex;
  justify-content: space-between;
  font-size: 1.2rem;
  border-top: 1px solid var(--border-color);
  padding-top: 0.5rem;
  margin-top: 0.5rem;
}

.bold-text {
  font-weight: 800;
}

.payment-options {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.payment-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: var(--bg-dark);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 600;
}

.payment-option.selected {
  border-color: var(--gold-500);
  background: var(--gold-glow);
}
</style>
