<template>
  <div class="dashboard-content">
    <!-- Top Bar: Period Switcher & Date Navigation -->
    <div class="period-header">
      <div class="period-tabs">
        <button 
          class="period-tab" 
          :class="{ active: viewPeriod === 'day' }" 
          @click="viewPeriod = 'day'"
        >
          Diário
        </button>
        <button 
          class="period-tab" 
          :class="{ active: viewPeriod === 'month' }" 
          @click="viewPeriod = 'month'"
        >
          Mensal
        </button>
      </div>

      <!-- Day Stepper (if Day) -->
      <div v-if="viewPeriod === 'day'" class="inline-date-nav">
        <button class="nav-arrow" @click="changeDay(-1)">‹</button>
        <span class="nav-date-text">{{ formattedDateDisplay }}</span>
        <button class="nav-arrow" @click="changeDay(1)">›</button>
      </div>

      <!-- Month Selector (if Month) -->
      <div v-else class="month-select-wrap">
        <select v-model="selectedMonth" class="minimal-select">
          <option v-for="(m, idx) in monthsList" :key="idx" :value="idx + 1">
            {{ m }} de 2026
          </option>
        </select>
      </div>
    </div>

    <!-- CARD DE PERFIL PÚBLICO DO BARBEIRO (INTEGRADO COM BARBERNOTIFY) -->
    <section v-if="state.currentUser" class="barber-profile-banner">
      <div class="profile-banner-left">
        <div class="banner-avatar-wrap">
          <img :src="state.currentUser.avatar_url" :alt="state.currentUser.name" class="banner-avatar-img" />
          <span v-if="uploadingPhoto" class="banner-avatar-Wait">...</span>
        </div>
        <div class="banner-user-info">
          <div class="banner-name-row">
            <h2 class="banner-name">{{ state.currentUser.name }}</h2>
            <span class="banner-role-badge" :class="isMaster ? 'role-master' : 'role-barber'">
              {{ isMaster ? 'Master' : 'Barbeiro' }}
            </span>
          </div>
          <p class="banner-sync-subtitle">
            Sua foto é sincronizada automaticamente com a vitrine de clientes no <strong>BarberNotify</strong>.
          </p>
        </div>
      </div>

      <div class="profile-banner-actions">
        <label class="btn-photo-action btn-photo-upload" :class="{ disabled: uploadingPhoto }">
          <input 
            type="file" 
            accept="image/png,image/jpeg,image/webp" 
            @change="onSelectNewPhoto" 
            style="display:none;" 
            :disabled="uploadingPhoto"
          />
          <span>{{ uploadingPhoto ? 'Enviando...' : 'Mudar Foto' }}</span>
        </label>
        <button 
          type="button" 
          class="btn-photo-action btn-photo-delete" 
          @click="onDeleteCurrentPhoto"
          :disabled="uploadingPhoto"
        >
          Excluir Foto
        </button>

        <button
          v-if="isMaster"
          type="button"
          class="btn-photo-action"
          :class="isShopClosedOnDay ? 'btn-day-reopen' : 'btn-day-close'"
          :disabled="togglingBlock"
          @click="handleToggleShopDay"
        >
          {{ togglingBlock ? 'Processando...' : (isShopClosedOnDay ? 'Reabrir Loja no Dia' : 'Fechar Loja no Dia') }}
        </button>

        <button
          v-else
          type="button"
          class="btn-photo-action"
          :class="isBarberClosedOnDay(state.currentUser?.id) ? 'btn-day-reopen' : 'btn-day-close'"
          :disabled="togglingBlock || isShopClosedByMasterOnDay"
          @click="handleToggleBarberDay(state.currentUser?.id)"
        >
          {{
            isShopClosedByMasterOnDay
              ? 'Loja Fechada pelo Master'
              : (togglingBlock ? 'Processando...' : (isBarberClosedOnDay(state.currentUser?.id) ? 'Reabrir Meu Horário' : 'Fechar Meu Horário'))
          }}
        </button>
      </div>
    </section>

    <!-- CONTROLE RÁPIDO DE DISPONIBILIDADE DOS BARBEIROS NO DIA (PARA O MASTER) -->
    <section v-if="isMaster && viewPeriod === 'day'" class="master-day-control-bar">
      <span class="master-control-label">
        Disponibilidade no dia ({{ formattedDateDisplay }}):
      </span>
      <div class="master-barber-pills">
        <div
          v-for="b in state.users"
          :key="b.id"
          class="master-barber-pill"
          :class="{ 'pill-closed': isBarberClosedOnDay(b.id) }"
        >
          <span class="pill-barber-name">{{ b.nickname || b.name }}</span>
          <span class="pill-barber-state">{{ isBarberClosedOnDay(b.id) ? 'Fechado' : 'Aberto' }}</span>
          <button
            type="button"
            class="pill-barber-btn"
            :disabled="togglingBlock"
            @click="handleToggleBarberDay(b.id)"
          >
            {{ isBarberClosedOnDay(b.id) ? 'Reabrir' : 'Fechar Horário' }}
          </button>
        </div>
      </div>
    </section>

    <!-- VISÃO FINANCEIRA COMPLETA (EXCLUSIVA PARA PERFIL MASTER) -->
    <template v-if="isMaster">
      <!-- HERO FINANCIAL STATEMENT (OPEN, NON-BOXED, FLUID) -->
      <section class="balance-hero">
        <span class="balance-title">
          {{ viewPeriod === 'day' ? 'Faturamento Diário' : 'Faturamento Mensal' }}
        </span>

        <div class="balance-amount">
          {{ formatCurrency(currentMetrics.totalGross) }}
        </div>

        <!-- Inline stats with subtle dots -->
        <div class="balance-substats">
          <span>Líquido: <strong class="emerald-text">{{ formatCurrency(currentMetrics.shopNet) }}</strong></span>
          <span class="dot-sep">·</span>
          <span>Comissões: <strong>{{ formatCurrency(currentMetrics.barberCommission) }}</strong></span>
          <span class="dot-sep">·</span>
          <span>{{ currentMetrics.completedCount }} atendimentos concluídos</span>
          <span class="dot-sep">·</span>
          <span>Ticket Médio: {{ formatCurrency(currentMetrics.averageTicket) }}</span>
        </div>
      </section>

      <!-- COMPARISON SECTION (PIE / DONUT CHARTS) -->
      <section class="analytics-section">
        <h2 class="section-title">Comparativo de Receita</h2>

        <div v-if="currentMetrics.totalGross === 0" class="empty-analytics">
          Sem movimentações financeiras para o período selecionado.
        </div>

        <div v-else class="donuts-grid">
          <!-- Donut 1: Formas de Pagamento -->
          <div class="donut-card">
            <div class="donut-heading">
              <h3>Por Forma de Pagamento</h3>
              <span class="donut-desc">Distribuição da receita</span>
            </div>

            <div class="donut-body">
              <div class="svg-container">
                <svg viewBox="0 0 100 100" class="donut-svg">
                  <circle
                    v-for="slice in paymentDonutSlices"
                    :key="slice.key"
                    cx="50"
                    cy="50"
                    r="36"
                    fill="transparent"
                    :stroke="slice.color"
                    stroke-width="12"
                    :stroke-dasharray="`${slice.dash} ${100 - slice.dash}`"
                    :stroke-dashoffset="slice.offset"
                  />
                </svg>
                <div class="donut-center-info">
                  <span class="center-number">{{ currentMetrics.completedCount }}</span>
                  <span class="center-label">vendas</span>
                </div>
              </div>

              <div class="donut-legend">
                <div 
                  v-for="item in paymentBreakdown" 
                  :key="item.label" 
                  class="legend-row"
                >
                  <div class="legend-swatch" :style="{ backgroundColor: item.color }"></div>
                  <span class="legend-name">{{ item.label }}</span>
                  <span class="legend-value">{{ formatCurrency(item.value) }}</span>
                  <span class="legend-percent">{{ item.percent }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Donut 2: Participação por Barbeiro (Master) -->
          <div class="donut-card">
            <div class="donut-heading">
              <h3>Por Profissional</h3>
              <span class="donut-desc">Participação de cada barbeiro</span>
            </div>

            <div class="donut-body">
              <div class="svg-container">
                <svg viewBox="0 0 100 100" class="donut-svg">
                  <circle
                    v-for="slice in secondaryDonutSlices"
                    :key="slice.key"
                    cx="50"
                    cy="50"
                    r="36"
                    fill="transparent"
                    :stroke="slice.color"
                    stroke-width="12"
                    :stroke-dasharray="`${slice.dash} ${100 - slice.dash}`"
                    :stroke-dashoffset="slice.offset"
                  />
                </svg>
                <div class="donut-center-info">
                  <span class="center-number">{{ secondaryChartItems.length }}</span>
                  <span class="center-label">equipe</span>
                </div>
              </div>

              <div class="donut-legend">
                <div 
                  v-for="item in secondaryChartItems" 
                  :key="item.label" 
                  class="legend-row"
                >
                  <div class="legend-swatch" :style="{ backgroundColor: item.color }"></div>
                  <span class="legend-name">{{ item.label }}</span>
                  <span class="legend-value">{{ formatCurrency(item.value) }}</span>
                  <span class="legend-percent">{{ item.percent }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- RECENT ATENDIMENTOS TIMELINE (MASTER) -->
      <section class="recent-section">
        <h2 class="section-title">Atendimentos Concluídos</h2>

        <div v-if="periodAppointments.length === 0" class="empty-list">
          Nenhum atendimento finalizado neste período.
        </div>

        <div v-else class="receipts-list">
          <div v-for="app in periodAppointments" :key="app.id" class="receipt-row">
            <span class="receipt-time">{{ app.scheduled_time || '09:00' }}</span>
            <div class="receipt-info">
              <span class="receipt-client">{{ app.client_name }}</span>
              <span class="receipt-service">{{ app.service_name }} · {{ app.barber_name }}</span>
            </div>
            <span class="receipt-method">{{ app.payment_method || 'PIX' }}</span>
            <span class="receipt-price">{{ formatCurrency(app.price_cents) }}</span>
          </div>
        </div>
      </section>
    </template>

    <!-- VISÃO OPERACIONAL DO BARBEIRO (SEM EXIBIR VALORES MONETÁRIOS NA PÁGINA INICIAL) -->
    <template v-else>
      <section class="balance-hero">
        <span class="balance-title">
          {{ viewPeriod === 'day' ? 'Resumo de Atendimentos do Dia' : 'Resumo de Atendimentos do Mês' }}
        </span>

        <div class="balance-amount">
          {{ barberAllPeriodAppointments.length }} {{ barberAllPeriodAppointments.length === 1 ? 'agendamento' : 'agendamentos' }}
        </div>

        <div class="balance-substats">
          <span>Aguardando / Confirmados: <strong class="amber-text">{{ barberPendingCount }}</strong></span>
          <span class="dot-sep">·</span>
          <span>Concluídos: <strong class="emerald-text">{{ currentMetrics.completedCount }}</strong></span>
        </div>
      </section>

      <section class="recent-section">
        <div class="section-header-row">
          <h2 class="section-title">Meus Atendimentos no Período</h2>
          <router-link to="/appointments" class="link-agenda">Ir para Agenda →</router-link>
        </div>

        <div v-if="barberAllPeriodAppointments.length === 0" class="empty-list">
          Nenhum atendimento agendado ou finalizado neste período.
        </div>

        <div v-else class="receipts-list">
          <div v-for="app in barberAllPeriodAppointments" :key="app.id" class="receipt-row">
            <span class="receipt-time">{{ app.scheduled_time || '09:00' }}</span>
            <div class="receipt-info">
              <span class="receipt-client">{{ app.client_name }}</span>
              <span class="receipt-service">{{ app.service_name }}</span>
            </div>
            <span 
              class="status-pill" 
              :class="app.status === 'COMPLETED' ? 'pill-completed' : 'pill-scheduled'"
            >
              {{ app.status === 'COMPLETED' ? 'Concluído' : 'Confirmado' }}
            </span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import {
  state,
  isMaster,
  formatCurrency,
  toLocalIsoDate,
  uploadBarberAvatar,
  removeBarberAvatar,
  syncDateFromBackend,
  closeDaySchedule,
  reopenDaySchedule
} from '../store/state.js';

const viewPeriod = ref('day');
const currentDateObj = ref(new Date());
const selectedMonth = ref(new Date().getMonth() + 1);
const uploadingPhoto = ref(false);
const togglingBlock = ref(false);

const selectedDate = computed(() => {
  return toLocalIsoDate(currentDateObj.value);
});

watch(selectedDate, (newDate) => {
  syncDateFromBackend(newDate);
}, { immediate: true });

const currentDayBlock = computed(() => {
  return state.dayBlocks?.[selectedDate.value] || { shopClosed: false, shopClosedByMaster: false, closedBarberIds: [] };
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
    a.scheduled_date === selectedDate.value &&
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
      await reopenDaySchedule({ date: selectedDate.value, openShop: true });
    } else {
      await closeDaySchedule({ date: selectedDate.value, closeShop: true });
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
      await reopenDaySchedule({ date: selectedDate.value, barberId, openShop: false });
    } else {
      await closeDaySchedule({ date: selectedDate.value, barberId, closeShop: false });
    }
  } catch (err) {
    alert(err.message || 'Erro ao atualizar fechamento de horário.');
  } finally {
    togglingBlock.value = false;
  }
}

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

const monthsList = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
];

async function onSelectNewPhoto(e) {
  const file = e.target.files?.[0];
  if (!file || !state.currentUser) return;

  if (file.size > 5 * 1024 * 1024) {
    alert('A imagem deve ter no máximo 5MB.');
    e.target.value = '';
    return;
  }

  try {
    uploadingPhoto.value = true;
    await uploadBarberAvatar(state.currentUser.id, file);
    alert('Foto atualizada e sincronizada com o BarberNotify!');
  } catch (err) {
    alert('Erro ao atualizar foto: ' + (err.message || 'Erro desconhecido'));
  } finally {
    uploadingPhoto.value = false;
    e.target.value = '';
  }
}

async function onDeleteCurrentPhoto() {
  if (!state.currentUser) return;
  if (confirm('Deseja excluir sua foto atual? O BarberNotify passará a exibir seu avatar neutro de iniciais para os clientes.')) {
    try {
      uploadingPhoto.value = true;
      await removeBarberAvatar(state.currentUser.id);
      alert('Foto excluída com sucesso e sincronizada com o BarberNotify!');
    } catch (err) {
      alert('Erro ao excluir foto: ' + (err.message || 'Erro desconhecido'));
    } finally {
      uploadingPhoto.value = false;
    }
  }
}

const barberAllPeriodAppointments = computed(() => {
  return state.appointments.filter(app => {
    if (app.is_day_block) return false;
    if (app.status === 'CANCELLED') return false;
    if (!isMaster.value && app.barber_id !== state.currentUser?.id) return false;

    if (viewPeriod.value === 'day') {
      return app.scheduled_date === selectedDate.value;
    } else {
      const year = currentDateObj.value.getFullYear();
      const monthStr = String(selectedMonth.value).padStart(2, '0');
      return app.scheduled_date && app.scheduled_date.startsWith(`${year}-${monthStr}`);
    }
  }).sort((a, b) => (a.scheduled_time || '').localeCompare(b.scheduled_time || ''));
});

const barberPendingCount = computed(() => {
  return barberAllPeriodAppointments.value.filter(a => a.status !== 'COMPLETED').length;
});

const periodAppointments = computed(() => {
  return state.appointments.filter(app => {
    if (app.is_day_block) return false;
    if (app.status !== 'COMPLETED') return false;
    if (!isMaster.value && app.barber_id !== state.currentUser?.id) return false;

    if (viewPeriod.value === 'day') {
      return app.scheduled_date === selectedDate.value;
    } else {
      const year = currentDateObj.value.getFullYear();
      const monthStr = String(selectedMonth.value).padStart(2, '0');
      return app.scheduled_date && app.scheduled_date.startsWith(`${year}-${monthStr}`);
    }
  });
});

const currentMetrics = computed(() => {
  let totalGross = 0;
  let barberCommission = 0;
  let shopNet = 0;
  let pix = 0;
  let creditCard = 0;
  let debitCard = 0;
  let cash = 0;

  periodAppointments.value.forEach(app => {
    const price = app.price_cents || 0;
    totalGross += price;

    const barber = state.users.find(u => u.id === app.barber_id);
    const rate = (barber?.commission_percentage || 50) / 100;
    const comm = Math.round(price * rate);
    barberCommission += comm;
    shopNet += (price - comm);

    if (app.payment_method === 'PIX') pix += price;
    else if (app.payment_method === 'CARTAO_CREDITO') creditCard += price;
    else if (app.payment_method === 'CARTAO_DEBITO') debitCard += price;
    else if (app.payment_method === 'DINHEIRO') cash += price;
    else pix += price;
  });

  const completedCount = periodAppointments.value.length;
  const averageTicket = completedCount > 0 ? Math.round(totalGross / completedCount) : 0;

  return {
    totalGross,
    barberCommission,
    shopNet,
    pix,
    creditCard,
    debitCard,
    cash,
    completedCount,
    averageTicket
  };
});

const paymentBreakdown = computed(() => {
  const total = currentMetrics.value.totalGross || 1;
  const items = [
    { label: 'PIX', value: currentMetrics.value.pix, color: '#10b981' },
    { label: 'Cartão Crédito', value: currentMetrics.value.creditCard, color: '#38bdf8' },
    { label: 'Cartão Débito', value: currentMetrics.value.debitCard, color: '#818cf8' },
    { label: 'Dinheiro', value: currentMetrics.value.cash, color: '#fbbf24' }
  ].filter(i => i.value > 0);

  return items.map(item => ({
    ...item,
    percent: Math.round((item.value / total) * 100)
  }));
});

const paymentDonutSlices = computed(() => {
  const total = currentMetrics.value.totalGross;
  if (!total) return [];

  let accumulated = 0;
  const circumference = 226.19;

  return paymentBreakdown.value.map(item => {
    const ratio = item.value / total;
    const dash = ratio * circumference;
    const offset = -accumulated;
    accumulated += dash;

    return {
      key: item.label,
      color: item.color,
      dash,
      offset
    };
  });
});

const secondaryChartItems = computed(() => {
  const total = currentMetrics.value.totalGross || 1;
  const palette = ['#d99726', '#38bdf8', '#a855f7', '#10b981'];
  return state.users.filter(u => u.is_active).map((b, idx) => {
    const bApps = periodAppointments.value.filter(a => a.barber_id === b.id);
    const val = bApps.reduce((acc, a) => acc + (a.price_cents || 0), 0);
    return {
      label: b.nickname || b.name,
      value: val,
      color: palette[idx % palette.length],
      percent: Math.round((val / total) * 100)
    };
  }).filter(i => i.value > 0);
});

const secondaryDonutSlices = computed(() => {
  const total = currentMetrics.value.totalGross;
  if (!total) return [];

  let accumulated = 0;
  const circumference = 226.19;

  return secondaryChartItems.value.map(item => {
    const ratio = item.value / total;
    const dash = ratio * circumference;
    const offset = -accumulated;
    accumulated += dash;

    return {
      key: item.label,
      color: item.color,
      dash,
      offset
    };
  });
});
</script>

<style scoped>
.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

/* Period Header - Simple border-bottom */
.period-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-hairline);
  padding-bottom: 0.75rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.period-tabs {
  display: flex;
  gap: 1rem;
}

.period-tab {
  background: transparent;
  border: none;
  color: var(--text-tertiary);
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s ease;
}

.period-tab:hover {
  color: var(--text-secondary);
}

.period-tab.active {
  color: var(--accent-gold);
  font-weight: 700;
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

.nav-date-text {
  font-size: 0.9rem;
  font-weight: 600;
}

.minimal-select {
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 0.85rem;
  cursor: pointer;
  outline: none;
}

.minimal-select option {
  background: #121622;
}

/* BARBER PROFILE BANNER */
.barber-profile-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.15rem;
  background: rgba(24, 30, 44, 0.65);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  flex-wrap: wrap;
}

.profile-banner-left {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.banner-avatar-wrap {
  position: relative;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  border: 2px solid var(--accent-gold);
  overflow: hidden;
  flex-shrink: 0;
  background: #0f172a;
}

.banner-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.banner-avatar-Wait {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}

.banner-user-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.banner-name-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.banner-name {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-primary);
}

.banner-role-badge {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  padding: 0.15rem 0.45rem;
  border-radius: 999px;
}

.role-master {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
  border: 1px solid rgba(251, 191, 36, 0.3);
}

.role-barber {
  background: rgba(56, 189, 248, 0.15);
  color: #38bdf8;
  border: 1px solid rgba(56, 189, 248, 0.3);
}

.banner-sync-subtitle {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.profile-banner-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn-photo-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  padding: 0.45rem 0.8rem;
  border-radius: 8px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-photo-upload {
  background: rgba(251, 191, 36, 0.14);
  border: 1px solid rgba(251, 191, 36, 0.4);
  color: #fbbf24;
}

.btn-photo-upload:hover {
  background: rgba(251, 191, 36, 0.24);
}

.btn-photo-delete {
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.35);
  color: #fca5a5;
}

.btn-photo-delete:hover {
  background: rgba(239, 68, 68, 0.22);
}

.btn-photo-action.disabled,
.btn-photo-action:disabled {
  opacity: 0.5;
  pointer-events: none;
}

/* BALANCE HERO - ZERO BOXED BORDERS */
.balance-hero {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 0.5rem 0;
}

.balance-title {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-tertiary);
}

.balance-amount {
  font-size: 2.75rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--text-primary);
  line-height: 1.1;
  font-family: var(--font-body);
}

.balance-substats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.45rem;
  font-size: 0.825rem;
  color: var(--text-secondary);
  margin-top: 0.25rem;
}

.dot-sep {
  color: var(--text-tertiary);
}

.emerald-text {
  color: #10b981;
}

.amber-text {
  color: #fbbf24;
}

/* ANALYTICS SECTION */
.analytics-section, .recent-section {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.link-agenda {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--accent-gold);
  text-decoration: none;
}

.link-agenda:hover {
  text-decoration: underline;
}

.section-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.empty-analytics, .empty-list {
  padding: 2rem 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.825rem;
}

.donuts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.donut-card {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  padding: 1rem 0;
  border-top: 1px solid var(--border-hairline);
}

.donut-heading h3 {
  font-size: 0.95rem;
  font-weight: 600;
}

.donut-desc {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.donut-body {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.svg-container {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.donut-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.donut-center-info {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.center-number {
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1;
}

.center-label {
  font-size: 0.65rem;
  color: var(--text-tertiary);
  text-transform: uppercase;
}

.donut-legend {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  flex: 1;
}

.legend-row {
  display: flex;
  align-items: center;
  font-size: 0.785rem;
  gap: 0.45rem;
}

.legend-swatch {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-name {
  color: var(--text-secondary);
  flex: 1;
}

.legend-value {
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 0.75rem;
}

.legend-percent {
  font-size: 0.7rem;
  color: var(--text-tertiary);
  min-width: 28px;
  text-align: right;
}

/* RECEIPTS TIMELINE */
.receipts-list {
  display: flex;
  flex-direction: column;
}

.receipt-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--border-hairline);
  font-size: 0.85rem;
}

.receipt-time {
  font-family: var(--font-mono);
  color: #fbbf24;
  font-size: 0.8rem;
  min-width: 45px;
}

.receipt-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.receipt-client {
  font-weight: 600;
  color: var(--text-primary);
}

.receipt-service {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.receipt-method {
  font-size: 0.7rem;
  color: #10b981;
  font-weight: 600;
}

.receipt-price {
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--text-primary);
}

.status-pill {
  font-size: 0.7rem;
  font-weight: 600;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
}

.pill-completed {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}

.pill-scheduled {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}

.btn-day-close {
  background: rgba(239, 68, 68, 0.14);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.35);
}

.btn-day-close:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.24);
  color: #fecaca;
}

.btn-day-reopen {
  background: rgba(16, 185, 129, 0.16);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.4);
}

.btn-day-reopen:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.26);
  color: #a7f3d0;
}

.master-day-control-bar {
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

.master-control-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.master-barber-pills {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.master-barber-pill {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.32rem 0.6rem;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 0.76rem;
}

.master-barber-pill.pill-closed {
  border-color: rgba(239, 68, 68, 0.4);
  background: rgba(239, 68, 68, 0.1);
}

.pill-barber-name {
  font-weight: 600;
  color: var(--text-primary);
}

.pill-barber-state {
  font-size: 0.7rem;
  padding: 0.1rem 0.38rem;
  border-radius: 4px;
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
  font-weight: 600;
}

.pill-closed .pill-barber-state {
  background: rgba(239, 68, 68, 0.2);
  color: #fca5a5;
}

.pill-barber-btn {
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

.pill-barber-btn:hover:not(:disabled) {
  border-color: var(--accent-gold);
  color: #fbbf24;
}
</style>
