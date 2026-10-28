<template>
  <div class="team-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Equipe de Barbeiros</h1>
        <p class="page-subtitle">Profissionais cadastrados e percentuais de comissão.</p>
      </div>

      <button v-if="isMaster" class="btn btn-primary" @click="showModal = true">
        + Adicionar Barbeiro
      </button>
    </div>

    <!-- Restrict Warning -->
    <!-- Barbers List -->
    <div class="team-grid">
      <div 
        v-for="barber in state.users" 
        :key="barber.id" 
        class="clean-panel barber-card"
        :class="{ 'master-card': barber.role === 'MASTER' }"
      >
        <div class="barber-head">
          <!-- AVATAR COM OVERLAY DE UPLOAD -->
          <div class="barber-avatar-box">
            <img :src="barber.avatar_url" :alt="barber.name" class="barber-img" />
            <label 
              v-if="canEditAvatar(barber)" 
              class="card-avatar-edit" 
              title="Alterar foto de perfil"
            >
              <input 
                type="file" 
                accept="image/png,image/jpeg,image/webp" 
                @change="(e) => handleCardAvatarUpload(barber.id, e)" 
                style="display:none;" 
              />
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" class="cam-icon-sm">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
            </label>
          </div>

          <div class="barber-info">
            <h3 class="barber-name">{{ barber.name }}</h3>
            <span class="barber-nick">{{ barber.nickname }}</span>
          </div>
          <span class="badge" :class="barber.role === 'MASTER' ? 'badge-gold' : 'badge-blue'">
            {{ barber.role === 'MASTER' ? 'Master' : 'Staff' }}
          </span>
        </div>

        <div class="barber-details">
          <div class="detail-row">
            <span>WhatsApp:</span>
            <span class="detail-val">{{ barber.phone }}</span>
          </div>
          <div class="detail-row">
            <span>Email:</span>
            <span class="detail-val">{{ barber.email }}</span>
          </div>
          <div class="detail-row">
            <span>Comissão:</span>
            <span class="detail-val gold-text bold">{{ barber.commission_percentage }}%</span>
          </div>
        </div>

        <div class="card-footer">
          <!-- BOTÃO CLARO E VISÍVEL PARA ALTERAR OU REMOVER FOTO -->
          <button 
            v-if="canEditAvatar(barber)" 
            type="button" 
            class="btn btn-secondary btn-sm full-width btn-card-avatar" 
            @click="openBarberPhotoModal(barber)"
          >
            📷 Alterar / Remover Foto
          </button>

          <button 
            v-if="isMaster && barber.role !== 'MASTER'" 
            class="btn btn-danger btn-sm full-width" 
            @click="handleRemoveBarber(barber)"
          >
            Remover Barbeiro
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL DEDICADO DE GESTÃO DE FOTO DO PROFISSIONAL -->
    <div v-if="selectedBarberForPhoto" class="modal-backdrop" @click.self="selectedBarberForPhoto = null">
      <div class="modal-content profile-photo-modal">
        <div class="modal-header">
          <h2 class="modal-title">Foto de Perfil</h2>
          <button class="btn-icon btn-secondary" @click="selectedBarberForPhoto = null">✕</button>
        </div>

        <div class="modal-body profile-modal-body">
          <div class="large-avatar-preview">
            <img :src="selectedBarberForPhoto.avatar_url" :alt="selectedBarberForPhoto.name" class="large-avatar-img" />
            <div v-if="uploadingAvatar" class="avatar-loading-overlay">
              <span class="loading-spinner">⏳</span>
              <span>Processando...</span>
            </div>
          </div>

          <div class="profile-info-text">
            <h3 class="profile-name">{{ selectedBarberForPhoto.name }}</h3>
            <span class="profile-nick">{{ selectedBarberForPhoto.nickname }}</span>
            <span class="badge" :class="selectedBarberForPhoto.role === 'MASTER' ? 'badge-gold' : 'badge-blue'">
              {{ selectedBarberForPhoto.role === 'MASTER' ? 'Master' : 'Barbeiro' }}
            </span>
          </div>

          <div class="photo-actions-grid">
            <label class="btn btn-primary btn-upload-photo" :class="{ 'btn-disabled': uploadingAvatar }">
              <input 
                type="file" 
                accept="image/png,image/jpeg,image/webp" 
                @change="handleModalAvatarUpload" 
                style="display:none;" 
                :disabled="uploadingAvatar"
              />
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="action-btn-svg">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/>
                <line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              <span>{{ uploadingAvatar ? 'Enviando...' : 'Escolher Nova Foto' }}</span>
            </label>

            <button 
              type="button" 
              class="btn btn-danger btn-remove-photo" 
              @click="handleModalRemoveAvatar"
              :disabled="uploadingAvatar"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="action-btn-svg">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
              <span>Remover Foto</span>
            </button>
          </div>

          <p class="photo-hint">
            Formatos suportados: <strong>JPG, PNG ou WebP</strong> (máximo 5MB).<br>
            A foto é salva em disco no servidor com link otimizado, sem inchar o banco.
          </p>
        </div>

        <div class="modal-footer">
          <button type="button" class="btn btn-secondary full-width" @click="selectedBarberForPhoto = null">
            Concluir
          </button>
        </div>
      </div>
    </div>

    <!-- MODAL: ADICIONAR BARBEIRO -->
    <div v-if="showModal" class="modal-backdrop" @click.self="showModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">Novo Barbeiro</h2>
          <button class="btn-icon btn-secondary" @click="showModal = false">✕</button>
        </div>

        <form @submit.prevent="handleSaveBarber" class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Nome Completo *</label>
              <input v-model="form.name" required placeholder="Ex: Roberto Alves" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Apelido *</label>
              <input v-model="form.nickname" required placeholder="Ex: Beto Navalha" class="form-input" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">WhatsApp *</label>
              <input v-model="form.phone" required placeholder="Ex: 11977772222" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Comissão (% sobre o corte) *</label>
              <input 
                type="number" 
                v-model.number="form.commission_percentage" 
                min="0" 
                max="100" 
                required 
                class="form-input" 
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Email de Acesso (Login) *</label>
            <input v-model="form.email" type="email" required placeholder="barbeiro@barbearia.com" class="form-input" />
          </div>

          <div class="form-group">
            <label class="form-label">Senha Inicial *</label>
            <input v-model="form.password" type="password" required placeholder="Senha de acesso..." class="form-input" />
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="showModal = false">Cancelar</button>
            <button type="submit" class="btn btn-primary">Adicionar Barbeiro</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { state, isMaster, addBarber, removeBarber, uploadBarberAvatar, removeBarberAvatar } from '../store/state.js';

const showModal = ref(false);
const selectedBarberForPhoto = ref(null);
const uploadingAvatar = ref(false);

const form = ref({
  name: '',
  nickname: '',
  phone: '',
  email: '',
  password: '',
  commission_percentage: 50
});

function canEditAvatar(barber) {
  // Master pode trocar de qualquer um; cada barbeiro pode trocar a sua própria
  return isMaster.value || (state.currentUser && state.currentUser.id === barber.id);
}

function openBarberPhotoModal(barber) {
  selectedBarberForPhoto.value = barber;
}

async function handleModalAvatarUpload(event) {
  const file = event.target.files?.[0];
  if (!file || !selectedBarberForPhoto.value) return;

  if (file.size > 5 * 1024 * 1024) {
    alert('A foto deve ter no máximo 5MB.');
    event.target.value = '';
    return;
  }

  try {
    uploadingAvatar.value = true;
    const updated = await uploadBarberAvatar(selectedBarberForPhoto.value.id, file);
    selectedBarberForPhoto.value = updated;
    alert('Foto do profissional atualizada com sucesso!');
  } catch (err) {
    alert('Erro ao enviar foto: ' + (err.message || 'Falha no envio'));
  } finally {
    uploadingAvatar.value = false;
    event.target.value = '';
  }
}

async function handleModalRemoveAvatar() {
  if (!selectedBarberForPhoto.value) return;

  if (confirm(`Deseja remover a foto de ${selectedBarberForPhoto.value.name} e restaurar o avatar padrão?`)) {
    try {
      uploadingAvatar.value = true;
      const updated = await removeBarberAvatar(selectedBarberForPhoto.value.id);
      selectedBarberForPhoto.value = updated;
      alert('Foto removida com sucesso!');
    } catch (err) {
      alert('Erro ao remover foto: ' + (err.message || 'Falha na remoção'));
    } finally {
      uploadingAvatar.value = false;
    }
  }
}

async function handleCardAvatarUpload(barberId, event) {
  const file = event.target.files?.[0];
  if (!file) return;

  if (file.size > 5 * 1024 * 1024) {
    alert('A foto deve ter no máximo 5MB.');
    event.target.value = '';
    return;
  }

  try {
    await uploadBarberAvatar(barberId, file);
    alert('Foto atualizada!');
  } catch (err) {
    alert('Erro ao enviar foto: ' + (err.message || 'Falha no envio'));
  } finally {
    event.target.value = '';
  }
}

function handleSaveBarber() {
  addBarber({
    name: form.value.name,
    nickname: form.value.nickname,
    phone: form.value.phone,
    email: form.value.email,
    commission_percentage: form.value.commission_percentage
  });

  form.value = {
    name: '',
    nickname: '',
    phone: '',
    email: '',
    password: '',
    commission_percentage: 50
  };
  showModal.value = false;
}

function handleRemoveBarber(barber) {
  if (confirm(`Tem certeza que deseja remover ${barber.name} (${barber.nickname}) da barbearia?`)) {
    removeBarber(barber.id);
  }
}
</script>

<style scoped>
.team-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 600;
}

.page-subtitle {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.75rem;
}

.barber-card {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.master-card {
  border-left: 2px solid var(--accent-gold);
}

.barber-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.barber-avatar-box {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  border: 1.5px solid var(--border-subtle);
  background: #18202f;
  flex-shrink: 0;
}

.barber-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-avatar-edit {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.barber-avatar-box:hover .card-avatar-edit {
  opacity: 1;
}

.cam-icon-sm {
  width: 18px;
  height: 18px;
  color: #fbbf24;
}

.barber-info {
  flex: 1;
  min-width: 0;
}

.barber-name {
  font-size: 0.95rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.barber-nick {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.barber-details {
  background: #090c13;
  border: 1px solid var(--border-subtle);
  padding: 0.5rem 0.65rem;
  border-radius: var(--radius-xs);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.785rem;
  color: var(--text-secondary);
}

.detail-val {
  color: var(--text-primary);
}

.bold {
  font-weight: 600;
}

.gold-text {
  color: #fbbf24;
}

.card-footer {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.btn-card-avatar {
  background: rgba(251, 191, 36, 0.08);
  border: 1px solid rgba(251, 191, 36, 0.25);
  color: #fbbf24;
  font-weight: 600;
  transition: all 0.2s ease;
}

.btn-card-avatar:hover {
  background: rgba(251, 191, 36, 0.16);
  border-color: #fbbf24;
}

.full-width {
  width: 100%;
}

/* Modal Profile Styling */
.profile-photo-modal {
  max-width: 380px;
}

.profile-modal-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 1.25rem 1rem;
  gap: 1rem;
}

.large-avatar-preview {
  position: relative;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  border: 3px solid var(--accent-gold);
  box-shadow: 0 0 20px rgba(251, 191, 36, 0.2);
  overflow: hidden;
  background: #18202f;
}

.large-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  color: #fbbf24;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.loading-spinner {
  font-size: 1.25rem;
  animation: spin 1s linear infinite;
}

.profile-info-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
}

.profile-name {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-primary);
}

.profile-nick {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.photo-actions-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
}

.btn-upload-photo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.65rem 1rem;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 600;
}

.btn-remove-photo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.6rem 1rem;
  font-size: 0.825rem;
}

.action-btn-svg {
  width: 16px;
  height: 16px;
}

.btn-disabled {
  opacity: 0.6;
  pointer-events: none;
}

.photo-hint {
  font-size: 0.72rem;
  color: var(--text-tertiary);
  line-height: 1.35;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.modal-header {
  padding: 0.85rem 1.25rem;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  font-size: 1rem;
  font-weight: 600;
}

.modal-body {
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--border-subtle);
}
</style>
