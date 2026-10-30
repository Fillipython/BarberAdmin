function resolveApiBase() {
  if (typeof window !== 'undefined') {
    const host = window.location.hostname;
    const protocol = window.location.protocol;
    if (
      host.includes('fillipython.ddns.net') ||
      window.location.pathname.startsWith('/BarberAdmin') ||
      (host !== 'localhost' && host !== '127.0.0.1')
    ) {
      return `${protocol}//${window.location.host}/BarberAdmin/api`;
    }
  }
  if (import.meta.env.VITE_API_URL && !import.meta.env.VITE_API_URL.includes('localhost')) {
    const envUrl = import.meta.env.VITE_API_URL.replace(/\/+$/, '');
    return envUrl.endsWith('/api') ? envUrl : `${envUrl}/api`;
  }
  return 'http://localhost:8080/api';
}

const API_BASE = resolveApiBase();
const V1 = `${API_BASE}/v1`;

async function request(endpoint, options = {}) {
  const url = `${V1}${endpoint}`;
  const headers = {
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {})
  };

  try {
    const response = await fetch(url, { ...options, headers });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Erro HTTP ${response.status}`);
    }
    if (response.status === 204) return null;
    return await response.json();
  } catch (error) {
    console.warn(`[BarberAdmin API] Falha na requisição para ${endpoint}:`, error.message);
    throw error;
  }
}

export const api = {
  // Autenticação
  auth: {
    login: (credentials) => request('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials)
    })
  },

  // BarberNotify
  barbernotify: {
    sendWebhookEvent: (eventData) => request('/barbernotify/sync', {
      method: 'POST',
      body: JSON.stringify(eventData)
    }),
    getStatus: () => request('/barbernotify/status')
  },

  // Agendamentos
  appointments: {
    list: (dateStr, barberId = null, role = 'MASTER') => {
      let q = `?date=${dateStr}&role=${role}`;
      if (barberId) q += `&barberId=${barberId}`;
      return request(`/appointments${q}`);
    },
    create: (data) => request('/appointments', {
      method: 'POST',
      body: JSON.stringify(data)
    }),
    overridePrice: (id, priceCents) => request(`/appointments/${id}/price`, {
      method: 'PATCH',
      body: JSON.stringify({ priceCents })
    }),
    changeService: (id, serviceId, customPriceCents = null) => request(`/appointments/${id}/service`, {
      method: 'PATCH',
      body: JSON.stringify({ serviceId, customPriceCents })
    }),
    updateStatus: (id, status) => request(`/appointments/${id}/status?status=${status}`, {
      method: 'PATCH'
    }),
    checkout: (id, paymentMethod, finalAmountCents, notes = '') => request(`/appointments/${id}/checkout`, {
      method: 'POST',
      body: JSON.stringify({ paymentMethod, finalAmountCents, notes })
    })
  },

  // Dashboard Financeiro
  dashboard: {
    daily: (dateStr, barberId = null, role = 'MASTER') => {
      let q = `?date=${dateStr}&role=${role}`;
      if (barberId) q += `&barberId=${barberId}`;
      return request(`/dashboard/daily${q}`);
    },
    monthly: (monthStr, barberId = null, role = 'MASTER') => {
      let q = `?month=${monthStr}&role=${role}`;
      if (barberId) q += `&barberId=${barberId}`;
      return request(`/dashboard/monthly${q}`);
    }
  },

  // Clientes
  clients: {
    list: (search = '') => request(`/clients${search ? `?search=${encodeURIComponent(search)}` : ''}`),
    create: (data) => request('/clients', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/clients/${id}`, { method: 'PUT', body: JSON.stringify(data) })
  },

  // Barbeiros
  staff: {
    list: () => request('/staff'),
    create: (data) => request('/staff', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/staff/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => request(`/staff/${id}`, { method: 'DELETE' }),
    uploadAvatar: async (id, file, callerId, callerRole = 'BARBER') => {
      const formData = new FormData();
      formData.append('file', file);
      let query = '';
      if (callerId) query += `?callerId=${callerId}&callerRole=${callerRole}`;
      const url = `${V1}/staff/${id}/avatar${query}`;
      const response = await fetch(url, {
        method: 'POST',
        body: formData
      });
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Erro ${response.status} ao enviar foto`);
      }
      return await response.json();
    },
    removeAvatar: async (id, callerId, callerRole = 'BARBER') => {
      let query = '';
      if (callerId) query += `?callerId=${callerId}&callerRole=${callerRole}`;
      return request(`/staff/${id}/avatar${query}`, { method: 'DELETE' });
    }
  },

  // Serviços
  services: {
    list: () => request('/services'),
    create: (data) => request('/services', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/services/${id}`, { method: 'PUT', body: JSON.stringify(data) })
  }
};
