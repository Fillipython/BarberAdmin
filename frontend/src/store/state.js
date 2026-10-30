import { reactive, computed } from 'vue';
import { api } from '../services/api.js';

const STORAGE_KEY = 'barber_admin_data_v9';

export function toLocalIsoDate(d = new Date()) {
  const yr = d.getFullYear();
  const mo = String(d.getMonth() + 1).padStart(2, '0');
  const da = String(d.getDate()).padStart(2, '0');
  return `${yr}-${mo}-${da}`;
}

const today = toLocalIsoDate(new Date());

export const DEFAULT_BARBER_PHOTOS = {
  '8a350074-5524-453d-a625-2b25e7e8ca59': 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=250',
  '70b1fee3-a338-4444-90f9-f4551afc15fe': 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=250',
  '0670b734-45c1-4435-ad0e-b8d6b3099c7e': 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&q=80&w=250'
};

export function buildInitialsAvatar(name) {
  const parts = String(name || 'BM').trim().split(/\s+/);
  const initials = parts.length > 1
    ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
    : parts[0].substring(0, 2).toUpperCase();
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120"><rect width="120" height="120" rx="60" fill="#18202f"/><circle cx="60" cy="60" r="56" fill="none" stroke="#d4af37" stroke-width="3"/><text x="50%" y="54%" dominant-baseline="middle" text-anchor="middle" fill="#fbbf24" font-family="Inter, sans-serif" font-weight="700" font-size="42">${initials}</text></svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

const INITIAL_DATA = {
  currentUser: {
    id: '8a350074-5524-453d-a625-2b25e7e8ca59',
    name: 'Carlos Silva',
    nickname: 'Mestre Carlos',
    email: 'contato@barberman.com.br',
    phone: '11988880001',
    role: 'MASTER',
    commission_percentage: 100,
    avatar_url: DEFAULT_BARBER_PHOTOS['8a350074-5524-453d-a625-2b25e7e8ca59']
  },
  users: [
    {
      id: '8a350074-5524-453d-a625-2b25e7e8ca59',
      name: 'Carlos Silva',
      nickname: 'Mestre Carlos',
      email: 'contato@barberman.com.br',
      phone: '11988880001',
      role: 'MASTER',
      commission_percentage: 100,
      avatar_url: DEFAULT_BARBER_PHOTOS['8a350074-5524-453d-a625-2b25e7e8ca59'],
      bio: 'Fundador e Administrador Geral do Barber Man. Especialista em visagismo e cortes premium.',
      is_active: true
    },
    {
      id: '70b1fee3-a338-4444-90f9-f4551afc15fe',
      name: 'Lucas Santana',
      nickname: 'Lucas Navalha',
      email: 'lucas@barberman.com.br',
      phone: '11988880002',
      role: 'BARBER',
      commission_percentage: 50,
      avatar_url: DEFAULT_BARBER_PHOTOS['70b1fee3-a338-4444-90f9-f4551afc15fe'],
      bio: 'Especialista em degrade navalhado e barba terapia.',
      is_active: true
    },
    {
      id: '0670b734-45c1-4435-ad0e-b8d6b3099c7e',
      name: 'Matheus Oliveira',
      nickname: 'Matheus Fade',
      email: 'matheus@barberman.com.br',
      phone: '11988880003',
      role: 'BARBER',
      commission_percentage: 50,
      avatar_url: DEFAULT_BARBER_PHOTOS['0670b734-45c1-4435-ad0e-b8d6b3099c7e'],
      bio: 'Especialista em platinados, luzes e cortes modernos.',
      is_active: true
    }
  ],
  services: [
    { id: '16b14dc5-1df2-4041-a26b-4b292ca2db35', name: 'Botox/desondulacao', category: 'Química', price_cents: 5000, duration_minutes: 60, is_active: true },
    { id: 'a7cb2ec3-f68f-42ff-859a-58666d3b727c', name: 'Selagem', category: 'Química', price_cents: 7500, duration_minutes: 60, is_active: true },
    { id: 'b5eac08f-4c62-40be-b974-c00881aa99e0', name: 'Hidratacao', category: 'Tratamento', price_cents: 2000, duration_minutes: 15, is_active: true },
    { id: '650e3202-2f1f-435f-91b9-442e47912118', name: 'Alisamento', category: 'Química', price_cents: 2000, duration_minutes: 15, is_active: true },
    { id: '187ebf61-e48d-4665-a71e-8076768b3915', name: 'Pigmentacao', category: 'Acabamento', price_cents: 2000, duration_minutes: 30, is_active: true },
    { id: '5379777b-3c95-42b4-9baa-06b621a325b8', name: 'Corte', category: 'Cabelo', price_cents: 3000, duration_minutes: 30, is_active: true },
    { id: '1332638b-4d7f-467f-ba2e-c0f6614ee0c6', name: 'Corte kids', category: 'Cabelo', price_cents: 3000, duration_minutes: 30, is_active: true },
    { id: 'bd3ac8a7-5955-4a97-a275-c621f635e635', name: 'Corte & Sobrancelha', category: 'Combo', price_cents: 3500, duration_minutes: 30, is_active: true },
    { id: 'b629d3af-6e6c-4c2f-930c-becd3c78bb58', name: 'Platinado & Corte', category: 'Química', price_cents: 13000, duration_minutes: 60, is_active: true },
    { id: '497594fe-cdc2-4ae5-a928-523ffd3fd522', name: 'Luzes & Corte', category: 'Química', price_cents: 10000, duration_minutes: 60, is_active: true },
    { id: '85f353f0-229b-4586-9850-dc2b65434a63', name: 'Corte & Barba', category: 'Combo', price_cents: 5000, duration_minutes: 60, is_active: true },
    { id: 'e3ce8dc4-44f0-4d62-95d1-8651ea77d353', name: 'Barba', category: 'Barba', price_cents: 2500, duration_minutes: 30, is_active: true },
    { id: '80af0829-4a7f-4745-a945-c7c8fa2dd00d', name: 'Corte, Barba & Sobrancelha', category: 'Combo', price_cents: 5500, duration_minutes: 60, is_active: true },
    { id: '9095beb0-8eb7-447c-aa9f-e52e03e65c36', name: 'Corte & barba simples', category: 'Combo', price_cents: 4000, duration_minutes: 30, is_active: true },
    { id: '3dcf6649-b31e-4fad-b493-4d06c86b5ff1', name: 'Corte, sobrancelha & barba simples', category: 'Combo', price_cents: 4500, duration_minutes: 30, is_active: true },
    { id: '3d0ecd31-320a-4f35-bd57-071a3bb66ec8', name: 'Pezinho & Barba', category: 'Combo', price_cents: 3000, duration_minutes: 30, is_active: true }
  ],
  clients: [
    { id: 'c1000000-0000-0000-0000-000000000001', name: 'Gabriel Santos', phone: '11977771001', email: 'gabriel.santos@exemplo.com', notes: 'Prefere degradê navalhado.', total_visits: 4, total_spent_cents: 20000 },
    { id: 'c1000000-0000-0000-0000-000000000002', name: 'Rodrigo Albuquerque', phone: '11977771002', email: 'rodrigo.albuquerque@exemplo.com', notes: 'Toalha quente na barba.', total_visits: 6, total_spent_cents: 30000 },
    { id: 'c1000000-0000-0000-0000-000000000003', name: 'Rafael Moreira', phone: '11977771003', email: 'rafael.moreira@exemplo.com', notes: 'Platinado e corte.', total_visits: 3, total_spent_cents: 39000 },
    { id: 'c1000000-0000-0000-0000-000000000004', name: 'Bruno Mendes', phone: '11977771004', email: 'bruno.mendes@exemplo.com', notes: 'Corte tradicional tesoura.', total_visits: 8, total_spent_cents: 24000 },
    { id: 'c1000000-0000-0000-0000-000000000005', name: 'Thiago Costa', phone: '11977771005', email: 'thiago.costa@exemplo.com', notes: 'Sobrancelha alinhada.', total_visits: 5, total_spent_cents: 17500 },
    { id: 'c1000000-0000-0000-0000-000000000006', name: 'Vinicius Pereira', phone: '11977771006', email: 'vinicius.pereira@exemplo.com', notes: 'Pigmentação de barba.', total_visits: 2, total_spent_cents: 10000 },
    { id: 'c1000000-0000-0000-0000-000000000007', name: 'Eduardo Ramos', phone: '11977771007', email: 'eduardo.ramos@exemplo.com', notes: 'Corte infantil do filho.', total_visits: 7, total_spent_cents: 21000 },
    { id: 'c1000000-0000-0000-0000-000000000008', name: 'Gustavo Henrique', phone: '11977771008', email: 'gustavo.henrique@exemplo.com', notes: 'Luzes e degrade.', total_visits: 3, total_spent_cents: 30000 },
    { id: 'c1000000-0000-0000-0000-000000000009', name: 'Leonardo Souza', phone: '11977771009', email: 'leonardo.souza@exemplo.com', notes: 'Barboterapia completa.', total_visits: 5, total_spent_cents: 25000 },
    { id: 'c1000000-0000-0000-0000-000000000010', name: 'Marcelo Antunes', phone: '11977771010', email: 'marcelo.antunes@exemplo.com', notes: 'Cliente corporativo pontual.', total_visits: 9, total_spent_cents: 45000 }
  ],
  appointments: [
    {
      id: 'a1000000-0000-0000-0000-000000000000',
      client_name: 'Bruno Mendes',
      client_phone: '11977771004',
      barber_id: '8a350074-5524-453d-a625-2b25e7e8ca59',
      barber_name: 'Carlos Silva',
      service_id: '5379777b-3c95-42b4-9baa-06b621a325b8',
      service_name: 'Corte',
      price_cents: 3000,
      scheduled_date: today,
      scheduled_time: '09:30',
      status: 'COMPLETED',
      payment_method: 'PIX'
    },
    {
      id: 'a1000000-0000-0000-0000-000000000001',
      client_name: 'Gabriel Santos',
      client_phone: '11977771001',
      barber_id: '8a350074-5524-453d-a625-2b25e7e8ca59',
      barber_name: 'Carlos Silva',
      service_id: 'b629d3af-6e6c-4c2f-930c-becd3c78bb58',
      service_name: 'Platinado & Corte',
      price_cents: 13000,
      scheduled_date: today,
      scheduled_time: '11:00',
      status: 'CONFIRMED',
      payment_method: null
    },
    {
      id: 'a1000000-0000-0000-0000-000000000002',
      client_name: 'Rodrigo Albuquerque',
      client_phone: '11977771002',
      barber_id: '70b1fee3-a338-4444-90f9-f4551afc15fe',
      barber_name: 'Lucas Santana',
      service_id: '85f353f0-229b-4586-9850-dc2b65434a63',
      service_name: 'Corte & Barba',
      price_cents: 5000,
      scheduled_date: today,
      scheduled_time: '14:00',
      status: 'CONFIRMED',
      payment_method: null
    },
    {
      id: 'a1000000-0000-0000-0000-000000000003',
      client_name: 'Rafael Moreira',
      client_phone: '11977771003',
      barber_id: '0670b734-45c1-4435-ad0e-b8d6b3099c7e',
      barber_name: 'Matheus Oliveira',
      service_id: '497594fe-cdc2-4ae5-a928-523ffd3fd522',
      service_name: 'Luzes & Corte',
      price_cents: 10000,
      scheduled_date: today,
      scheduled_time: '16:00',
      status: 'CONFIRMED',
      payment_method: null
    }
  ]
};

const AUTH_TOKEN_KEY = 'barber_admin_auth_token_v1';
const AUTH_USER_KEY = 'barber_admin_auth_user_v1';

// Canal de sincronização em tempo real entre abas (BarberAdmin <-> BarberNotify)
const notifySyncChannel = typeof window !== 'undefined' && window.BroadcastChannel
  ? new BroadcastChannel('barber_notify_sync')
  : null;

if (notifySyncChannel) {
  notifySyncChannel.onmessage = (event) => {
    if (event.data === 'appointment_changed') {
      syncFromBackend();
    }
  };
}

export function normalizeService(s) {
  if (!s) return s;
  const price = s.price_cents ?? s.priceCents ?? 0;
  const duration = s.duration_minutes ?? s.durationMinutes ?? 30;
  const active = (s.is_active ?? s.isActive) !== false;
  return {
    ...s,
    price_cents: price,
    priceCents: price,
    duration_minutes: duration,
    durationMinutes: duration,
    is_active: active,
    isActive: active
  };
}

export function formatAvatarUrl(url, name = 'Barbeiro', barberId = null) {
  if (url === 'REMOVED' || url === 'NONE') {
    return buildInitialsAvatar(name);
  }
  if (!url || String(url).includes('1534528741775')) {
    if (barberId && DEFAULT_BARBER_PHOTOS[barberId]) {
      return DEFAULT_BARBER_PHOTOS[barberId];
    }
    return DEFAULT_BARBER_PHOTOS['8a350074-5524-453d-a625-2b25e7e8ca59'] || buildInitialsAvatar(name);
  }
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    return url;
  }
  let base = 'http://localhost:8080';
  if (typeof window !== 'undefined') {
    const host = window.location.hostname;
    const protocol = window.location.protocol;
    if (host.includes('fillipython.ddns.net') || window.location.pathname.startsWith('/BarberAdmin')) {
      base = `${protocol}//${window.location.host}/BarberAdmin`;
    }
  }
  if (url.startsWith('/api/')) {
    return `${base}${url}`;
  }
  return `${base}/api${url.startsWith('/') ? '' : '/'}${url}`;
}

export function normalizeUser(u) {
  if (!u) return u;
  const active = (u.is_active ?? u.isActive) !== false;
  const commission = Number(u.commission_percentage ?? u.commissionPercentage) || (u.role === 'MASTER' ? 100 : 50);
  const rawAvatar = u.avatar_url ?? u.avatarUrl;
  const avatar = formatAvatarUrl(rawAvatar, u.name, u.id);
  return {
    ...u,
    is_active: active,
    isActive: active,
    commission_percentage: commission,
    commissionPercentage: commission,
    avatar_url: avatar,
    avatarUrl: avatar,
    raw_avatar_url: rawAvatar,
    has_custom_photo: Boolean(rawAvatar && rawAvatar !== 'REMOVED' && rawAvatar !== 'NONE')
  };
}

export function normalizeClient(c) {
  if (!c) return c;
  const visits = c.total_visits ?? c.totalVisits ?? 0;
  const spent = c.total_spent_cents ?? c.totalSpentCents ?? 0;
  return {
    ...c,
    total_visits: visits,
    totalVisits: visits,
    total_spent_cents: spent,
    totalSpentCents: spent
  };
}

export function normalizeAppointment(a) {
  if (!a) return a;
  let schedDate = a.scheduled_date;
  let schedTime = a.scheduled_time || a.timeFormatted;
  if (!schedDate && a.scheduledAt) {
    const parsedDate = new Date(a.scheduledAt);
    schedDate = !isNaN(parsedDate.getTime())
      ? toLocalIsoDate(parsedDate)
      : String(a.scheduledAt).split('T')[0];
  }
  if (!schedTime && a.scheduledAt && String(a.scheduledAt).includes('T')) {
    schedTime = String(a.scheduledAt).split('T')[1].substring(0, 5);
  }
  const notesStr = a.notes || '';
  const isDayBlock = Boolean(
    notesStr.includes('AGENDA_FECHADA') ||
    a.client_name === 'Agenda Fechada' ||
    a.clientName === 'Agenda Fechada' ||
    a.is_day_block
  );
  return {
    id: a.id,
    client_id: a.client_id ?? a.clientId,
    client_name: a.client_name ?? a.clientName ?? 'Cliente',
    client_phone: a.client_phone ?? a.clientPhone ?? '',
    barber_id: a.barber_id ?? a.barberId,
    barber_name: a.barber_name ?? a.barberName ?? 'Barbeiro',
    service_id: a.service_id ?? a.serviceId,
    service_name: a.service_name ?? a.serviceName ?? 'Serviço',
    price_cents: a.price_cents ?? a.priceCents ?? 0,
    scheduled_date: schedDate || today,
    scheduled_time: schedTime || '09:00',
    status: a.status || 'CONFIRMED',
    payment_method: a.payment_method ?? a.paymentMethod ?? (a.status === 'COMPLETED' ? 'PIX' : null),
    notes: notesStr,
    is_day_block: isDayBlock
  };
}

function loadSavedState() {
  let loaded = { ...INITIAL_DATA };
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      loaded = { ...INITIAL_DATA, ...JSON.parse(raw) };
    }
  } catch (e) {
    console.error('Failed to load state from localStorage', e);
  }

  if (Array.isArray(loaded.services)) {
    loaded.services = loaded.services.map(normalizeService);
  }
  if (Array.isArray(loaded.users)) {
    loaded.users = loaded.users.map(normalizeUser);
  }
  if (Array.isArray(loaded.clients)) {
    loaded.clients = loaded.clients.map(normalizeClient);
  }
  if (Array.isArray(loaded.appointments)) {
    loaded.appointments = loaded.appointments.map(normalizeAppointment);
  }

  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  const savedUserRaw = localStorage.getItem(AUTH_USER_KEY);
  let savedUser = null;
  if (savedUserRaw) {
    try {
      const parsedUser = JSON.parse(savedUserRaw);
      if (parsedUser && String(parsedUser.avatar_url || parsedUser.avatarUrl || '').includes('1534528741775')) {
        parsedUser.avatar_url = DEFAULT_BARBER_PHOTOS[parsedUser.id] || DEFAULT_BARBER_PHOTOS['8a350074-5524-453d-a625-2b25e7e8ca59'];
        parsedUser.avatarUrl = parsedUser.avatar_url;
        parsedUser.raw_avatar_url = parsedUser.avatar_url;
      }
      savedUser = normalizeUser(parsedUser);
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(savedUser));
    } catch (e) {}
  }

  loaded.isAuthenticated = Boolean(token && savedUser);
  loaded.token = token || null;
  loaded.currentUser = savedUser || null;
  loaded.dayBlocks = loaded.dayBlocks || {};

  return loaded;
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
export const isMaster = computed(() => state.currentUser?.role === 'MASTER');

export function isUserAuthenticated() {
  return Boolean(state.isAuthenticated && state.currentUser);
}

export async function login(email, password) {
  const response = await api.auth.login({ email, password });
  state.isAuthenticated = true;
  state.token = response.token;
  state.currentUser = normalizeUser({
    id: response.id,
    name: response.name,
    nickname: response.nickname,
    email: response.email,
    phone: response.phone,
    role: response.role,
    commissionPercentage: response.commissionPercentage,
    avatarUrl: response.avatarUrl
  });

  localStorage.setItem(AUTH_TOKEN_KEY, response.token);
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(state.currentUser));
  saveState();

  await syncFromBackend();
  return response;
}

export function logout() {
  state.isAuthenticated = false;
  state.token = null;
  state.currentUser = null;
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
  saveState();
}

export function switchUser(userId) {
  const user = state.users.find(u => u.id === userId);
  if (user) {
    state.currentUser = { ...user };
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(state.currentUser));
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
  const newBarber = normalizeUser({
    id: `barber-${Date.now()}`,
    role: 'BARBER',
    commission_percentage: Number(barber.commission_percentage) || 50,
    avatar_url: null,
    is_active: true,
    ...barber
  });
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
  const newApp = normalizeAppointment({
    id: `app-${Date.now()}`,
    status: 'CONFIRMED',
    ...appointment
  });
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
      api.appointments.checkout(id, paymentMethod, app.price_cents).catch(() => {});
    } else {
      api.appointments.updateStatus(id, newStatus).catch(() => {});
      if (newStatus === 'CANCELLED') {
        notifyBarberNotifySlotCancel(app.barber_id, app.scheduled_date, app.scheduled_time);
      }
    }
    saveState();
    notifySyncChannel?.postMessage('appointment_changed');
  }
}

export function updateAppointmentPrice(id, newPriceCents) {
  const app = state.appointments.find(a => a.id === id);
  if (app) {
    app.price_cents = newPriceCents;
    api.appointments.overridePrice(id, newPriceCents).catch(() => {});
    saveState();
  }
}

export function updateAppointmentService(id, serviceId, serviceName, priceCents) {
  const app = state.appointments.find(a => a.id === id);
  if (app) {
    app.service_id = serviceId;
    app.service_name = serviceName;
    app.price_cents = priceCents;
    api.appointments.changeService(id, serviceId, priceCents).catch(() => {});
    saveState();
  }
}

function resolveBarberNotifyApiBase() {
  if (typeof window !== 'undefined') {
    const host = window.location.hostname;
    const protocol = window.location.protocol;
    if (host.includes('fillipython.ddns.net') || window.location.pathname.startsWith('/BarberAdmin') || (host !== 'localhost' && host !== '127.0.0.1')) {
      return `${protocol}//${window.location.host}/BarberNotify/api`;
    }
  }
  return 'http://localhost:8081/api';
}

async function notifyBarberNotifySlotCancel(barberId, dateStr, timeStr) {
  if (!barberId || !dateStr || !timeStr) return;
  try {
    const base = resolveBarberNotifyApiBase();
    await fetch(`${base}/appointments/by-slot?barberId=${barberId}&date=${dateStr}&time=${encodeURIComponent(timeStr)}`, {
      method: 'DELETE'
    });
  } catch (e) {}
}

export async function syncDateFromBackend(dateStr) {
  const targetDate = dateStr || toLocalIsoDate(new Date());
  try {
    const notifyBase = resolveBarberNotifyApiBase();
    const [appointmentsList, blockStatus, notifyBlockRes] = await Promise.all([
      api.appointments.list(targetDate, null, 'MASTER').catch(() => null),
      api.appointments.getDayBlockStatus(targetDate).catch(() => null),
      fetch(`${notifyBase}/appointments/day-block?date=${targetDate}`).then(r => r.ok ? r.json() : null).catch(() => null)
    ]);

    if (Array.isArray(appointmentsList)) {
      const normalizedFromApi = appointmentsList.map(normalizeAppointment);
      const otherDays = state.appointments.filter(a => a.scheduled_date !== targetDate);
      state.appointments = [...normalizedFromApi, ...otherDays];
    }

    const mergedClosedIds = new Set([
      ...(blockStatus?.closedBarberIds || []),
      ...(notifyBlockRes?.closedBarberIds || [])
    ]);
    const activeStaff = (state.users || []).filter(u => u.is_active !== false);
    const allClosed = activeStaff.length > 0 && activeStaff.every(u => mergedClosedIds.has(u.id));
    const shopClosedByMaster = Boolean(blockStatus?.shopClosedByMaster || notifyBlockRes?.shopClosedByMaster);

    state.dayBlocks = {
      ...state.dayBlocks,
      [targetDate]: {
        date: targetDate,
        shopClosed: Boolean(blockStatus?.shopClosed || notifyBlockRes?.shopClosed || allClosed || shopClosedByMaster),
        shopClosedByMaster,
        closedBarberIds: Array.from(mergedClosedIds)
      }
    };

    saveState();
  } catch (e) {
    console.warn('[BarberAdmin State] Falha ao sincronizar data:', targetDate, e.message);
  }
}

export async function closeDaySchedule({ date, barberId = null, closeShop = false }) {
  const caller = state.currentUser;
  const callerId = caller ? caller.id : barberId;
  const callerRole = caller ? caller.role : 'BARBER';
  const targetDate = date || toLocalIsoDate(new Date());
  const targetBarberId = closeShop ? null : (barberId || callerId);

  const notifyBase = resolveBarberNotifyApiBase();
  await Promise.all([
    api.appointments.closeDay({
      date: targetDate,
      barberId: targetBarberId,
      closeShop,
      callerId,
      callerRole
    }).catch(() => null),
    fetch(`${notifyBase}/appointments/day-block`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        date: targetDate,
        barberId: targetBarberId,
        closeShop
      })
    }).catch(() => null)
  ]);

  await syncDateFromBackend(targetDate);
  notifySyncChannel?.postMessage('appointment_changed');
  return state.dayBlocks[targetDate];
}

export async function reopenDaySchedule({ date, barberId = null, openShop = false }) {
  const caller = state.currentUser;
  const callerId = caller ? caller.id : barberId;
  const callerRole = caller ? caller.role : 'BARBER';
  const targetDate = date || toLocalIsoDate(new Date());
  const targetBarberId = openShop ? null : (barberId || callerId);

  const notifyBase = resolveBarberNotifyApiBase();
  let notifyQuery = `?date=${targetDate}&openShop=${openShop}`;
  if (targetBarberId) notifyQuery += `&barberId=${targetBarberId}`;

  await Promise.all([
    api.appointments.reopenDay({
      date: targetDate,
      barberId: targetBarberId,
      openShop,
      callerId,
      callerRole
    }).catch(() => null),
    fetch(`${notifyBase}/appointments/day-block${notifyQuery}`, {
      method: 'DELETE'
    }).catch(() => null)
  ]);

  await syncDateFromBackend(targetDate);
  notifySyncChannel?.postMessage('appointment_changed');
  return state.dayBlocks[targetDate];
}

// Sincronização automática em segundo plano com a API (incluindo agendamentos vindos do BarberNotify)
export async function syncFromBackend() {
  try {
    const todayStr = toLocalIsoDate(new Date());
    const [staffList, servicesList, clientsList] = await Promise.all([
      api.staff.list().catch(() => null),
      api.services.list().catch(() => null),
      api.clients.list().catch(() => null)
    ]);

    if (staffList && staffList.length > 0) {
      state.users = staffList.map(normalizeUser);
      if (state.currentUser) {
        const current = state.users.find(u => u.id === state.currentUser.id);
        if (current) {
          state.currentUser = current;
          localStorage.setItem(AUTH_USER_KEY, JSON.stringify(state.currentUser));
        }
      }
    }

    if (servicesList && servicesList.length > 0) {
      state.services = servicesList.map(normalizeService);
    }

    if (clientsList && clientsList.length > 0) {
      state.clients = clientsList.map(normalizeClient);
    }

    await syncDateFromBackend(todayStr);

    saveState();
  } catch (err) {
    console.warn('[BarberAdmin State] Sincronização em segundo plano indisponível no momento:', err.message);
  }
}

// Dispara sincronização inicial
syncFromBackend();

// Formatters
export function formatCurrency(cents) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format((cents || 0) / 100);
}

async function notifyBarberNotifyAvatarChange(barberId, rawAvatarUrl) {
  try {
    const isProd = typeof window !== 'undefined' && (window.location.hostname.includes('fillipython.ddns.net') || window.location.pathname.startsWith('/BarberAdmin'));
    const notifyApiUrl = isProd
      ? `${window.location.protocol}//${window.location.host}/BarberNotify/api/barbers/${barberId}/avatar`
      : `http://localhost:8081/api/barbers/${barberId}/avatar`;

    await fetch(notifyApiUrl, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ avatarUrl: rawAvatarUrl })
    });
  } catch (e) {}
  notifySyncChannel?.postMessage('staff_updated');
}

// Upload de foto do profissional (respeita a regra de cada um editar sua própria foto e sincroniza com o BarberNotify)
export async function uploadBarberAvatar(barberId, file) {
  const caller = state.currentUser;
  const callerId = caller ? caller.id : barberId;
  const callerRole = caller ? caller.role : 'BARBER';

  const updatedStaff = await api.staff.uploadAvatar(barberId, file, callerId, callerRole);
  const normalized = normalizeUser(updatedStaff);

  const idx = state.users.findIndex(u => u.id === barberId);
  if (idx !== -1) {
    state.users[idx] = { ...state.users[idx], ...normalized };
  }

  if (state.currentUser && state.currentUser.id === barberId) {
    state.currentUser = { ...state.currentUser, ...normalized };
    try {
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(state.currentUser));
    } catch (e) {}
  }

  saveState();
  await notifyBarberNotifyAvatarChange(barberId, updatedStaff.avatarUrl);
  return normalized;
}

// Remove a foto do profissional e sincroniza com o BarberNotify para exibir avatar neutro aos clientes
export async function removeBarberAvatar(barberId) {
  const caller = state.currentUser;
  const callerId = caller ? caller.id : barberId;
  const callerRole = caller ? caller.role : 'BARBER';

  const updatedStaff = await api.staff.removeAvatar(barberId, callerId, callerRole);
  const normalized = normalizeUser(updatedStaff);

  const idx = state.users.findIndex(u => u.id === barberId);
  if (idx !== -1) {
    state.users[idx] = { ...state.users[idx], ...normalized };
  }

  if (state.currentUser && state.currentUser.id === barberId) {
    state.currentUser = { ...state.currentUser, ...normalized };
    try {
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(state.currentUser));
    } catch (e) {}
  }

  saveState();
  await notifyBarberNotifyAvatarChange(barberId, 'REMOVED');
  return normalized;
}
