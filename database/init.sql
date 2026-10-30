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
--      Hash bcrypt: $2b$10$HE9vLiZI187bFCw7eMg7reU4TU5ss8VAkPskkbGQXkSjUVVyKRJH6
-- =============================================================================

-- Barbeiros Reais (Sincronizados com BarberNotify)
INSERT INTO users (id, name, nickname, email, password_hash, phone, role, commission_percentage, avatar_url, bio) VALUES
(
    '8a350074-5524-453d-a625-2b25e7e8ca59',
    'Carlos Silva',
    'Mestre Carlos',
    'contato@barberman.com.br',
    '$2b$10$HE9vLiZI187bFCw7eMg7reU4TU5ss8VAkPskkbGQXkSjUVVyKRJH6',
    '11988880001',
    'MASTER',
    100.00,
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=250&auto=format&fit=crop&q=80',
    'Fundador e Administrador Geral do Barber Man. Especialista em visagismo e cortes premium.'
),
(
    '70b1fee3-a338-4444-90f9-f4551afc15fe',
    'Lucas Santana',
    'Lucas Navalha',
    'lucas@barberman.com.br',
    '$2b$10$HE9vLiZI187bFCw7eMg7reU4TU5ss8VAkPskkbGQXkSjUVVyKRJH6',
    '11988880002',
    'BARBER',
    50.00,
    'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=250&auto=format&fit=crop&q=80',
    'Especialista em degrade navalhado e barba terapia.'
),
(
    '0670b734-45c1-4435-ad0e-b8d6b3099c7e',
    'Matheus Oliveira',
    'Matheus Fade',
    'matheus@barberman.com.br',
    '$2b$10$HE9vLiZI187bFCw7eMg7reU4TU5ss8VAkPskkbGQXkSjUVVyKRJH6',
    '11988880003',
    'BARBER',
    50.00,
    'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=250&auto=format&fit=crop&q=80',
    'Especialista em platinados, luzes e cortes modernos.'
)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, nickname = EXCLUDED.nickname, email = EXCLUDED.email, password_hash = EXCLUDED.password_hash, avatar_url = COALESCE(users.avatar_url, EXCLUDED.avatar_url);

-- Tabela de Serviços Completa (16 Serviços Reais do BarberNotify)
INSERT INTO services (id, name, description, category, price_cents, duration_minutes, display_order) VALUES
('16b14dc5-1df2-4041-a26b-4b292ca2db35', 'Botox/desondulacao', 'Redução de volume e alinhamento dos fios', 'Química', 5000, 60, 1),
('a7cb2ec3-f68f-42ff-859a-58666d3b727c', 'Selagem', 'Tratamento intensivo para selar cutículas e brilho', 'Química', 7500, 60, 2),
('b5eac08f-4c62-40be-b974-c00881aa99e0', 'Hidratacao', 'Tratamento nutritivo pós-corte', 'Tratamento', 2000, 15, 3),
('650e3202-2f1f-435f-91b9-442e47912118', 'Alisamento', 'Alisamento capilar masculino de alta durabilidade', 'Química', 2000, 15, 4),
('187ebf61-e48d-4665-a71e-8076768b3915', 'Pigmentacao', 'Camuflagem de falhas e realce de contorno', 'Acabamento', 2000, 30, 5),
('5379777b-3c95-42b4-9baa-06b621a325b8', 'Corte', 'Corte masculino tradicional tesoura/máquina', 'Cabelo', 3000, 30, 6),
('1332638b-4d7f-467f-ba2e-c0f6614ee0c6', 'Corte kids', 'Corte infantil com paciência e cuidado', 'Cabelo', 3000, 30, 7),
('bd3ac8a7-5955-4a97-a275-c621f635e635', 'Corte & Sobrancelha', 'Corte masculino alinhado com design de sobrancelha', 'Combo', 3500, 30, 8),
('b629d3af-6e6c-4c2f-930c-becd3c78bb58', 'Platinado & Corte', 'Descoloração global efeito platinado e corte estilizado', 'Química', 13000, 60, 9),
('497594fe-cdc2-4ae5-a928-523ffd3fd522', 'Luzes & Corte', 'Mechas/luzes modernas com corte', 'Química', 10000, 60, 10),
('85f353f0-229b-4586-9850-dc2b65434a63', 'Corte & Barba', 'Combo completo corte de cabelo e barba com toalha quente', 'Combo', 5000, 60, 11),
('e3ce8dc4-44f0-4d62-95d1-8651ea77d353', 'Barba', 'Modelagem de barba com toalha quente e balm', 'Barba', 2500, 30, 12),
('80af0829-4a7f-4745-a945-c7c8fa2dd00d', 'Corte, Barba & Sobrancelha', 'Experiência completa de cuidado masculino', 'Combo', 5500, 60, 13),
('9095beb0-8eb7-447c-aa9f-e52e03e65c36', 'Corte & barba simples', 'Corte e barba alinhada express', 'Combo', 4000, 30, 14),
('3dcf6649-b31e-4fad-b493-4d06c86b5ff1', 'Corte, sobrancelha & barba simples', 'Pacote express de cuidados', 'Combo', 4500, 30, 15),
('3d0ecd31-320a-4f35-bd57-071a3bb66ec8', 'Pezinho & Barba', 'Alinhamento do contorno do cabelo e barba feita', 'Combo', 3000, 30, 16)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, price_cents = EXCLUDED.price_cents, duration_minutes = EXCLUDED.duration_minutes;

-- Clientes Fictícios (10 Clientes de Exemplo)
INSERT INTO clients (id, name, phone, email, notes, total_visits, total_spent_cents) VALUES
('c1000000-0000-0000-0000-000000000001', 'Gabriel Santos', '11977771001', 'gabriel.santos@exemplo.com', 'Prefere degradê navalhado.', 4, 20000),
('c1000000-0000-0000-0000-000000000002', 'Rodrigo Albuquerque', '11977771002', 'rodrigo.albuquerque@exemplo.com', 'Toalha quente na barba.', 6, 30000),
('c1000000-0000-0000-0000-000000000003', 'Rafael Moreira', '11977771003', 'rafael.moreira@exemplo.com', 'Platinado e corte.', 3, 39000),
('c1000000-0000-0000-0000-000000000004', 'Bruno Mendes', '11977771004', 'bruno.mendes@exemplo.com', 'Corte tradicional tesoura.', 8, 24000),
('c1000000-0000-0000-0000-000000000005', 'Thiago Costa', '11977771005', 'thiago.costa@exemplo.com', 'Sobrancelha alinhada.', 5, 17500),
('c1000000-0000-0000-0000-000000000006', 'Vinicius Pereira', '11977771006', 'vinicius.pereira@exemplo.com', 'Pigmentação de barba.', 2, 10000),
('c1000000-0000-0000-0000-000000000007', 'Eduardo Ramos', '11977771007', 'eduardo.ramos@exemplo.com', 'Corte infantil do filho.', 7, 21000),
('c1000000-0000-0000-0000-000000000008', 'Gustavo Henrique', '11977771008', 'gustavo.henrique@exemplo.com', 'Luzes e degrade.', 3, 30000),
('c1000000-0000-0000-0000-000000000009', 'Leonardo Souza', '11977771009', 'leonardo.souza@exemplo.com', 'Barboterapia completa.', 5, 25000),
('c1000000-0000-0000-0000-000000000010', 'Marcelo Antunes', '11977771010', 'marcelo.antunes@exemplo.com', 'Cliente corporativo pontual.', 9, 45000)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, phone = EXCLUDED.phone, email = EXCLUDED.email;

-- Agendamentos Fictícios de Exemplo
INSERT INTO appointments (id, client_id, barber_id, scheduled_at, end_at, status, total_price_cents, discount_cents, final_price_cents, notes, completed_at) VALUES
('a1000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000001', '8a350074-5524-453d-a625-2b25e7e8ca59', CURRENT_DATE + TIME '12:00:00', CURRENT_DATE + TIME '13:00:00', 'COMPLETED', 13000, 0, 13000, 'Platinado e Corte', CURRENT_DATE + TIME '13:00:00'),
('a1000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000002', '70b1fee3-a338-4444-90f9-f4551afc15fe', CURRENT_DATE + TIME '14:00:00', CURRENT_DATE + TIME '15:00:00', 'CONFIRMED', 5000, 0, 5000, 'Corte & Barba', NULL)
ON CONFLICT (id) DO NOTHING;

-- Registros Financeiros Fictícios dos Agendamentos
INSERT INTO financial_records (appointment_id, barber_id, client_id, type, amount_cents, barber_commission_cents, shop_net_cents, payment_method, payment_status, paid_at, description) VALUES
('a1000000-0000-0000-0000-000000000001', '8a350074-5524-453d-a625-2b25e7e8ca59', 'c1000000-0000-0000-0000-000000000001', 'RECEITA_SERVICO', 13000, 13000, 0, 'PIX', 'PAGO', CURRENT_DATE + TIME '13:05:00', 'Recebimento de Agendamento')
ON CONFLICT DO NOTHING;
