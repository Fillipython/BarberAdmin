<template>
  <div class="dashboard-container">
    <!-- Top Filter Bar -->
    <div class="filter-row clean-panel">
      <div class="filter-controls">
        <!-- View Mode: Dia vs Mês -->
        <div class="segmented-control">
          <button 
            class="seg-btn" 
            :class="{ active: viewPeriod === 'day' }" 
            @click="viewPeriod = 'day'"
          >
            Diário
          </button>
          <button 
            class="seg-btn" 
            :class="{ active: viewPeriod === 'month' }" 
            @click="viewPeriod = 'month'"
          >
            Mensal
          </button>
        </div>

        <!-- Date selector (if Day) -->
        <div v-if="viewPeriod === 'day'" class="picker-wrap">
          <label class="picker-label">Data:</label>
          <input type="date" v-model="selectedDate" class="input-flat" />
        </div>

        <!-- Month selector (if Month) -->
        <div v-else class="picker-wrap">
          <label class="picker-label">Mês:</label>
          <select v-model="selectedMonth" class="input-flat">
            <option v-for="(m, idx) in monthsList" :key="idx" :value="idx + 1">
              {{ m }} / 2026
            </option>
          </select>
        </div>
      </div>

      <div class="period-summary-text">
        <span>Exibindo:</span>
        <strong>{{ periodLabel }}</strong>
      </div>
    </div>

    <!-- MAIN FINANCIAL HERO (RENDIMENTO) -->
    <div class="clean-panel hero-finance">
      <div class="finance-primary">
        <span class="finance-subtitle">
          {{ isMaster 
              ? (viewPeriod === 'day' ? 'Rendimento Total do Dia' : 'Rendimento Total do Mês') 
              : 'Sua Produção no Período' }}
        </span>
        <div class="finance-value gold-text">
          {{ formatCurrency(currentMetrics.totalGross) }}
        </div>
      </div>

      <div class="finance-breakdown-row">
        <div class="breakdown-col">
          <span class="breakdown-label">
            {{ isMaster ? 'Líquido Barbearia' : 'Sua Comissão' }}
          </span>
          <span class="breakdown-value emerald-text">
            {{ formatCurrency(isMaster ? currentMetrics.shopNet : currentMetrics.barberCommission) }}
          </span>
        </div>

        <div v-if="isMaster" class="breakdown-col">
          <span class="breakdown-label">Comissões da Equipe</span>
          <span class="breakdown-value">
            {{ formatCurrency(currentMetrics.barberCommission) }}
          </span>
        </div>

        <div class="breakdown-col">
          <span class="breakdown-label">Atendimentos</span>
          <span class="breakdown-value">{{ currentMetrics.completedCount }}</span>
        </div>

        <div class="breakdown-col">
          <span class="breakdown-label">Ticket Médio</span>
          <span class="breakdown-value">{{ formatCurrency(currentMetrics.averageTicket) }}</span>
        </div>
      </div>
    </div>

    <!-- COMPARISON CHARTS (PIE / DONUT CHARTS) -->
    <div class="charts-row">
      <!-- Donut Chart 1: Formas de Pagamento -->
      <div class="clean-panel chart-box">
        <div class="chart-header">
          <h3 class="chart-title">Comparativo por Pagamento</h3>
          <span class="chart-sub">Distribuição da receita</span>
        </div>

        <div v-if="currentMetrics.totalGross === 0" class="no-data-chart">
          Sem movimentações no período selecionado
        </div>

        <div v-else class="chart-layout">
          <!-- SVG Pie / Donut -->
          <div class="svg-wrap">
            <svg viewBox="0 0 100 100" class="donut-svg">
              <circle
                v-for="slice in paymentDonutSlices"
                :key="slice.key"
                cx="50"
                cy="50"
                r="36"
                fill="transparent"
                :stroke="slice.color"
                stroke-width="14"
                :stroke-dasharray="`${slice.dash} ${100 - slice.dash}`"
                :stroke-dashoffset="slice.offset"
              />
            </svg>
            <div class="donut-center">
              <span class="center-count">{{ currentMetrics.completedCount }}</span>
              <span class="center-sub">vendas</span>
            </div>
          </div>

          <!-- Legend -->
          <div class="chart-legend">
            <div 
              v-for="item in paymentBreakdown" 
              :key="item.label" 
              class="legend-item"
            >
              <div class="legend-color" :style="{ backgroundColor: item.color }"></div>
              <div class="legend-info">
                <span class="legend-name">{{ item.label }}</span>
                <span class="legend-val">{{ formatCurrency(item.value) }} ({{ item.percent }}%)</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Donut Chart 2: Produção por Barbeiro (Master) ou Divisão de Ganho (Staff) -->
      <div class="clean-panel chart-box">
        <div class="chart-header">
          <h3 class="chart-title">
            {{ isMaster ? 'Participação por Barbeiro' : 'Divisão de Receita' }}
          </h3>
          <span class="chart-sub">
            {{ isMaster ? 'Comparação entre profissionais' : 'Sua comissão vs Barbearia' }}
          </span>
        </div>

        <div v-if="currentMetrics.totalGross === 0" class="no-data-chart">
          Sem movimentações no período selecionado
        </div>

        <div v-else class="chart-layout">
          <!-- SVG Pie / Donut -->
          <div class="svg-wrap">
            <svg viewBox="0 0 100 100" class="donut-svg">
              <circle
                v-for="slice in secondaryDonutSlices"
                :key="slice.key"
                cx="50"
                cy="50"
                r="36"
                fill="transparent"
                :stroke="slice.color"
                stroke-width="14"
                :stroke-dasharray="`${slice.dash} ${100 - slice.dash}`"
                :stroke-dashoffset="slice.offset"
              />
            </svg>
            <div class="donut-center">
              <span class="center-count">{{ secondaryChartItems.length }}</span>
              <span class="center-sub">{{ isMaster ? 'barbeiros' : 'partes' }}</span>
            </div>
          </div>

          <!-- Legend -->
          <div class="chart-legend">
            <div 
              v-for="item in secondaryChartItems" 
              :key="item.label" 
              class="legend-item"
            >
              <div class="legend-color" :style="{ backgroundColor: item.color }"></div>
              <div class="legend-info">
                <span class="legend-name">{{ item.label }}</span>
                <span class="legend-val">{{ formatCurrency(item.value) }} ({{ item.percent }}%)</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- RECENT TRANSACTIONS / SERVICES COMPLETED -->
    <div class="clean-panel list-panel">
      <div class="panel-header-simple">
        <h3 class="panel-heading">Atendimentos Concluídos no Período</h3>
      </div>

      <div v-if="periodAppointments.length === 0" class="empty-list-row">
        Nenhum atendimento concluído encontrado para este período.
      </div>

      <div v-else class="table-wrap">
        <table class="compact-table">
          <thead>
            <tr>
              <th>Horário</th>
              <th>Cliente</th>
              <th v-if="isMaster">Barbeiro</th>
              <th>Serviço</th>
              <th>Pagamento</th>
              <th class="text-right">Valor</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="app in periodAppointments" :key="app.id">
              <td class="text-mono">{{ app.scheduled_time || '09:00' }}</td>
              <td class="bold">{{ app.client_name }}</td>
              <td v-if="isMaster">{{ app.barber_name }}</td>
              <td>{{ app.service_name }}</td>
              <td>
                <span class="method-tag">{{ app.payment_method || 'PIX' }}</span>
              </td>
              <td class="text-right bold gold-text">{{ formatCurrency(app.price_cents) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { state, isMaster, formatCurrency } from '../store/state.js';

const viewPeriod = ref('day'); // 'day' | 'month'
const selectedDate = ref(new Date().toISOString().split('T')[0]);
const selectedMonth = ref(10); // Outubro

const monthsList = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
];

const periodLabel = computed(() => {
  if (viewPeriod.value === 'day') {
    const parts = selectedDate.value.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return selectedDate.value;
  }
  return `${monthsList[selectedMonth.value - 1]} de 2026`;
});

// Filter appointments for the period
const periodAppointments = computed(() => {
  return state.appointments.filter(app => {
    if (app.status !== 'COMPLETED') return false;

    // Master sees all; staff sees only his own
    if (!isMaster.value && app.barber_id !== state.currentUser.id) {
      return false;
    }

    if (viewPeriod.value === 'day') {
      return app.scheduled_date === selectedDate.value;
    } else {
      // Month match (e.g. "2026-10-xx")
      const monthStr = String(selectedMonth.value).padStart(2, '0');
      return app.scheduled_date && app.scheduled_date.startsWith(`2026-${monthStr}`);
    }
  });
});

// Metrics
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

// Payment Breakdown for Donut Chart 1
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

// Generate SVG circle dash offsets for Donut Chart 1
// Circumference of radius 36 is ~226.2, we normalize to 100 in stroke-dasharray (circumference 226)
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

// Secondary Chart Items (Master: Barbers Comparison; Staff: Commission vs Shop)
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
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* Filter Row */
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.65rem 1rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.segmented-control {
  display: flex;
  background: #080a0f;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xs);
  padding: 2px;
}

.seg-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-xs);
  cursor: pointer;
  transition: all 0.15s ease;
}

.seg-btn.active {
  background: var(--bg-subtle);
  color: var(--text-primary);
}

.picker-wrap {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.picker-label {
  font-size: 0.725rem;
  color: var(--text-tertiary);
  text-transform: uppercase;
  font-weight: 600;
}

.input-flat {
  background: #080a0f;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xs);
  padding: 0.35rem 0.6rem;
  color: var(--text-primary);
  font-size: 0.8rem;
  outline: none;
}

.period-summary-text {
  font-size: 0.785rem;
  color: var(--text-secondary);
  display: flex;
  gap: 0.35rem;
}

/* HERO FINANCIAL CARD */
.hero-finance {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  border-left: 3px solid var(--accent-gold);
}

.finance-primary {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.finance-subtitle {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--text-secondary);
}

.finance-value {
  font-size: 2.25rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.1;
  font-family: var(--font-body);
}

.gold-text {
  color: #fbbf24;
}

.emerald-text {
  color: #34d399;
}

.finance-breakdown-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-subtle);
}

.breakdown-col {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.breakdown-label {
  font-size: 0.7rem;
  color: var(--text-tertiary);
  text-transform: uppercase;
  font-weight: 600;
}

.breakdown-value {
  font-size: 1.15rem;
  font-weight: 600;
}

/* CHARTS ROW */
.charts-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
}

.chart-box {
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.chart-header {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.chart-title {
  font-size: 0.95rem;
  font-weight: 600;
}

.chart-sub {
  font-size: 0.725rem;
  color: var(--text-tertiary);
}

.no-data-chart {
  padding: 2.5rem 1rem;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.8rem;
}

.chart-layout {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.svg-wrap {
  position: relative;
  width: 130px;
  height: 130px;
  flex-shrink: 0;
}

.donut-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.center-count {
  font-size: 1.2rem;
  font-weight: 700;
  line-height: 1;
}

.center-sub {
  font-size: 0.65rem;
  color: var(--text-tertiary);
  text-transform: uppercase;
}

.chart-legend {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex: 1;
  min-width: 140px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.legend-color {
  width: 9px;
  height: 9px;
  border-radius: var(--radius-xs);
  flex-shrink: 0;
}

.legend-info {
  display: flex;
  flex-direction: column;
}

.legend-name {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-primary);
}

.legend-val {
  font-size: 0.7rem;
  color: var(--text-secondary);
}

/* LIST PANEL */
.list-panel {
  padding: 1rem 1.25rem;
}

.panel-header-simple {
  margin-bottom: 0.75rem;
}

.panel-heading {
  font-size: 0.95rem;
  font-weight: 600;
}

.empty-list-row {
  padding: 1.5rem 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.8rem;
}

.table-wrap {
  overflow-x: auto;
}

.compact-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 0.825rem;
}

.compact-table th {
  padding: 0.5rem 0.75rem;
  color: var(--text-tertiary);
  font-size: 0.7rem;
  text-transform: uppercase;
  font-weight: 600;
  border-bottom: 1px solid var(--border-subtle);
}

.compact-table td {
  padding: 0.65rem 0.75rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
}

.text-mono {
  font-family: var(--font-mono);
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.text-right {
  text-align: right;
}

.bold {
  font-weight: 600;
}

.method-tag {
  background: var(--bg-subtle);
  border: 1px solid var(--border-subtle);
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-xs);
  font-size: 0.675rem;
  font-weight: 600;
}
</style>
