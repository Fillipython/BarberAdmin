<template>
  <div class="services-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Tabela de Serviços & Valores</h1>
        <p class="page-subtitle">
          {{ isMaster 
              ? 'Gerenciamento de serviços, durações e preços praticados na barbearia.' 
              : 'Consulte os serviços disponíveis e tabela de preços vigentes.' }}
        </p>
      </div>

      <button v-if="isMaster" class="btn btn-primary" @click="openNewModal">
        <span>✂️</span> Adicionar Serviço
      </button>
    </div>

    <!-- Category filter tabs -->
    <div class="category-tabs">
      <button 
        class="cat-btn" 
        :class="{ active: selectedCategory === 'ALL' }" 
        @click="selectedCategory = 'ALL'"
      >
        Todos ({{ state.services.length }})
      </button>
      <button 
        v-for="cat in categories" 
        :key="cat" 
        class="cat-btn" 
        :class="{ active: selectedCategory === cat }" 
        @click="selectedCategory = cat"
      >
        {{ cat }}
      </button>
    </div>

    <!-- Services Grid -->
    <div class="services-grid">
      <div 
        v-for="service in filteredServices" 
        :key="service.id" 
        class="glass-card service-card glass-card-hover"
        :class="{ 'inactive-service': !service.is_active }"
      >
        <div class="service-top">
          <span class="badge" :class="getCategoryBadgeClass(service.category)">
            {{ service.category }}
          </span>
          <span class="duration-tag">⏱️ {{ service.duration_minutes }} min</span>
        </div>

        <div class="service-body">
          <h3 class="service-title">{{ service.name }}</h3>
          <p v-if="service.description" class="service-desc">{{ service.description }}</p>
        </div>

        <div class="service-bottom">
          <div class="price-box">
            <span class="price-label">Valor:</span>
            <span class="price-val gold-text">{{ formatCurrency(service.price_cents) }}</span>
          </div>

          <div v-if="isMaster" class="service-actions">
            <button class="btn btn-secondary btn-sm" @click="openEditModal(service)">
              ✏️ Editar
            </button>
            <button 
              class="btn btn-sm" 
              :class="service.is_active ? 'btn-danger' : 'btn-primary'" 
              @click="toggleServiceStatus(service.id)"
            >
              {{ service.is_active ? 'Desativar' : 'Ativar' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL: EDITAR / CRIAR SERVIÇO (MASTER) -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">{{ isEditing ? 'Editar Serviço' : 'Novo Serviço' }}</h2>
          <button class="btn-icon btn-secondary" @click="showModal = false">✕</button>
        </div>

        <form @submit.prevent="handleSaveService" class="modal-body">
          <div class="form-group">
            <label class="form-label">Nome do Serviço *</label>
            <input v-model="form.name" required placeholder="Ex: Degradê Navalhado" class="form-input" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Categoria *</label>
              <select v-model="form.category" required class="form-select">
                <option value="Cabelo">Cabelo</option>
                <option value="Barba">Barba</option>
                <option value="Combo">Combo</option>
                <option value="Química">Química</option>
                <option value="Tratamento">Tratamento</option>
                <option value="Acabamento">Acabamento</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Duração (Minutos) *</label>
              <input type="number" v-model.number="form.duration_minutes" required min="5" step="5" class="form-input" />
            </div>
          </div>

          <div class="form-group highlight-box">
            <label class="form-label">Preço do Serviço (R$) *</label>
            <input 
              type="number" 
              step="0.50" 
              v-model.number="formPriceDisplay" 
              required 
              class="form-input price-input" 
            />
          </div>

          <div class="form-group">
            <label class="form-label">Descrição (Opcional)</label>
            <textarea v-model="form.description" rows="2" placeholder="Detalhes do procedimento..." class="form-textarea"></textarea>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="showModal = false">Cancelar</button>
            <button type="submit" class="btn btn-primary">
              {{ isEditing ? 'Salvar Alterações' : 'Cadastrar Serviço' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { 
  state, 
  isMaster, 
  formatCurrency, 
  addService, 
  updateService, 
  toggleServiceStatus 
} from '../store/state.js';

const selectedCategory = ref('ALL');
const showModal = ref(false);
const isEditing = ref(false);
const editingId = ref(null);
const formPriceDisplay = ref(30.00);

const form = ref({
  name: '',
  category: 'Cabelo',
  duration_minutes: 30,
  description: ''
});

const categories = computed(() => {
  const set = new Set(state.services.map(s => s.category));
  return Array.from(set);
});

const filteredServices = computed(() => {
  if (selectedCategory.value === 'ALL') return state.services;
  return state.services.filter(s => s.category === selectedCategory.value);
});

function openNewModal() {
  isEditing.value = false;
  editingId.value = null;
  form.value = {
    name: '',
    category: 'Cabelo',
    duration_minutes: 30,
    description: ''
  };
  formPriceDisplay.value = 35.00;
  showModal.value = true;
}

function openEditModal(service) {
  isEditing.value = true;
  editingId.value = service.id;
  form.value = {
    name: service.name,
    category: service.category,
    duration_minutes: service.duration_minutes,
    description: service.description || ''
  };
  formPriceDisplay.value = service.price_cents / 100;
  showModal.value = true;
}

function handleSaveService() {
  const price_cents = Math.round(formPriceDisplay.value * 100);

  if (isEditing.value) {
    updateService(editingId.value, {
      ...form.value,
      price_cents
    });
  } else {
    addService({
      ...form.value,
      price_cents
    });
  }

  showModal.value = false;
}

function getCategoryBadgeClass(cat) {
  const map = {
    Cabelo: 'badge-blue',
    Barba: 'badge-gold',
    Combo: 'badge-emerald',
    Química: 'badge-rose'
  };
  return map[cat] || 'badge-muted';
}
</script>

<style scoped>
.services-page {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-title {
  font-size: 1.75rem;
}

.page-subtitle {
  color: var(--text-muted);
  font-size: 0.95rem;
}

.category-tabs {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

.cat-btn {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
  padding: 0.5rem 1rem;
  border-radius: var(--radius-full);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.cat-btn.active {
  background: var(--gold-500);
  color: #0b0f19;
  border-color: var(--gold-500);
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.25rem;
}

.service-card {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.inactive-service {
  opacity: 0.5;
}

.service-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.duration-tag {
  font-size: 0.75rem;
  color: var(--text-dim);
}

.service-title {
  font-size: 1.15rem;
  color: var(--text-main);
  margin-bottom: 0.25rem;
}

.service-desc {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.service-bottom {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  border-top: 1px solid var(--border-color);
  padding-top: 0.75rem;
  margin-top: auto;
}

.price-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  color: var(--text-dim);
  display: block;
}

.price-val {
  font-size: 1.35rem;
  font-weight: 800;
  font-family: var(--font-display);
}

.gold-text {
  color: #fbbf24;
}

.service-actions {
  display: flex;
  gap: 0.5rem;
}

/* Modal */
.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1.25rem;
}

.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.highlight-box {
  background: rgba(245, 158, 11, 0.05);
  border: 1px dashed rgba(245, 158, 11, 0.3);
  padding: 1rem;
  border-radius: var(--radius-md);
}

.price-input {
  font-size: 1.35rem;
  font-weight: 800;
  color: #fbbf24;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}
</style>
