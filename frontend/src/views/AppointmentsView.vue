<template>
  <div class="agenda-view">
    <!-- Clean, Open Header with Date Navigation Inline -->
    <div class="agenda-header">
      <div class="header-left">
        <h1 class="page-title">Agenda</h1>
        <!-- Sleek Inline Date Stepper without boxes -->
        <div class="inline-date-nav">
          <button class="nav-arrow" @click="changeDay(-1)" title="Dia anterior">‹</button>
          <span class="nav-date-text">{{ formattedDateDisplay }}</span>
          <button class="nav-arrow" @click="changeDay(1)" title="Próximo dia">›</button>
          <button v-if="!isToday" class="today-link" @click="goToToday">Hoje</button>
        </div>
      </div>

      <div class="header-actions-row">
        <button
          v-if="isMaster"
          type="button"
          class="btn btn-sm"
          :class="isShopClosedOnDay ? 'btn-reopen-day' : 'btn-close-day'"
          :disabled="togglingBlock"
          @click="handleToggleShopDay"
        >
          {{ togglingBlock ? 'Processando...' : (isShopClosedOnDay ? 'Reabrir Loja no Dia' : 'Fechar Loja no Dia') }}
        </button>

        <button
          v-else
          type="button"
          class="btn btn-sm"
          :class="isBarberClosedOnDay(state.currentUser?.id) ? 'btn-reopen-day' : 'btn-close-day'"
          :disabled="togglingBlock || isShopClosedByMasterOnDay"
          @click="handleToggleBarberDay(state.currentUser?.id)"
        >
          {{
            isShopClosedByMasterOnDay
              ? 'Loja Fechada pelo Master'
              : (togglingBlock ? 'Processando...' : (isBarberClosedOnDay(state.currentUser?.id) ? 'Reabrir Meu Horário no Dia' : 'Fechar Meu Horário no Dia'))
          }}
        </button>

        <button class="btn btn-primary btn-sm" @click="openNewModal">
          + Agendar
        </button>
      </div>
    </div>

    <!-- Painel de Disponibilidade por Barbeiro (Master pode fechar/reabrir horário de qualquer barbeiro; Barbeiro vê seu status) -->
    <div class="day-availability-bar" :class="{ 'shop-closed-banner': isShopClosedOnDay || (!isMaster && isBarberClosedOnDay(state.currentUser?.id)) }">
      <div class="availability-info">
        <span class="availability-title">
          <template v-if="isShopClosedOnDay">
            Loja fechada nesta data — todos os horários estão bloqueados no BarberNotify.
          </template>
          <template v-else-if="!isMaster && isBarberClosedOnDay(state.currentUser?.id)">
            Seu horário completo está fechado para clientes nesta data no BarberNotify.
          </template>
          <template v-else>
            Disponibilidade no dia ({{ formattedDateDisplay }}):
          </template>
        </span>
      </div>

      <div v-if="isMaster" class="barber-availability-chips">
        <div
          v-for="b in state.users"
          :key="b.id"
          class="barber-chip-item"
          :class="{ 'chip-closed': isBarberClosedOnDay(b.id) }"
        >
          <span class="chip-name">{{ b.nickname || b.name }}</span>
          <span class="chip-status">{{ isBarberClosedOnDay(b.id) ? 'Fechado' : 'Aberto' }}</span>
          <button
            type="button"
            class="chip-toggle-btn"
            :disabled="togglingBlock"
            @click="handleToggleBarberDay(b.id)"
          >
            {{ isBarberClosedOnDay(b.id) ? 'Reabrir' : 'Fechar Horário' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Understated Text Tabs for Status & Barber Filter -->
    <div class="agenda-subbar">
      <div class="status-tabs">
        <button 
          v-for="st in statusOptions" 
          :key="st.value" 
          class="tab-link" 
          :class="{ active: filterStatus === st.value }"
          @click="filterStatus = st.value"
        >
          {{ st.label }}
        </button>
      </div>

      <!-- Barber Filter for Master -->
      <div v-if="isMaster" class="barber-filter-inline">
        <select v-model="selectedBarberFilter" class="minimal-filter-select">
          <option value="ALL">Todos os Barbeiros</option>
          <option v-for="b in state.users" :key="b.id" :value="b.id">
            {{ b.nickname || b.name }}
          </option>
        </select>
      </div>
    </div>

    <!-- Timeline List of Appointments (Content-first, Hairline dividers, Zero Chunky Boxes) -->
    <div class="timeline-container">
      <div v-if="filteredAppointments.length === 0" class="empty-timeline">
        <span>Nenhum atendimento agendado para este dia.</span>
      </div>

      <div v-else class="timeline-list">
        <div 
          v-for="app in filteredAppointments" 
          :key="app.id" 
          class="timeline-item"
          :class="{ 'item-done': app.status === 'COMPLETED' }"
        >
          <!-- Time Column -->
          <div class="time-block">
            <span class="time-digit">{{ app.scheduled_time || '09:00' }}</span>
            <span class="status-dot" :class="getStatusClass(app.status)"></span>
          </div>

          <!-- Main Info Column -->
          <div class="info-block">
            <div class="client-name">{{ app.client_name }}</div>
            <div class="service-meta">
              <span class="service-name">{{ app.service_name }}</span>
              <span v-if="isMaster" class="barber-name">· {{ app.barber_name }}</span>
              <span v-if="app.payment_method" class="payment-note">· {{ app.payment_method }}</span>
            </div>
          </div>

          <!-- Price & Actions Column (Master can edit price anytime) -->
          <div class="actions-block">
            <div class="price-wrapper">
              <span class="price-num">{{ formatCurrency(app.price_cents) }}</span>
              <!-- Master direct edit button (Service & Price) -->
              <button 
                v-if="isMaster" 
                class="edit-price-btn" 
                @click="openEditPriceModal(app)" 
                title="Barbeiro Master: Alterar Serviço ou Valor"
              >
                <svg class="pencil-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/>
                  <path d="m15 5 4 4"/>
                </svg>
              </button>
            </div>

            <div v-if="app.status === 'CONFIRMED'" class="item-actions">
              <button class="btn btn-primary btn-xs" @click="openCompleteModal(app)">
                Receber
              </button>
              <button class="cancel-link" @click="cancelAppointment(app.id)" title="Cancelar">
                ✕
              </button>
            </div>
            <span v-else class="done-tag">
              {{ app.status === 'COMPLETED' ? 'Concluído' : 'Cancelado' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL: EDITAR SERVIÇO E VALOR (BARBEIRO MASTER) -->
    <div v-if="editingPriceApp" class="modal-backdrop" @click.self="editingPriceApp = null">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">Editar Atendimento</h2>
          <button class="btn-icon btn-secondary" @click="editingPriceApp = null">✕</button>
        </div>

        <form @submit.prevent="savePriceEdit" class="modal-body">
          <p class="edit-summary">
            Cliente: <strong>{{ editingPriceApp.client_name }}</strong>
          </p>

          <div class="form-group">
            <label class="form-label">Tipo de Serviço</label>
            <select v-model="editSelectedServiceId" @change="onEditServiceChange" class="form-select">
              <option v-for="s in activeServices" :key="s.id" :value="s.id">
                {{ s.name }} — {{ formatCurrency(s.price_cents ?? s.priceCents) }}
              </option>
            </select>
          </div>

          <div class="form-group highlight-box">
            <label class="form-label">Valor Cobrado (R$) — Ajustável</label>
            <input 
              type="number" 
              step="0.50" 
              v-model.number="quickEditPriceDisplay" 
              required 
              class="form-input price-lg" 
            />
            <span class="hint-text">
              Ao selecionar outro serviço, o valor atualiza para o preço daquele serviço, mas você pode mudar para o valor que quiser.
            </span>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="editingPriceApp = null">Cancelar</button>
            <button type="submit" class="btn btn-primary">Salvar Alterações</button>
          </div>
        </form>
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
          <div class="form-group highlight-box">
            <label class="form-label">Serviço *</label>
            <select v-model="form.service_id" @change="onServiceSelect" required class="form-select">
              <option value="">Selecione o serviço...</option>
              <option v-for="s in activeServices" :key="s.id" :value="s.id">
                {{ s.name }} ({{ formatCurrency(s.price_cents ?? s.priceCents) }})
              </option>
            </select>

            <div class="price-input-group">
              <label class="form-label">Valor Cobrado (R$) — ajustável:</label>
              <input 
                type="number" 
                step="0.50" 
                v-model.number="formPriceDisplay" 
                required 
                class="form-input price-bold-input" 
              />
              <span class="hint-text">Valor ajustável livremente. Ao trocar o serviço, atualiza automaticamente.</span>
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
          <p class="edit-summary">
            {{ completingApp.client_name }} — {{ completingApp.service_name }}
          </p>

          <!-- Price can be modified before completing -->
          <div class="form-group">
            <label class="form-label">Valor Final (R$)</label>
            <input 
              type="number" 
              step="0.50" 
              v-model.number="completeFinalPriceDisplay" 
              required 
              class="form-input price-lg" 
            />
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
import { ref, computed, watch } from 'vue';
import { 
  state, 
  isMaster, 
  formatCurrency, 
  toLocalIsoDate,
  addAppointment, 
  updateAppointmentStatus,
  updateAppointmentPrice,
  updateAppointmentService,
  syncDateFromBackend,
  closeDaySchedule,
  reopenDaySchedule
} from '../store/state.js';

const showModal = ref(false);
const togglingBlock = ref(false);
const completingApp = ref(null);
const completePaymentMethod = ref('PIX');
const completeFinalPriceDisplay = ref(0);

const editingPriceApp = ref(null);
const editSelectedServiceId = ref('');
const quickEditPriceDisplay = ref(0);

const selectedBarberFilter = ref('ALL');
const filterStatus = ref('ALL');
const selectedClientId = ref('');
const formPriceDisplay = ref(35.00);

const statusOptions = [
  { label: 'Todos', value: 'ALL' },
  { label: 'Agendados', value: 'CONFIRMED' },
  { label: 'Concluídos', value: 'COMPLETED' },
  { label: 'Cancelados', value: 'CANCELLED' }
];

const currentDateObj = ref(new Date());

const filterDate = computed(() => {
  return toLocalIsoDate(currentDateObj.value);
});

watch(filterDate, (newDate) => {
  syncDateFromBackend(newDate);
}, { immediate: true });

const currentDayBlock = computed(() => {
  return state.dayBlocks?.[filterDate.value] || { shopClosed: false, shopClosedByMaster: false, closedBarberIds: [] };
});

const isShopClosedByMasterOnDay = computed(() => {
  return Boolean(currentDayBlock.value.shopClosedByMaster);
});

const isShopClosedOnDay = computed(() => {
  if (currentDayBlock.value.shopClosed) return true;
  const closedIds = currentDayBlock.value.closedBarberIds || [];
  const activeUsers = (state.users || []).filter(u => u.is_active !== false);
  return activeUsers.length > 0 && activeUsers.every(u => closedIds.includes(u.id));
});

function isBarberClosedOnDay(barberId) {
  if (!barberId) return false;
  if (isShopClosedOnDay.value) return true;
  const closedIds = currentDayBlock.value.closedBarberIds || [];
  if (closedIds.includes(barberId)) return true;
  return state.appointments.some(a =>
    a.scheduled_date === filterDate.value &&
    a.barber_id === barberId &&
    a.status !== 'CANCELLED' &&
    a.is_day_block
  );
}

async function handleToggleShopDay() {
  if (!isMaster.value) return;
  try {
    togglingBlock.value = true;
    if (isShopClosedOnDay.value) {
      await reopenDaySchedule({ date: filterDate.value, openShop: true });
    } else {
      await closeDaySchedule({ date: filterDate.value, closeShop: true });
    }
  } catch (err) {
    alert(err.message || 'Erro ao atualizar fechamento da loja.');
  } finally {
    togglingBlock.value = false;
  }
}

async function handleToggleBarberDay(barberId) {
  if (!barberId) return;
  try {
    togglingBlock.value = true;
    if (isBarberClosedOnDay(barberId)) {
      await reopenDaySchedule({ date: filterDate.value, barberId, openShop: false });
    } else {
      await closeDaySchedule({ date: filterDate.value, barberId, closeShop: false });
    }
  } catch (err) {
    alert(err.message || 'Erro ao atualizar fechamento de horário.');
  } finally {
    togglingBlock.value = false;
  }
}

const activeServices = computed(() => {
  return (state.services || []).filter(s => (s.is_active ?? s.isActive) !== false);
});

const isToday = computed(() => {
  const todayStr = toLocalIsoDate(new Date());
  return filterDate.value === todayStr;
});

const formattedDateDisplay = computed(() => {
  const options = { weekday: 'short', day: '2-digit', month: 'short' };
  const str = currentDateObj.value.toLocaleDateString('pt-BR', options);
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
    if (app.is_day_block) return false;
    if (!isMaster.value && app.barber_id !== state.currentUser.id) return false;
    if (isMaster.value && selectedBarberFilter.value !== 'ALL' && app.barber_id !== selectedBarberFilter.value) return false;
    if (app.scheduled_date !== filterDate.value) return false;
    if (filterStatus.value !== 'ALL' && app.status !== filterStatus.value) return false;
    return true;
  });
});

function openNewModal() {
  const defaultService = activeServices.value[0];
  form.value = {
    client_name: '',
    client_phone: '',
    barber_id: state.currentUser?.id || state.users[0]?.id || '',
    service_id: defaultService?.id || '',
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
    formPriceDisplay.value = ((service.price_cents ?? service.priceCents) || 0) / 100;
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

function openEditPriceModal(app) {
  editingPriceApp.value = app;
  editSelectedServiceId.value = app.service_id || state.services[0]?.id || '';
  quickEditPriceDisplay.value = (app.price_cents || 0) / 100;
}

function onEditServiceChange() {
  const service = state.services.find(s => s.id === editSelectedServiceId.value);
  if (service) {
    quickEditPriceDisplay.value = ((service.price_cents ?? service.priceCents) || 0) / 100;
  }
}

function savePriceEdit() {
  if (editingPriceApp.value) {
    const service = state.services.find(s => s.id === editSelectedServiceId.value);
    const serviceName = service ? service.name : editingPriceApp.value.service_name;
    const priceCents = Math.round(quickEditPriceDisplay.value * 100);

    updateAppointmentService(
      editingPriceApp.value.id,
      editSelectedServiceId.value,
      serviceName,
      priceCents
    );
    editingPriceApp.value = null;
  }
}

function openCompleteModal(app) {
  completingApp.value = app;
  completeFinalPriceDisplay.value = (app.price_cents || 0) / 100;
  completePaymentMethod.value = 'PIX';
}

function confirmComplete() {
  if (completingApp.value) {
    const newPriceCents = Math.round(completeFinalPriceDisplay.value * 100);
    updateAppointmentPrice(completingApp.value.id, newPriceCents);
    updateAppointmentStatus(completingApp.value.id, 'COMPLETED', completePaymentMethod.value);
    completingApp.value = null;
  }
}

function cancelAppointment(id) {
  if (confirm('Cancelar este agendamento?')) {
    updateAppointmentStatus(id, 'CANCELLED');
  }
}

function getStatusClass(status) {
  if (status === 'COMPLETED') return 'dot-green';
  if (status === 'CANCELLED') return 'dot-red';
  return 'dot-blue';
}
</script>

<style scoped>
.agenda-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Header - Open, clean, no box */
.agenda-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.header-actions-row {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.btn-close-day {
  background: rgba(239, 68, 68, 0.14);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.35);
  font-weight: 600;
}

.btn-close-day:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.24);
  color: #fecaca;
}

.btn-reopen-day {
  background: rgba(16, 185, 129, 0.16);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.4);
  font-weight: 600;
}

.btn-reopen-day:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.26);
  color: #a7f3d0;
}

.day-availability-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid var(--border-hairline);
}

.day-availability-bar.shop-closed-banner {
  background: rgba(239, 68, 68, 0.09);
  border-color: rgba(239, 68, 68, 0.32);
}

.availability-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.shop-closed-banner .availability-title {
  color: #fca5a5;
}

.barber-availability-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.barber-chip-item {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.32rem 0.6rem;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 0.76rem;
}

.barber-chip-item.chip-closed {
  border-color: rgba(239, 68, 68, 0.4);
  background: rgba(239, 68, 68, 0.1);
}

.chip-name {
  font-weight: 600;
  color: var(--text-primary);
}

.chip-status {
  font-size: 0.7rem;
  padding: 0.1rem 0.38rem;
  border-radius: 4px;
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  font-weight: 600;
}

.chip-closed .chip-status {
  background: rgba(239, 68, 68, 0.2);
  color: #fca5a5;
}

.chip-toggle-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: var(--text-primary);
  font-size: 0.72rem;
  font-weight: 600;
  padding: 0.2rem 0.5rem;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip-toggle-btn:hover:not(:disabled) {
  border-color: var(--accent-gold);
  color: #fbbf24;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 1rem;
  flex-wrap: wrap;
}

.page-title {
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.inline-date-nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.nav-arrow {
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 1.25rem;
  cursor: pointer;
  padding: 0 0.2rem;
  line-height: 1;
}

.nav-arrow:hover {
  color: var(--text-primary);
}

.nav-date-text {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
}

.today-link {
  background: transparent;
  border: none;
  color: var(--accent-gold);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  text-decoration: underline;
  text-underline-offset: 3px;
}

/* Subbar with Text Links/Chips */
.agenda-subbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-hairline);
  padding-bottom: 0.65rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.status-tabs {
  display: flex;
  gap: 0.85rem;
}

.tab-link {
  background: transparent;
  border: none;
  color: var(--text-tertiary);
  font-size: 0.825rem;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s ease;
}

.tab-link:hover {
  color: var(--text-secondary);
}

.tab-link.active {
  color: var(--accent-gold);
  font-weight: 600;
}

.barber-filter-inline {
  display: flex;
  align-items: center;
}

.minimal-filter-select {
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 0.785rem;
  cursor: pointer;
  outline: none;
}

.minimal-filter-select option {
  background: #111520;
}

/* TIMELINE LIST - HAIRLINE DIVIDERS, ZERO CHUNKY BOXES */
.timeline-container {
  display: flex;
  flex-direction: column;
}

.empty-timeline {
  padding: 3rem 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.85rem;
}

.timeline-list {
  display: flex;
  flex-direction: column;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem 0;
  border-bottom: 1px solid var(--border-hairline);
  transition: background-color 0.15s ease;
}

.timeline-item:hover {
  background: rgba(255, 255, 255, 0.015);
}

.item-done {
  opacity: 0.65;
}

.time-block {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 60px;
}

.time-digit {
  font-size: 0.95rem;
  font-weight: 600;
  font-family: var(--font-mono);
  color: #fbbf24;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-blue { background: #38bdf8; }
.dot-green { background: #10b981; }
.dot-red { background: #f43f5e; }

.info-block {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.client-name {
  font-size: 0.925rem;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.service-meta {
  font-size: 0.785rem;
  color: var(--text-secondary);
}

.barber-name {
  color: var(--text-tertiary);
}

.payment-note {
  color: #10b981;
}

.actions-block {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.price-wrapper {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.price-num {
  font-size: 0.95rem;
  font-weight: 600;
  font-family: var(--font-mono);
  color: var(--text-primary);
}

.edit-price-btn {
  background: transparent;
  border: none;
  color: var(--accent-gold);
  cursor: pointer;
  padding: 2px 4px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pencil-icon {
  width: 14px;
  height: 14px;
  stroke: var(--accent-gold);
  transition: transform 0.15s ease, stroke 0.15s ease;
}

.edit-price-btn:hover .pencil-icon {
  stroke: #fbbf24;
  transform: scale(1.15);
}

.item-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.cancel-link {
  background: transparent;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 0 0.3rem;
  font-size: 0.85rem;
}

.cancel-link:hover {
  color: #f43f5e;
}

.done-tag {
  font-size: 0.725rem;
  color: var(--text-tertiary);
}

/* Modals */
.modal-header {
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid var(--border-hairline);
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

.edit-summary {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.price-lg {
  font-size: 1.35rem;
  font-weight: 700;
  color: #fbbf24;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.highlight-box {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-hairline);
  padding: 0.75rem;
  border-radius: 6px;
}

.price-input-group {
  margin-top: 0.5rem;
}

.price-bold-input {
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
  border-top: 1px solid var(--border-hairline);
}

.payment-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.4rem;
  margin-top: 0.35rem;
}

.pay-chip {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: #151a26;
  border: 1px solid var(--border-hairline);
  padding: 0.55rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8rem;
}

.pay-chip.active {
  border-color: var(--accent-gold);
  background: rgba(217, 151, 38, 0.15);
}
</style>
