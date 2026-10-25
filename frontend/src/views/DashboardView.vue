<template>
  <div class="dashboard-page">
    <!-- Top Action Bar -->
    <div class="page-header">
      <div>
        <div class="user-greeting">
          <span class="greeting-badge" :class="isMaster ? 'badge-gold' : 'badge-blue'">
            {{ isMaster ? 'Painel Master' : 'Espaço do Barbeiro' }}
          </span>
          <h1 class="page-title">Olá, {{ state.currentUser.nickname || state.currentUser.name }}</h1>
        </div>
        <p class="page-subtitle">
          {{ isMaster 
              ? 'Acompanhamento financeiro em tempo real e rendimento diário de toda a barbearia.' 
              : 'Seu resumo diário de atendimentos e comissões acumuladas.' }}
        </p>
      </div>

      <!-- Controls: Date filter & View Toggle -->
      <div class="header-controls">
        <div class="view-toggle">
          <button 
            class="toggle-btn" 
            :class="{ active: activeTab === 'daily' }" 
            @click="activeTab = 'daily'"
          >
            Diário
          </button>
          <button 
            v-if="isMaster"
            class="toggle-btn" 
            :class="{ active: activeTab === 'monthly' }" 
            @click="activeTab = 'monthly'"
          >
            Histórico Mensal
          </button>
        </div>

        <div v-if="activeTab === 'daily'" class="date-picker-wrap">
          <input type="date" v-model="selectedDate" class="form-input form-input-sm" />
        </div>
      </div>
    </div>

    <!-- DAILY VIEW -->
    <div v-if="activeTab === 'daily'" class="tab-content">
      <!-- KPI Stats Grid -->
      <div class="kpi-grid">
        <!-- Card 1: Faturamento Bruto / Rendimento -->
        <div class="glass-card stat-card border-gold">
          <div class="stat-icon-wrap gold-glow">
            <span class="icon-text">💰</span>
          </div>
          <div class="stat-info">
            <span class="stat-label">
              {{ isMaster ? 'Rendimento Total (Dia)' : 'Sua Produção (Dia)' }}
            </span>
            <h2 class="stat-value gold-text">{{ formatCurrency(dailyMetrics.totalGross) }}</h2>
            <span class="stat-sub">
              {{ isMaster ? 'Total bruto de todos os barbeiros' : 'Valor dos cortes realizados por você' }}
            </span>
          </div>
        </div>

        <!-- Card 2: Comissão / Ganho Líquido -->
        <div class="glass-card stat-card border-emerald">
          <div class="stat-icon-wrap emerald-glow">
            <span class="icon-text">📈</span>
          </div>
          <div class="stat-info">
            <span class="stat-label">
              {{ isMaster ? 'Líquido Barbearia (Caixa)' : 'Sua Comissão' }}
            </span>
            <h2 class="stat-value emerald-text">
              {{ formatCurrency(isMaster ? dailyMetrics.shopNet : dailyMetrics.barberCommission) }}
            </h2>
            <span class="stat-sub">
              {{ isMaster ? 'Após repasse das comissões' : `${state.currentUser.commission_percentage}% sobre seus serviços` }}
            </span>
          </div>
        </div>

        <!-- Card 3: Atendimentos Concluídos -->
        <div class="glass-card stat-card border-blue">
          <div class="stat-icon-wrap blue-glow">
            <span class="icon-text">✂️</span>
          </div>
          <div class="stat-info">
            <span class="stat-label">Atendimentos do Dia</span>
            <h2 class="stat-value blue-text">{{ dailyMetrics.completedCount }}</h2>
            <span class="stat-sub">{{ dailyMetrics.pendingCount }} agendamentos restantes</span>
          </div>
        </div>

        <!-- Card 4: Ticket Médio -->
        <div class="glass-card stat-card border-rose">
          <div class="stat-icon-wrap rose-glow">
            <span class="icon-text">🏷️</span>
          </div>
          <div class="stat-info">
            <span class="stat-label">Ticket Médio</span>
            <h2 class="stat-value rose-text">{{ formatCurrency(dailyMetrics.averageTicket) }}</h2>
            <span class="stat-sub">Média por cliente atendido</span>
          </div>
        </div>
      </div>

      <!-- Payment Breakdown (Master or Staff) -->
      <div class="payment-methods-card glass-card">
        <h3 class="card-title">Distribuição por Forma de Pagamento (Hoje)</h3>
        <div class="methods-grid">
          <div class="method-box">
            <span class="method-tag tag-pix">⚡ PIX</span>
            <span class="method-val">{{ formatCurrency(dailyMetrics.pix) }}</span>
          </div>
          <div class="method-box">
            <span class="method-tag tag-card">💳 Cartão de Crédito</span>
            <span class="method-val">{{ formatCurrency(dailyMetrics.creditCard) }}</span>
          </div>
          <div class="method-box">
            <span class="method-tag tag-debit">💳 Cartão de Débito</span>
            <span class="method-val">{{ formatCurrency(dailyMetrics.debitCard) }}</span>
          </div>
          <div class="method-box">
            <span class="method-tag tag-cash">💵 Dinheiro em Espécie</span>
            <span class="method-val">{{ formatCurrency(dailyMetrics.cash) }}</span>
          </div>
        </div>
      </div>

      <!-- Performance by Barber (Exclusivo Master) -->
      <div v-if="isMaster" class="team-perf-card glass-card">
        <div class="card-header-flex">
          <div>
            <h3 class="card-title">Desempenho dos Barbeiros no Dia</h3>
            <p class="card-desc">Produção individual e rateio de comissões calculados automaticamente.</p>
          </div>
          <router-link to="/appointments" class="btn btn-secondary btn-sm">Ver Todos os Agendamentos</router-link>
        </div>

        <div class="table-responsive">
          <table class="custom-table">
            <thead>
              <tr>
                <th>Barbeiro</th>
                <th>Cortes/Serviços</th>
                <th>Total Bruto</th>
                <th>Comissão Repassada</th>
                <th>Líquido Barbearia</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="barber in barbersPerformance" :key="barber.id">
                <td class="barber-cell">
                  <img :src="barber.avatar_url" class="avatar-sm" />
                  <div>
                    <span class="barber-name">{{ barber.name }}</span>
                    <span class="barber-nick">{{ barber.nickname }}</span>
                  </div>
                </td>
                <td><span class="count-badge">{{ barber.count }} atendimentos</span></td>
                <td class="bold-text">{{ formatCurrency(barber.gross) }}</td>
                <td class="emerald-text">{{ formatCurrency(barber.commission) }}</td>
                <td class="gold-text">{{ formatCurrency(barber.shopNet) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- MONTHLY HISTORICAL VIEW (MASTER ONLY) -->
    <div v-else-if="activeTab === 'monthly' && isMaster" class="tab-content">
      <div class="glass-card monthly-card">
        <div class="card-header-flex">
          <div>
            <h3 class="card-title">Histórico Mensal Consolidado</h3>
            <p class="card-desc">Acompanhe a evolução de receita mês a mês para planejar o crescimento da barbearia.</p>
          </div>
          <select v-model="selectedYear" class="form-select form-select-sm year-select">
            <option :value="2026">Ano 2026</option>
            <option :value="2025">Ano 2025</option>
          </select>
        </div>

        <div class="monthly-grid">
          <div v-for="m in monthlyHistory" :key="m.monthName" class="month-box glass-card-hover">
            <div class="month-head">
              <span class="month-title">{{ m.monthName }}</span>
              <span class="month-count">{{ m.totalOrders }} atendimentos</span>
            </div>
            <div class="month-value">{{ formatCurrency(m.grossRevenue) }}</div>
            <div class="month-breakdown">
              <span>Comissões: {{ formatCurrency(m.totalCommissions) }}</span>
              <span class="emerald-text">Líquido: {{ formatCurrency(m.shopNet) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { state, isMaster, formatCurrency } from '../store/state.js';

const activeTab = ref('daily');
const selectedDate = ref(new Date().toISOString().split('T')[0]);
const selectedYear = ref(2026);

// Filtered appointments based on selected date & current user role
const filteredAppointments = computed(() => {
  return state.appointments.filter(a => {
    const matchDate = a.scheduled_date === selectedDate.value;
    if (!matchDate) return false;
    if (isMaster.value) return true;
    return a.barber_id === state.currentUser.id;
  });
});

const dailyMetrics = computed(() => {
  let totalGross = 0;
  let barberCommission = 0;
  let shopNet = 0;
  let pix = 0;
  let creditCard = 0;
  let debitCard = 0;
  let cash = 0;
  let completedCount = 0;
  let pendingCount = 0;

  filteredAppointments.value.forEach(app => {
    if (app.status === 'COMPLETED') {
      completedCount++;
      const price = app.price_cents || 0;
      totalGross += price;

      // Find barber for commission rate
      const barber = state.users.find(u => u.id === app.barber_id);
      const rate = (barber?.commission_percentage || 50) / 100;
      const commission = Math.round(price * rate);
      barberCommission += commission;
      shopNet += (price - commission);

      if (app.payment_method === 'PIX') pix += price;
      else if (app.payment_method === 'CARTAO_CREDITO') creditCard += price;
      else if (app.payment_method === 'CARTAO_DEBITO') debitCard += price;
      else if (app.payment_method === 'DINHEIRO') cash += price;
      else pix += price; // default fallback
    } else if (app.status !== 'CANCELLED') {
      pendingCount++;
    }
  });

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
    pendingCount,
    averageTicket
  };
});

// Barbers breakdown for Master
const barbersPerformance = computed(() => {
  return state.users.filter(u => u.is_active).map(barber => {
    const barberApps = filteredAppointments.value.filter(
      a => a.barber_id === barber.id && a.status === 'COMPLETED'
    );
    const count = barberApps.length;
    const gross = barberApps.reduce((acc, a) => acc + (a.price_cents || 0), 0);
    const commission = Math.round(gross * (barber.commission_percentage / 100));
    const shopNet = gross - commission;

    return {
      ...barber,
      count,
      gross,
      commission,
      shopNet
    };
  });
});

// Monthly mock/aggregate history for demonstration
const monthlyHistory = computed(() => {
  const months = [
    { monthName: 'Janeiro', totalOrders: 142, grossRevenue: 685000, totalCommissions: 342500, shopNet: 342500 },
    { monthName: 'Fevereiro', totalOrders: 158, grossRevenue: 760000, totalCommissions: 380000, shopNet: 380000 },
    { monthName: 'Março', totalOrders: 180, grossRevenue: 890000, totalCommissions: 445000, shopNet: 445000 },
    { monthName: 'Abril', totalOrders: 195, grossRevenue: 980000, totalCommissions: 490000, shopNet: 490000 },
    { monthName: 'Maio', totalOrders: 210, grossRevenue: 1050000, totalCommissions: 525000, shopNet: 525000 },
    { monthName: 'Junho', totalOrders: 230, grossRevenue: 1180000, totalCommissions: 590000, shopNet: 590000 },
    { monthName: 'Julho', totalOrders: 245, grossRevenue: 1250000, totalCommissions: 625000, shopNet: 625000 },
    { monthName: 'Agosto', totalOrders: 260, grossRevenue: 1320000, totalCommissions: 660000, shopNet: 660000 },
    { monthName: 'Setembro', totalOrders: 275, grossRevenue: 1410000, totalCommissions: 705000, shopNet: 705000 },
    { monthName: 'Outubro (Atual)', totalOrders: 190, grossRevenue: 950000, totalCommissions: 475000, shopNet: 475000 }
  ];
  return months;
});
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
}

.user-greeting {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

.page-title {
  font-size: 1.75rem;
  color: var(--text-main);
}

.page-subtitle {
  color: var(--text-muted);
  font-size: 0.95rem;
  max-width: 600px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.view-toggle {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  padding: 0.25rem;
  border-radius: var(--radius-md);
  display: flex;
  gap: 0.25rem;
}

.toggle-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 0.45rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toggle-btn.active {
  background: var(--gold-500);
  color: #0b0f19;
}

.form-input-sm {
  padding: 0.45rem 0.75rem;
  font-size: 0.85rem;
}

/* KPI Grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 1.25rem;
}

.stat-card {
  padding: 1.5rem;
  display: flex;
  gap: 1.25rem;
  align-items: center;
}

.border-gold { border-left: 4px solid var(--gold-500); }
.border-emerald { border-left: 4px solid var(--emerald-500); }
.border-blue { border-left: 4px solid var(--blue-500); }
.border-rose { border-left: 4px solid var(--rose-500); }

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.gold-glow { background: var(--gold-glow); }
.emerald-glow { background: var(--emerald-glow); }
.blue-glow { background: var(--blue-glow); }
.rose-glow { background: var(--rose-glow); }

.stat-label {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-muted);
  letter-spacing: 0.04em;
}

.stat-value {
  font-size: 1.65rem;
  font-weight: 800;
  margin: 0.2rem 0;
}

.gold-text { color: #fbbf24; }
.emerald-text { color: #34d399; }
.blue-text { color: #60a5fa; }
.rose-text { color: #fb7185; }

.stat-sub {
  font-size: 0.8rem;
  color: var(--text-dim);
}

/* Payment Methods Breakdown */
.payment-methods-card {
  padding: 1.5rem;
}

.card-title {
  font-size: 1.15rem;
  margin-bottom: 1rem;
}

.card-desc {
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-top: 0.2rem;
}

.methods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.method-box {
  background: var(--bg-dark);
  border: 1px solid var(--border-color);
  padding: 1rem;
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.method-tag {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.tag-pix { color: #34d399; }
.tag-card { color: #60a5fa; }
.tag-debit { color: #a78bfa; }
.tag-cash { color: #fbbf24; }

.method-val {
  font-size: 1.25rem;
  font-weight: 700;
  font-family: var(--font-display);
}

/* Team Performance */
.team-perf-card {
  padding: 1.5rem;
  margin-top: 1.5rem;
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.table-responsive {
  overflow-x: auto;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.custom-table th {
  padding: 0.85rem 1rem;
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-dim);
  border-bottom: 1px solid var(--border-color);
}

.custom-table td {
  padding: 1rem;
  border-bottom: 1px solid var(--border-color);
  font-size: 0.925rem;
}

.barber-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.avatar-sm {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 2px solid var(--gold-500);
}

.barber-name {
  display: block;
  font-weight: 600;
}

.barber-nick {
  font-size: 0.8rem;
  color: var(--text-dim);
}

.count-badge {
  background: rgba(255, 255, 255, 0.06);
  padding: 0.25rem 0.6rem;
  border-radius: var(--radius-full);
  font-size: 0.8rem;
  font-weight: 600;
}

/* Monthly Grid */
.monthly-card {
  padding: 1.5rem;
}

.monthly-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
  margin-top: 1.25rem;
}

.month-box {
  background: var(--bg-dark);
  border: 1px solid var(--border-color);
  padding: 1.25rem;
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.month-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.month-title {
  font-weight: 700;
  font-size: 1.05rem;
}

.month-count {
  font-size: 0.75rem;
  color: var(--text-dim);
}

.month-value {
  font-size: 1.4rem;
  font-weight: 800;
  color: #fbbf24;
  font-family: var(--font-display);
}

.month-breakdown {
  display: flex;
  flex-direction: column;
  font-size: 0.785rem;
  color: var(--text-muted);
  gap: 0.2rem;
  border-top: 1px solid var(--border-color);
  padding-top: 0.5rem;
  margin-top: 0.25rem;
}
</style>
