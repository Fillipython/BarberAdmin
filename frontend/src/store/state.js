import { reactive, computed } from 'vue';

const STORAGE_KEY = 'barber_admin_data_v4';

const today = new Date().toISOString().split('T')[0];

const INITIAL_DATA = {
  currentUser: {
    id: '00000000-0000-0000-0000-000000000001',
    name: 'Alexandre Duarte',
    nickname: 'Alexandre',
    email: 'master@barberadmin.com',
    role: 'MASTER',
    commission_percentage: 100,
    avatar_url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=250'
  },
  users: [
    {
      id: '00000000-0000-0000-0000-000000000001',
      name: 'Alexandre Duarte',
      nickname: 'Alexandre',
      email: 'master@barberadmin.com',
      phone: '11988880001',
      role: 'MASTER',
      commission_percentage: 100,
      avatar_url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=250',
      is_active: true
    },
    {
      id: '00000000-0000-0000-0000-000000000002',
      name: 'Lucas Santana',
      nickname: 'Lucas',
      email: 'lucas@barberadmin.com',
      phone: '11988880002',
      role: 'BARBER',
      commission_percentage: 50,
      avatar_url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=250',
      is_active: true
    },
    {
      id: '00000000-0000-0000-0000-000000000003',
      name: 'Matheus Oliveira',
      nickname: 'Matheus',
      email: 'matheus@barberadmin.com',
      phone: '11988880003',
      role: 'BARBER',
      commission_percentage: 50,
      avatar_url: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=250',
      is_active: true
    },
    {
      id: '00000000-0000-0000-0000-000000000004',
      name: 'Rodrigo Lima',
      nickname: 'Rodrigo',
      email: 'rodrigo.barber@barberadmin.com',
      phone: '11988880004',
      role: 'BARBER',
      commission_percentage: 50,
      avatar_url: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&q=80&w=250',
      is_active: true
    },
    {
      id: '00000000-0000-0000-0000-000000000005',
      name: 'Bruno Rocha',
      nickname: 'Bruno',
      email: 'bruno@barberadmin.com',
      phone: '11988880005',
      role: 'BARBER',
      commission_percentage: 50,
      avatar_url: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&q=80&w=250',
      is_active: true
    }
  ],
  services: [
    { id: '1', name: 'Corte Tradicional', category: 'Cabelo', price_cents: 3000, duration_minutes: 30, is_active: true },
    { id: '2', name: 'Corte Kids', category: 'Cabelo', price_cents: 3000, duration_minutes: 30, is_active: true },
    { id: '3', name: 'Barba Tradicional', category: 'Barba', price_cents: 2500, duration_minutes: 30, is_active: true },
    { id: '4', name: 'Corte & Barba', category: 'Combo', price_cents: 5000, duration_minutes: 60, is_active: true },
    { id: '5', name: 'Corte & Sobrancelha', category: 'Combo', price_cents: 3500, duration_minutes: 30, is_active: true },
    { id: '6', name: 'Corte, Barba & Sobrancelha', category: 'Combo', price_cents: 5500, duration_minutes: 60, is_active: true },
    { id: '7', name: 'Pezinho & Barba', category: 'Combo', price_cents: 3000, duration_minutes: 30, is_active: true },
    { id: '8', name: 'Corte & Barba Simples', category: 'Combo', price_cents: 4000, duration_minutes: 30, is_active: true },
    { id: '9', name: 'Corte, Sobrancelha & Barba Simples', category: 'Combo', price_cents: 4500, duration_minutes: 30, is_active: true },
    { id: '10', name: 'Platinado & Corte', category: 'Química', price_cents: 13000, duration_minutes: 60, is_active: true },
    { id: '11', name: 'Luzes & Corte', category: 'Química', price_cents: 10000, duration_minutes: 60, is_active: true },
    { id: '12', name: 'Botox / Desondulação', category: 'Química', price_cents: 5000, duration_minutes: 60, is_active: true },
    { id: '13', name: 'Selagem Capilar', category: 'Química', price_cents: 7500, duration_minutes: 60, is_active: true },
    { id: '14', name: 'Hidratação Profunda', category: 'Tratamento', price_cents: 2000, duration_minutes: 15, is_active: true },
    { id: '15', name: 'Alisamento', category: 'Química', price_cents: 2000, duration_minutes: 15, is_active: true },
    { id: '16', name: 'Pigmentação de Barba/Cabelo', category: 'Acabamento', price_cents: 2000, duration_minutes: 30, is_active: true }
  ],
  clients: [
    { id: 'c1', name: 'Gabriel Santos', phone: '11977771001', email: 'gabriel@email.com', notes: 'Gosta do degradê navalhado.', total_visits: 8, total_spent_cents: 40000 },
    { id: 'c2', name: 'Rodrigo Albuquerque', phone: '11977771002', email: 'rodrigo@email.com', notes: 'Toalha quente na barba.', total_visits: 6, total_spent_cents: 30000 },
    { id: 'c3', name: 'Rafael Moreira', phone: '11977771003', email: 'rafael@email.com', notes: 'Platinado e corte a cada 2 meses.', total_visits: 5, total_spent_cents: 55000 },
    { id: 'c4', name: 'Bruno Mendes', phone: '11977771004', email: 'bruno.m@email.com', notes: 'Corte tradicional na tesoura.', total_visits: 12, total_spent_cents: 36000 },
    { id: 'c5', name: 'Thiago Costa', phone: '11977771005', email: 'thiago@email.com', notes: 'Sobrancelha alinhada na navalha.', total_visits: 4, total_spent_cents: 18000 },
    { id: 'c6', name: 'Vinicius Pereira', phone: '11977771006', email: 'vinicius@email.com', notes: 'Pigmentação de barba.', total_visits: 7, total_spent_cents: 35000 },
    { id: 'c7', name: 'Eduardo Ramos', phone: '11977771007', email: 'eduardo@email.com', notes: 'Corte kids do filho também.', total_visits: 9, total_spent_cents: 45000 },
    { id: 'c8', name: 'Gustavo Henrique', phone: '11977771008', email: 'gustavo@email.com', notes: 'Luzes e degrade.', total_visits: 3, total_spent_cents: 30000 },
    { id: 'c9', name: 'Leonardo Souza', phone: '11977771009', email: 'leonardo@email.com', notes: 'Barboterapia completa.', total_visits: 5, total_spent_cents: 25000 },
    { id: 'c10', name: 'Marcelo Antunes', phone: '11977771010', email: 'marcelo@email.com', notes: 'Cliente corporativo pontual.', total_visits: 11, total_spent_cents: 55000 }
  ],
  appointments: [
    {
      id: 'a1',
      client_name: 'Rodrigo Albuquerque',
      client_phone: '11977771002',
      barber_id: '00000000-0000-0000-0000-000000000001',
      barber_name: 'Alexandre Duarte',
      service_id: '4',
      service_name: 'Corte & Barba',
      price_cents: 5000,
      scheduled_date: today,
      scheduled_time: '09:00',
      status: 'COMPLETED',
      payment_method: 'PIX'
    },
    {
      id: 'a2',
      client_name: 'Gabriel Santos',
      client_phone: '11977771001',
      barber_id: '00000000-0000-0000-0000-000000000002',
      barber_name: 'Lucas Santana',
      service_id: '1',
      service_name: 'Corte Tradicional',
      price_cents: 3000,
      scheduled_date: today,
      scheduled_time: '09:30',
      status: 'COMPLETED',
      payment_method: 'CARTAO_CREDITO'
    },
    {
      id: 'a3',
      client_name: 'Rafael Moreira',
      client_phone: '11977771003',
      barber_id: '00000000-0000-0000-0000-000000000003',
      barber_name: 'Matheus Oliveira',
      service_id: '10',
      service_name: 'Platinado & Corte',
      price_cents: 13000,
      scheduled_date: today,
      scheduled_time: '10:00',
      status: 'COMPLETED',
      payment_method: 'PIX'
    },
    {
      id: 'a4',
      client_name: 'Bruno Mendes',
      client_phone: '11977771004',
      barber_id: '00000000-0000-0000-0000-000000000004',
      barber_name: 'Rodrigo Lima',
      service_id: '6',
      service_name: 'Corte, Barba & Sobrancelha',
      price_cents: 5500,
      scheduled_date: today,
      scheduled_time: '11:00',
      status: 'COMPLETED',
      payment_method: 'DINHEIRO'
    },
    {
      id: 'a5',
      client_name: 'Thiago Costa',
      client_phone: '11977771005',
      barber_id: '00000000-0000-0000-0000-000000000005',
      barber_name: 'Bruno Rocha',
      service_id: '5',
      service_name: 'Corte & Sobrancelha',
      price_cents: 3500,
      scheduled_date: today,
      scheduled_time: '11:30',
      status: 'COMPLETED',
      payment_method: 'CARTAO_DEBITO'
    },
    {
      id: 'a6',
      client_name: 'Vinicius Pereira',
      client_phone: '11977771006',
      barber_id: '00000000-0000-0000-0000-000000000001',
      barber_name: 'Alexandre Duarte',
      service_id: '16',
      service_name: 'Pigmentação de Barba/Cabelo',
      price_cents: 2000,
      scheduled_date: today,
      scheduled_time: '14:00',
      status: 'CONFIRMED',
      payment_method: null
    },
    {
      id: 'a7',
      client_name: 'Eduardo Ramos',
      client_phone: '11977771007',
      barber_id: '00000000-0000-0000-0000-000000000002',
      barber_name: 'Lucas Santana',
      service_id: '4',
      service_name: 'Corte & Barba',
      price_cents: 5000,
      scheduled_date: today,
      scheduled_time: '15:00',
      status: 'CONFIRMED',
      payment_method: null
    },
    {
      id: 'a8',
      client_name: 'Gustavo Henrique',
      client_phone: '11977771008',
      barber_id: '00000000-0000-0000-0000-000000000003',
      barber_name: 'Matheus Oliveira',
      service_id: '11',
      service_name: 'Luzes & Corte',
      price_cents: 10000,
      scheduled_date: today,
      scheduled_time: '16:00',
      status: 'CONFIRMED',
      payment_method: null
    }
  ]
};

function loadSavedState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    console.error('Failed to load state from localStorage', e);
  }
  return INITIAL_DATA;
}

export const state = reactive(loadSavedState());

export function saveState() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch (e) {
    console.error('Failed to persist state', e);
  }
}

// Helpers & Actions
export const isMaster = computed(() => state.currentUser.role === 'MASTER');

export function switchUser(userId) {
  const user = state.users.find(u => u.id === userId);
  if (user) {
    state.currentUser = { ...user };
    saveState();
  }
}

export function addService(service) {
  const newService = {
    id: String(Date.now()),
    is_active: true,
    ...service
  };
  state.services.unshift(newService);
  saveState();
  return newService;
}

export function updateService(id, updates) {
  const index = state.services.findIndex(s => s.id === id);
  if (index !== -1) {
    state.services[index] = { ...state.services[index], ...updates };
    saveState();
  }
}

export function toggleServiceStatus(id) {
  const service = state.services.find(s => s.id === id);
  if (service) {
    service.is_active = !service.is_active;
    saveState();
  }
}

export function addBarber(barber) {
  const newBarber = {
    id: `barber-${Date.now()}`,
    role: 'BARBER',
    commission_percentage: Number(barber.commission_percentage) || 50,
    avatar_url: `https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=250`,
    is_active: true,
    ...barber
  };
  state.users.push(newBarber);
  saveState();
  return newBarber;
}

export function removeBarber(id) {
  const index = state.users.findIndex(u => u.id === id);
  if (index !== -1 && state.users[index].role !== 'MASTER') {
    state.users.splice(index, 1);
    saveState();
  }
}

export function addClient(client) {
  const newClient = {
    id: `client-${Date.now()}`,
    total_visits: 0,
    total_spent_cents: 0,
    ...client
  };
  state.clients.unshift(newClient);
  saveState();
  return newClient;
}

export function addAppointment(appointment) {
  const newApp = {
    id: `app-${Date.now()}`,
    status: 'CONFIRMED',
    ...appointment
  };
  state.appointments.unshift(newApp);
  saveState();
  return newApp;
}

export function updateAppointmentStatus(id, newStatus, paymentMethod = 'PIX') {
  const app = state.appointments.find(a => a.id === id);
  if (app) {
    app.status = newStatus;
    if (newStatus === 'COMPLETED') {
      app.payment_method = paymentMethod;
    }
    saveState();
  }
}

export function updateAppointmentPrice(id, newPriceCents) {
  const app = state.appointments.find(a => a.id === id);
  if (app) {
    app.price_cents = newPriceCents;
    saveState();
  }
}

export function updateAppointmentService(id, serviceId, serviceName, priceCents) {
  const app = state.appointments.find(a => a.id === id);
  if (app) {
    app.service_id = serviceId;
    app.service_name = serviceName;
    app.price_cents = priceCents;
    saveState();
  }
}

// Formatters
export function formatCurrency(cents) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format((cents || 0) / 100);
}
