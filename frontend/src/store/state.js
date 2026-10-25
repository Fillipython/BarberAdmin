import { reactive, computed } from 'vue';

const STORAGE_KEY = 'barber_admin_data_v1';

const INITIAL_DATA = {
  currentUser: {
    id: '00000000-0000-0000-0000-000000000001',
    name: 'Carlos Silva',
    nickname: 'Mestre Carlos',
    email: 'master@barberadmin.com',
    role: 'MASTER',
    commission_percentage: 100,
    avatar_url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=250'
  },
  users: [
    {
      id: '00000000-0000-0000-0000-000000000001',
      name: 'Carlos Silva',
      nickname: 'Mestre Carlos',
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
      nickname: 'Lucas Navalha',
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
      nickname: 'Matheus Fade',
      email: 'matheus@barberadmin.com',
      phone: '11988880003',
      role: 'BARBER',
      commission_percentage: 50,
      avatar_url: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=250',
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
    {
      id: 'c1',
      name: 'Rodrigo Albuquerque',
      phone: '11977771001',
      email: 'rodrigo@email.com',
      notes: 'Gosta do degradê bem baixo navalhado.',
      total_visits: 6,
      total_spent_cents: 30000
    },
    {
      id: 'c2',
      name: 'Gabriel Santos',
      phone: '11977771002',
      email: 'gabriel@email.com',
      notes: 'Prefere toalha bem quente na barba.',
      total_visits: 3,
      total_spent_cents: 15000
    },
    {
      id: 'c3',
      name: 'Felipe Almeida',
      phone: '11977771003',
      email: 'felipe@email.com',
      notes: 'Cliente de platinado pontual.',
      total_visits: 4,
      total_spent_cents: 35000
    }
  ],
  appointments: [
    {
      id: 'a1',
      client_name: 'Rodrigo Albuquerque',
      client_phone: '11977771001',
      barber_id: '00000000-0000-0000-0000-000000000001',
      barber_name: 'Carlos Silva',
      service_id: '4',
      service_name: 'Corte & Barba',
      price_cents: 5000,
      scheduled_date: new Date().toISOString().split('T')[0],
      scheduled_time: '09:00',
      status: 'COMPLETED',
      payment_method: 'PIX'
    },
    {
      id: 'a2',
      client_name: 'Gabriel Santos',
      client_phone: '11977771002',
      barber_id: '00000000-0000-0000-0000-000000000002',
      barber_name: 'Lucas Santana',
      service_id: '1',
      service_name: 'Corte Tradicional',
      price_cents: 3000,
      scheduled_date: new Date().toISOString().split('T')[0],
      scheduled_time: '10:30',
      status: 'COMPLETED',
      payment_method: 'CARTAO_CREDITO'
    },
    {
      id: 'a3',
      client_name: 'Felipe Almeida',
      client_phone: '11977771003',
      barber_id: '00000000-0000-0000-0000-000000000003',
      barber_name: 'Matheus Oliveira',
      service_id: '10',
      service_name: 'Platinado & Corte',
      price_cents: 13000,
      scheduled_date: new Date().toISOString().split('T')[0],
      scheduled_time: '14:00',
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

// Formatters
export function formatCurrency(cents) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format((cents || 0) / 100);
}
