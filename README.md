# BarberAdmin - Backend & Database

Sistema de gestão administrativa para barbearias focado em alto desempenho, permitindo uso tanto na Web quanto empacotado como aplicativo mobile/desktop no futuro.

---

## 🚀 Como Subir o Banco de Dados e Adminer

Certifique-se de ter o **Docker** e o **Docker Compose** instalados e em execução.

### 1. Iniciar os Containers
```bash
docker compose up -d
```

### 2. Acessar o Adminer (Interface Web do Banco)
- **URL**: [http://localhost:8080](http://localhost:8080)
- **Sistema**: `PostgreSQL`
- **Servidor**: `postgres`
- **Usuário**: `barberadmin`
- **Senha**: `barberadmin_secret_2026`
- **Base de dados**: `barberadmin_db`

---

## 🏛️ Modelagem e Regras de Negócio Implementadas

### 1. Perfis de Barbeiros e Acessos
- **Barbeiro Master (`role: MASTER`)**:
  - Acesso irrestrito a todas as áreas.
  - Visualiza rendimento diário, semanal, mensal e histórico consolidado de toda a barbearia ou por barbeiro individual.
  - Consulta a carteira unificada de todos os clientes.
  - Visualiza a agenda e atendimentos de todos os barbeiros.
  - Permissão para cadastrar, editar serviços e preços.
  - Permissão para adicionar ou remover outros barbeiros da equipe.
- **Barbeiros Subordinados (`role: BARBER`)**:
  - Visualizam apenas seus próprios atendimentos, agendamentos e métricas de faturamento/comissão.

### 2. Serviços e Preços Dinâmicos
- Tabela `services` com 16 serviços mapeados (preços em centavos).
- Suporte a mudança de valor no agendamento através da tabela associativa `appointment_services` com `price_cents_at_booking`, garantindo rastreabilidade histórica e permitindo descontos ou acréscimos sem alterar o catálogo principal.

### 3. Painel Financeiro e Rendimento Diário
- Tabela `financial_records` com divisão de comissão de barbeiro e valor líquido da barbearia.
- Views SQL prontas para o Dashboard:
  - `view_daily_earnings`: Agrupamento diário com valores por forma de pagamento (PIX, Dinheiro, Cartão).
  - `view_monthly_earnings`: Agrupamento mensal e anual para análises históricas.

---

## 🔑 Credenciais Iniciais de Teste (Seeds)

| Usuário | Cargo | Email | Senha Padrão |
| :--- | :--- | :--- | :--- |
| **Carlos Silva** | Barbeiro Master | `master@barberadmin.com` | `admin123` |
| **Lucas Santana** | Barbeiro Staff | `lucas@barberadmin.com` | `admin123` |
| **Matheus Oliveira** | Barbeiro Staff | `matheus@barberadmin.com` | `admin123` |

---

## 🛠️ Comandos Úteis

- **Ver status dos containers**: `docker compose ps`
- **Ver logs do PostgreSQL**: `docker compose logs -f postgres`
- **Parar containers**: `docker compose down`
- **Resetar banco de dados (recriar do zero com seeds)**: `docker compose down -v && docker compose up -d`
