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

    <!-- HERO FINANCIAL STATEMENT (OPEN, NON-BOXED, FLUID) -->
    <section class="balance-hero">
      <span class="balance-title">
        {{ isMaster 
            ? (viewPeriod === 'day' ? 'Faturamento Diário' : 'Faturamento Mensal') 
            : 'Sua Produção' }}
      </span>

      <div class="balance-amount">
        {{ formatCurrency(currentMetrics.totalGross) }}
      </div>

      <!-- Inline stats with subtle dots -->
      <div class="balance-substats">
        <span>Líquido: <strong class="emerald-text">{{ formatCurrency(isMaster ? currentMetrics.shopNet : currentMetrics.barberCommission) }}</strong></span>
        <span class="dot-sep">·</span>
        <span v-if="isMaster">Comissões: <strong>{{ formatCurrency(currentMetrics.barberCommission) }}</strong></span>
        <span v-if="isMaster" class="dot-sep">·</span>
        <span>{{ currentMetrics.completedCount }} atendimentos</span>
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

        <!-- Donut 2: Participação por Barbeiro (Master) ou Lucro vs Comissão (Staff) -->
        <div class="donut-card">
          <div class="donut-heading">
            <h3>{{ isMaster ? 'Por Profissional' : 'Divisão de Lucro' }}</h3>
            <span class="donut-desc">{{ isMaster ? 'Participação de cada barbeiro' : 'Sua comissão vs Barbearia' }}</span>
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
                <span class="center-label">{{ isMaster ? 'equipe' : 'partes' }}</span>
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

    <!-- RECENT ATENDIMENTOS TIMELINE -->
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
            <span class="receipt-service">{{ app.service_name }} <span v-if="isMaster">· {{ app.barber_name }}</span></span>
          </div>
          <span class="receipt-method">{{ app.payment_method || 'PIX' }}</span>
          <span class="receipt-price">{{ formatCurrency(app.price_cents) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { state, isMaster, formatCurrency } from '../store/state.js';

const viewPeriod = ref('day');
const currentDateObj = ref(new Date());
const selectedMonth = ref(10);

const selectedDate = computed(() => {
  return currentDateObj.value.toISOString().split('T')[0];
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

const monthsList = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
];

const periodAppointments = computed(() => {
  return state.appointments.filter(app => {
    if (app.status !== 'COMPLETED') return false;
    if (!isMaster.value && app.barber_id !== state.currentUser.id) return false;

    if (viewPeriod.value === 'day') {
      return app.scheduled_date === selectedDate.value;
    } else {
      const monthStr = String(selectedMonth.value).padStart(2, '0');
      return app.scheduled_date && app.scheduled_date.startsWith(`2026-${monthStr}`);
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

  if (isMaster.value) {
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
  } else {
    return [
      {
        label: 'Sua Comissão',
        value: currentMetrics.value.barberCommission,
        color: '#10b981',
        percent: Math.round((currentMetrics.value.barberCommission / total) * 100)
      },
      {
        label: 'Barbearia',
        value: currentMetrics.value.shopNet,
        color: '#d99726',
        percent: Math.round((currentMetrics.value.shopNet / total) * 100)
      }
    ];
  }
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

/* ANALYTICS SECTION */
.analytics-section, .recent-section {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
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
</style>
