<template>
  <div class="appointments-page">
    <!-- Top Bar: Action & Status -->
    <div class="header-action-row">
      <h1 class="page-title">Agenda</h1>
      <button class="btn btn-primary btn-sm" @click="openNewModal">
        + Agendar
      </button>
    </div>

    <!-- MINIMALIST DATE NAVIGATOR (NO UGLY OS WHITE POPUP) -->
    <div class="clean-panel date-navigator">
      <button class="date-arrow-btn" @click="changeDay(-1)" title="Dia anterior">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </button>

      <div class="date-display">
        <span class="date-main">{{ formattedDateDisplay }}</span>
        <span v-if="isToday" class="today-tag">Hoje</span>
      </div>

      <button class="date-arrow-btn" @click="changeDay(1)" title="Próximo dia">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="9 18 15 12 9 6"></polyline>
        </svg>
      </button>

      <button v-if="!isToday" class="btn btn-secondary btn-xs go-today-btn" @click="goToToday">
        Ir para Hoje
      </button>
    </div>

    <!-- COMPACT FILTER STRIP -->
    <div class="clean-panel filters-strip">
      <div v-if="isMaster" class="mini-filter">
        <select v-model="selectedBarberFilter" class="strip-select">
          <option value="ALL">Todos os Barbeiros</option>
          <option v-for="b in state.users" :key="b.id" :value="b.id">
            {{ b.nickname || b.name }}
          </option>
        </select>
      </div>

      <div class="mini-filter">
        <select v-model="filterStatus" class="strip-select">
          <option value="ALL">Todos os Status</option>
          <option value="CONFIRMED">Agendados</option>
          <option value="COMPLETED">Concluídos</option>
          <option value="CANCELLED">Cancelados</option>
        </select>
      </div>

      <div class="counter-label">
        {{ filteredAppointments.length }} {{ filteredAppointments.length === 1 ? 'atendimento' : 'atendimentos' }}
      </div>
    </div>

    <!-- APPOINTMENTS LIST -->
    <div class="appointments-list">
      <div v-if="filteredAppointments.length === 0" class="clean-panel empty-block">
        <p class="empty-msg">Nenhum atendimento neste dia.</p>
        <button class="btn btn-secondary btn-sm" @click="openNewModal">Criar Agendamento</button>
      </div>

      <div v-else class="cards-stack">
        <div 
          v-for="app in filteredAppointments" 
          :key="app.id" 
          class="clean-panel app-row"
          :class="{ 'app-completed': app.status === 'COMPLETED' }"
        >
          <div class="time-col">
            <span class="app-time">{{ app.scheduled_time || '09:00' }}</span>
            <span class="status-indicator" :class="getStatusBadgeClass(app.status)">
              {{ formatStatus(app.status) }}
            </span>
          </div>

          <div class="info-col">
            <div class="client-title">{{ app.client_name }}</div>
            <div class="service-details">
              <span>{{ app.service_name }}</span>
              <span v-if="isMaster" class="barber-assigned">({{ app.barber_name }})</span>
            </div>
            <div v-if="app.payment_method" class="payment-method-tag">
              Pago via {{ app.payment_method }}
            </div>
          </div>

          <div class="action-col">
            <div class="price-tag">{{ formatCurrency(app.price_cents) }}</div>

            <div v-if="app.status === 'CONFIRMED'" class="buttons-pair">
              <button class="btn btn-primary btn-xs" @click="openCompleteModal(app)">
                Receber
              </button>
              <button class="btn btn-secondary btn-xs" @click="cancelAppointment(app.id)">
                ✕
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL NOVO AGENDAMENTO -->
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
              <option value="">-- Selecione ou digite o nome --</option>
              <option v-for="c in state.clients" :key="c.id" :value="c.id">
                {{ c.name }} ({{ c.phone }})
              </option>
            </select>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Nome do Cliente *</label>
              <input v-model="form.client_name" required placeholder="Nome..." class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">WhatsApp *</label>
              <input v-model="form.client_phone" required placeholder="DDD + número" class="form-input" />
            </div>
          </div>

          <div v-if="isMaster" class="form-group">
            <label class="form-label">Barbeiro Designado *</label>
            <select v-model="form.barber_id" required class="form-select">
              <option v-for="b in state.users" :key="b.id" :value="b.id">
                {{ b.name }} ({{ b.nickname }})
              </option>
            </select>
          </div>

          <!-- Dynamic Service & Editable Price -->
          <div class="form-group service-select-box">
            <label class="form-label">Serviço *</label>
            <select v-model="form.service_id" @change="onServiceSelect" required class="form-select">
              <option value="">Selecione o serviço...</option>
              <option v-for="s in state.services.filter(s => s.is_active)" :key="s.id" :value="s.id">
                {{ s.name }} ({{ formatCurrency(s.price_cents) }})
              </option>
            </select>

            <div class="price-input-group">
              <label class="form-label">Valor (R$) — ajustável:</label>
              <input 
                type="number" 
                step="0.50" 
                v-model.number="formPriceDisplay" 
                required 
                class="form-input price-bold-input" 
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Horário *</label>
              <input type="time" v-model="form.scheduled_time" required class="form-input" />
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="showModal = false">Cancelar</button>
            <button type="submit" class="btn btn-primary">Salvar Agendamento</button>
          </div>
        </form>
      </div>
    </div>

    <!-- MODAL RECEBER / FINALIZAR -->
    <div v-if="completingApp" class="modal-backdrop" @click.self="completingApp = null">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">Receber Atendimento</h2>
          <button class="btn-icon btn-secondary" @click="completingApp = null">✕</button>
        </div>

        <div class="modal-body">
          <div class="summary-receipt">
            <span>{{ completingApp.client_name }} — {{ completingApp.service_name }}</span>
            <div class="receipt-price">{{ formatCurrency(completingApp.price_cents) }}</div>
          </div>

          <div class="form-group">
            <label class="form-label">Forma de Pagamento</label>
            <div class="payment-grid">
              <label class="pay-chip" :class="{ active: completePaymentMethod === 'PIX' }">
                <input type="radio" v-model="completePaymentMethod" value="PIX" />
                <span>PIX</span>
              </label>
              <label class="pay-chip" :class="{ active: completePaymentMethod === 'DINHEIRO' }">
                <input type="radio" v-model="completePaymentMethod" value="DINHEIRO" />
                <span>Dinheiro</span>
              </label>
              <label class="pay-chip" :class="{ active: completePaymentMethod === 'CARTAO_CREDITO' }">
                <input type="radio" v-model="completePaymentMethod" value="CARTAO_CREDITO" />
                <span>Crédito</span>
              </label>
              <label class="pay-chip" :class="{ active: completePaymentMethod === 'CARTAO_DEBITO' }">
                <input type="radio" v-model="completePaymentMethod" value="CARTAO_DEBITO" />
                <span>Débito</span>
              </label>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn btn-secondary" @click="completingApp = null">Voltar</button>
            <button class="btn btn-primary" @click="confirmComplete">Confirmar Recebimento</button>
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
const filterStatus = ref('ALL');
const selectedClientId = ref('');
const formPriceDisplay = ref(35.00);

// Minimalist Date Logic without OS datepicker
const currentDateObj = ref(new Date());

const filterDate = computed(() => {
  return currentDateObj.value.toISOString().split('T')[0];
});

const isToday = computed(() => {
  const todayStr = new Date().toISOString().split('T')[0];
  return filterDate.value === todayStr;
});

const formattedDateDisplay = computed(() => {
  const options = { weekday: 'short', day: '2-digit', month: 'short' };
  const str = currentDateObj.value.toLocaleDateString('pt-BR', options);
  // capitalize first letter
  return str.charAt(0).toUpperCase() + str.slice(1);
});

function changeDay(delta) {
  const d = new Date(currentDateObj.value);
  d.setDate(d.getDate() + delta);
  currentDateObj.value = d;
}

function goToToday() {
  currentDateObj.value = new Date();
}

const form = ref({
  client_name: '',
  client_phone: '',
  barber_id: state.currentUser.id,
  service_id: '',
  scheduled_time: '14:00'
});

const filteredAppointments = computed(() => {
  return state.appointments.filter(app => {
    if (!isMaster.value && app.barber_id !== state.currentUser.id) return false;
    if (isMaster.value && selectedBarberFilter.value !== 'ALL' && app.barber_id !== selectedBarberFilter.value) return false;
    if (app.scheduled_date !== filterDate.value) return false;
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
    barber_name: barber ? (barber.nickname || barber.name) : 'Barbeiro',
    service_id: form.value.service_id,
    service_name: service ? service.name : 'Serviço',
    price_cents: Math.round(formPriceDisplay.value * 100),
    scheduled_date: filterDate.value,
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
  if (confirm('Cancelar este agendamento?')) {
    updateAppointmentStatus(id, 'CANCELLED');
  }
}

function formatStatus(status) {
  const map = {
    CONFIRMED: 'Agendado',
    COMPLETED: 'Feito',
    CANCELLED: 'Cancelado'
  };
  return map[status] || status;
}

function getStatusBadgeClass(status) {
  const map = {
    CONFIRMED: 'status-blue',
    COMPLETED: 'status-green',
    CANCELLED: 'status-red'
  };
  return map[status] || 'status-gray';
}
</script>

<style scoped>
.appointments-page {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.header-action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 1.15rem;
  font-weight: 700;
}

/* SLEEK NATIVE DATE CONTROLLER */
.date-navigator {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.45rem 0.75rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
}

.date-arrow-btn {
  background: #080a0f;
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  width: 32px;
  height: 32px;
  border-radius: var(--radius-xs);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.date-arrow-btn:hover {
  border-color: var(--accent-gold);
}

.date-arrow-btn svg {
  width: 14px;
  height: 14px;
}

.date-display {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.date-main {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text-primary);
}

.today-tag {
  background: var(--accent-gold-dim);
  color: #fbbf24;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.1rem 0.35rem;
  border-radius: var(--radius-xs);
  text-transform: uppercase;
}

.btn-xs {
  padding: 0.25rem 0.5rem;
  font-size: 0.7rem;
}

.go-today-btn {
  margin-left: 0.5rem;
}

/* FILTERS STRIP */
.filters-strip {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.75rem;
  flex-wrap: wrap;
}

.strip-select {
  background: #080a0f;
  border: 1px solid var(--border-subtle);
  color: var(--text-primary);
  font-size: 0.75rem;
  padding: 0.3rem 0.5rem;
  border-radius: var(--radius-xs);
  outline: none;
}

.counter-label {
  margin-left: auto;
  font-size: 0.725rem;
  color: var(--text-tertiary);
}

/* LIST OF APPOINTMENTS */
.cards-stack {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.app-row {
  display: flex;
  align-items: center;
  padding: 0.75rem 0.85rem;
  gap: 0.75rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-left: 3px solid #38bdf8;
}

.app-completed {
  border-left-color: #10b981;
}

.time-col {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 52px;
}

.app-time {
  font-size: 0.95rem;
  font-weight: 700;
  font-family: var(--font-mono);
  color: #fbbf24;
}

.status-indicator {
  font-size: 0.6rem;
  font-weight: 700;
  text-transform: uppercase;
}

.status-blue { color: #38bdf8; }
.status-green { color: #34d399; }
.status-red { color: #fb7185; }
.status-gray { color: var(--text-tertiary); }

.info-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.client-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.service-details {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.barber-assigned {
  color: var(--text-tertiary);
  margin-left: 0.25rem;
}

.payment-method-tag {
  font-size: 0.65rem;
  color: #34d399;
  font-weight: 500;
}

.action-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.35rem;
}

.price-tag {
  font-size: 0.9rem;
  font-weight: 700;
  color: #f1f5f9;
}

.buttons-pair {
  display: flex;
  gap: 0.35rem;
}

.empty-block {
  padding: 2rem 1rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.empty-msg {
  font-size: 0.8rem;
  color: var(--text-tertiary);
}

/* MODAL */
.modal-header {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 0.95rem;
  font-weight: 600;
}

.modal-body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.service-select-box {
  background: #090c13;
  border: 1px solid var(--border-subtle);
  padding: 0.65rem;
  border-radius: var(--radius-xs);
}

.price-input-group {
  margin-top: 0.5rem;
}

.price-bold-input {
  font-size: 1.1rem;
  font-weight: 700;
  color: #fbbf24;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.4rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--border-subtle);
}

.summary-receipt {
  background: #090c13;
  padding: 0.75rem;
  border-radius: var(--radius-xs);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  font-weight: 600;
}

.receipt-price {
  font-size: 1.1rem;
  color: #fbbf24;
}

.payment-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.4rem;
  margin-top: 0.3rem;
}

.pay-chip {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: #090c13;
  border: 1px solid var(--border-subtle);
  padding: 0.5rem 0.6rem;
  border-radius: var(--radius-xs);
  cursor: pointer;
  font-size: 0.785rem;
}

.pay-chip.active {
  border-color: var(--accent-gold);
  background: var(--accent-gold-dim);
}
</style>
