const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const V1 = `${API_BASE}/api/v1`;

async function request(endpoint, options = {}) {
  const url = `${V1}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
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
  // BarberNotify
  barbernotify: {
    sendWebhookEvent: (eventData) => request('/barbernotify/webhook', {
      method: 'POST',
      body: JSON.stringify(eventData),
      headers: { 'X-BarberNotify-Secret': 'barbernotify_webhook_secret_2026' }
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
    delete: (id) => request(`/staff/${id}`, { method: 'DELETE' })
  },

  // Serviços
  services: {
    list: () => request('/services'),
    create: (data) => request('/services', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/services/${id}`, { method: 'PUT', body: JSON.stringify(data) })
  }
};
