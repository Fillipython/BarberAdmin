-- =============================================================================
-- BarberAdmin Database Schema & Seed Data
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- 1. TIPOS ENUMERADOS
-- =============================================================================
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('MASTER', 'BARBER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE appointment_status AS ENUM ('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_method AS ENUM ('PIX', 'DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'OUTRO');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_status AS ENUM ('PENDENTE', 'PAGO', 'ESTORNADO');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE transaction_type AS ENUM ('RECEITA_SERVICO', 'DESPESA', 'COMISSAO_PAGA', 'OUTRO');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- =============================================================================
-- 2. TABELA DE USUÁRIOS / BARBEIROS
--    - Suporta Barbeiro Master (acesso total/admin) e Barbeiros Subordinados
--    - Login via email/senha com hash seguro
--    - Permite ativação/desativação e exclusão lógica (soft delete)
-- =============================================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(25) NOT NULL UNIQUE,
    role user_role NOT NULL DEFAULT 'BARBER',
    commission_percentage NUMERIC(5, 2) NOT NULL DEFAULT 50.00,
    avatar_url TEXT,
    bio TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users (is_active) WHERE deleted_at IS NULL;

-- =============================================================================
-- 3. TABELA DE CLIENTES
--    - Cadastro unificado de clientes
--    - Métricas de visitas e total gasto calculados
-- =============================================================================
CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(25) NOT NULL UNIQUE,
    email VARCHAR(255),
    notes TEXT,
    avatar_url TEXT,
    total_visits INT NOT NULL DEFAULT 0,
    total_spent_cents BIGINT NOT NULL DEFAULT 0,
    last_visit_at TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clients_phone ON clients (phone);
CREATE INDEX IF NOT EXISTS idx_clients_name ON clients (name);

-- =============================================================================
-- 4. TABELA DE SERVIÇOS
--    - Preços em centavos (ex: 3500 = R$ 35,00)
--    - Editável pelo Barbeiro Master
-- =============================================================================
CREATE TABLE IF NOT EXISTS services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL DEFAULT 'Geral',
    price_cents INT NOT NULL CHECK (price_cents >= 0),
    duration_minutes INT NOT NULL DEFAULT 30 CHECK (duration_minutes > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_services_is_active ON services (is_active);

-- =============================================================================
-- 5. TABELA DE AGENDAMENTOS
--    - Barbeiro Master visualiza todos
--    - Barbeiros comuns visualizam apenas os seus (barber_id = current_user.id)
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    barber_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    scheduled_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status appointment_status NOT NULL DEFAULT 'CONFIRMED',
    total_price_cents INT NOT NULL DEFAULT 0,
    discount_cents INT NOT NULL DEFAULT 0,
    final_price_cents INT NOT NULL DEFAULT 0,
    notes TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_appointments_barber_date ON appointments (barber_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_appointments_client ON appointments (client_id);
CREATE INDEX IF NOT EXISTS idx_appointments_scheduled_at ON appointments (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments (status);

-- Restrição: Evita conflito duplo de horário para o mesmo barbeiro quando agendamento estiver ativo
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_barber_slot 
ON appointments (barber_id, scheduled_at) 
WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'));

-- =============================================================================
-- 6. ITENS DE SERVIÇO DO AGENDAMENTO (N:N)
--    - Guarda o snapshot do valor (price_cents_at_booking)
--    - Permite mudar o valor de um serviço especificamente neste atendimento
-- =============================================================================
CREATE TABLE IF NOT EXISTS appointment_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE RESTRICT,
    service_name VARCHAR(120) NOT NULL,
    price_cents_at_booking INT NOT NULL CHECK (price_cents_at_booking >= 0),
    duration_minutes_at_booking INT NOT NULL DEFAULT 30,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_app_services_appointment ON appointment_services (appointment_id);

-- =============================================================================
-- 7. REGISTROS FINANCEIROS / FLUXO DE CAIXA
--    - Alimenta o dashboard diário, mensal e histórico
--    - Permite ao Master acompanhar rendimento diário global ou filtrado por barbeiro
-- =============================================================================
CREATE TABLE IF NOT EXISTS financial_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL,
    barber_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    type transaction_type NOT NULL DEFAULT 'RECEITA_SERVICO',
    amount_cents INT NOT NULL,
    barber_commission_cents INT NOT NULL DEFAULT 0,
    shop_net_cents INT NOT NULL DEFAULT 0,
    payment_method payment_method NOT NULL DEFAULT 'PIX',
    payment_status payment_status NOT NULL DEFAULT 'PAGO',
    paid_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_financial_barber_paid_at ON financial_records (barber_id, paid_at);
CREATE INDEX IF NOT EXISTS idx_financial_paid_at ON financial_records (paid_at);
CREATE INDEX IF NOT EXISTS idx_financial_type ON financial_records (type);

-- =============================================================================
-- 8. VIEWS OTIMIZADAS PARA O DASHBOARD (RENDIMENTO DIÁRIO E MENSAL)
-- =============================================================================

-- Rendimento Diário (Visão por data e por barbeiro)
CREATE OR REPLACE VIEW view_daily_earnings AS
SELECT 
    CAST(f.paid_at AT TIME ZONE 'America/Sao_Paulo' AS DATE) AS reference_date,
    u.id AS barber_id,
    u.name AS barber_name,
    u.nickname AS barber_nickname,
    COUNT(f.id) AS total_transactions,
    SUM(f.amount_cents) AS total_gross_cents,
    SUM(f.barber_commission_cents) AS total_barber_cents,
    SUM(f.shop_net_cents) AS total_shop_cents,
    SUM(CASE WHEN f.payment_method = 'PIX' THEN f.amount_cents ELSE 0 END) AS pix_cents,
    SUM(CASE WHEN f.payment_method = 'DINHEIRO' THEN f.amount_cents ELSE 0 END) AS cash_cents,
    SUM(CASE WHEN f.payment_method = 'CARTAO_CREDITO' THEN f.amount_cents ELSE 0 END) AS credit_card_cents,
    SUM(CASE WHEN f.payment_method = 'CARTAO_DEBITO' THEN f.amount_cents ELSE 0 END) AS debit_card_cents
FROM financial_records f
JOIN users u ON u.id = f.barber_id
WHERE f.payment_status = 'PAGO' AND f.type = 'RECEITA_SERVICO'
GROUP BY 1, 2, 3, 4
ORDER BY reference_date DESC;

-- Rendimento Mensal Consolidado
CREATE OR REPLACE VIEW view_monthly_earnings AS
SELECT 
    EXTRACT(YEAR FROM f.paid_at AT TIME ZONE 'America/Sao_Paulo')::INT AS reference_year,
    EXTRACT(MONTH FROM f.paid_at AT TIME ZONE 'America/Sao_Paulo')::INT AS reference_month,
    u.id AS barber_id,
    u.name AS barber_name,
    COUNT(f.id) AS total_transactions,
    SUM(f.amount_cents) AS total_gross_cents,
    SUM(f.barber_commission_cents) AS total_barber_cents,
    SUM(f.shop_net_cents) AS total_shop_cents
FROM financial_records f
JOIN users u ON u.id = f.barber_id
WHERE f.payment_status = 'PAGO' AND f.type = 'RECEITA_SERVICO'
GROUP BY 1, 2, 3, 4
ORDER BY reference_year DESC, reference_month DESC;

-- =============================================================================
-- 9. DADOS INICIAIS (SEEDS)
--    - Senha padrão inicial para todos: "admin123"
--      Hash bcrypt: $2a$10$wT8hM0.UeW1m61k1qP102OfqR/5xR1iC2WvM8n2c8s7oY1E8kZ2wG
-- =============================================================================

-- Barbeiros (1 Master e 2 Barbeiros de equipe)
INSERT INTO users (id, name, nickname, email, password_hash, phone, role, commission_percentage, bio) VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'Carlos Silva',
    'Mestre Carlos',
    'master@barberadmin.com',
    '$2a$10$wT8hM0.UeW1m61k1qP102OfqR/5xR1iC2WvM8n2c8s7oY1E8kZ2wG',
    '11988880001',
    'MASTER',
    100.00,
    'Fundador e Barbeiro Master. Gestão geral, visagismo e cortes premium.'
),
(
    '00000000-0000-0000-0000-000000000002',
    'Lucas Santana',
    'Lucas Navalha',
    'lucas@barberadmin.com',
    '$2a$10$wT8hM0.UeW1m61k1qP102OfqR/5xR1iC2WvM8n2c8s7oY1E8kZ2wG',
    '11988880002',
    'BARBER',
    50.00,
    'Especialista em degradê navalhado e barba terapia.'
),
(
    '00000000-0000-0000-0000-000000000003',
    'Matheus Oliveira',
    'Matheus Fade',
    'matheus@barberadmin.com',
    '$2a$10$wT8hM0.UeW1m61k1qP102OfqR/5xR1iC2WvM8n2c8s7oY1E8kZ2wG',
    '11988880003',
    'BARBER',
    50.00,
    'Especialista em platinados, luzes e cortes modernos.'
)
ON CONFLICT (email) DO NOTHING;

-- Tabela de Serviços Completa
INSERT INTO services (name, description, category, price_cents, duration_minutes, display_order) VALUES
('Corte Tradicional', 'Corte masculino na tesoura ou máquina com acabamento', 'Cabelo', 3000, 30, 1),
('Corte Kids', 'Corte infantil com paciência e cuidado', 'Cabelo', 3000, 30, 2),
('Barba Tradicional', 'Modelagem de barba com toalha quente e balm', 'Barba', 2500, 30, 3),
('Corte & Barba', 'Combo completo corte de cabelo e barba com toalha quente', 'Combo', 5000, 60, 4),
('Corte & Sobrancelha', 'Corte masculino alinhado com design de sobrancelha', 'Combo', 3500, 30, 5),
('Corte, Barba & Sobrancelha', 'Experiência completa de cuidado masculino', 'Combo', 5500, 60, 6),
('Pezinho & Barba', 'Alinhamento do contorno do cabelo e barba feita', 'Combo', 3000, 30, 7),
('Corte & Barba Simples', 'Corte e barba alinhada express', 'Combo', 4000, 30, 8),
('Corte, Sobrancelha & Barba Simples', 'Pacote express de cuidados', 'Combo', 4500, 30, 9),
('Platinado & Corte', 'Descoloração global efeito platinado e corte estilizado', 'Química', 13000, 60, 10),
('Luzes & Corte', 'Mechas/luzes modernas com corte', 'Química', 10000, 60, 11),
('Botox / Desondulação', 'Redução de volume e alinhamento dos fios', 'Química', 5000, 60, 12),
('Selagem Capilar', 'Tratamento intensivo para selar cutículas e brilho', 'Química', 7500, 60, 13),
('Hidratação Profunda', 'Tratamento nutritivo pós-corte', 'Tratamento', 2000, 15, 14),
('Alisamento', 'Alisamento capilar masculino de alta durabilidade', 'Química', 2000, 15, 15),
('Pigmentação de Barba/Cabelo', 'Camuflagem de falhas e realce de contorno', 'Acabamento', 2000, 30, 16)
ON CONFLICT DO NOTHING;

-- Clientes Iniciais
INSERT INTO clients (id, name, phone, email, notes, total_visits, total_spent_cents) VALUES
(
    '10000000-0000-0000-0000-000000000001',
    'Rodrigo Albuquerque',
    '11977771001',
    'rodrigo@email.com',
    'Gosta do degradê bem baixo, disfarçado na navalha.',
    6,
    30000
),
(
    '10000000-0000-0000-0000-000000000002',
    'Gabriel Santos',
    '11977771002',
    'gabriel@email.com',
    'Prefere barba mais desenhada e toalha bem quente.',
    3,
    15000
),
(
    '10000000-0000-0000-0000-000000000003',
    'Felipe Almeida',
    '11977771003',
    'felipe@email.com',
    'Cliente pontual, faz platinado semestralmente.',
    4,
    35000
)
ON CONFLICT (phone) DO NOTHING;

-- Exemplos de Agendamentos e Financeiro (Hoje e dias recentes para teste de dashboard)
INSERT INTO appointments (id, client_id, barber_id, scheduled_at, end_at, status, total_price_cents, final_price_cents, notes, completed_at) VALUES
(
    '20000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    CURRENT_DATE + TIME '09:00:00',
    CURRENT_DATE + TIME '10:00:00',
    'COMPLETED',
    5000,
    5000,
    'Corte & Barba finalizado com sucesso.',
    CURRENT_DATE + TIME '10:00:00'
),
(
    '20000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    CURRENT_DATE + TIME '10:30:00',
    CURRENT_DATE + TIME '11:00:00',
    'COMPLETED',
    3000,
    3000,
    'Corte tradicional.',
    CURRENT_DATE + TIME '11:00:00'
),
(
    '20000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000003',
    CURRENT_DATE + TIME '14:00:00',
    CURRENT_DATE + TIME '15:00:00',
    'CONFIRMED',
    13000,
    13000,
    'Platinado & Corte agendado para a tarde.',
    NULL
)
ON CONFLICT DO NOTHING;

-- Registro Financeiro correspondente às ordens concluídas
INSERT INTO financial_records (appointment_id, barber_id, client_id, type, amount_cents, barber_commission_cents, shop_net_cents, payment_method, payment_status, paid_at, description) VALUES
(
    '20000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'RECEITA_SERVICO',
    5000,
    5000,
    0,
    'PIX',
    'PAGO',
    CURRENT_DATE + TIME '10:05:00',
    'Atendimento Mestre Carlos - Corte & Barba (Rodrigo)'
),
(
    '20000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002',
    'RECEITA_SERVICO',
    3000,
    1500,
    1500,
    'CARTAO_CREDITO',
    'PAGO',
    CURRENT_DATE + TIME '11:02:00',
    'Atendimento Lucas Navalha - Corte (Gabriel)'
)
ON CONFLICT DO NOTHING;
