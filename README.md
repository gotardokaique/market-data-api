# 📈 Market Data API

API REST para consulta de dados de mercado financeiro em tempo real, com integração a múltiplos provedores de dados (CoinGecko, Brapi e Alpha Vantage).

Construída com **Java 17**, **Spring Boot 3.5** e arquitetura limpa baseada no **Strategy Pattern** — permitindo adicionar novos provedores sem alterar uma linha do código existente.

---

## 📋 Índice

- [✨ Funcionalidades](#-funcionalidades)
- [🏗️ Arquitetura](#️-arquitetura)
- [🔌 Provedores de Dados](#-provedores-de-dados)
- [🚀 Como Executar](#-como-executar)
- [🔑 Configuração de API Keys](#-configuração-de-api-keys)
- [📡 Endpoints](#-endpoints)
- [📊 Exemplos de Uso](#-exemplos-de-uso)
- [📏 Métricas e Monitoramento](#-métricas-e-monitoramento)
- [⚙️ Configurações](#️-configurações)
- [🧱 Estrutura do Projeto](#-estrutura-do-projeto)
- [🛡️ Tratamento de Erros](#️-tratamento-de-erros)
- [🧩 Design Patterns](#-design-patterns)
- [🛣️ Roadmap](#️-roadmap)
- [📄 Licença](#-licença)

---

## ✨ Funcionalidades

| Feature | Descrição |
|---------|-----------|
| 🪙 **Criptomoedas** | Preços em tempo real via CoinGecko (Bitcoin, Ethereum, etc.) |
| 🇧🇷 **Ações da B3** | Cotações de ações brasileiras via Brapi (PETR4, VALE3, ITUB4) |
| 🏢 **Fundos Imobiliários** | Cotações de FIIs via Brapi (HGLG11, MXRF11, XPML11) |
| 🌍 **Ações Globais** | Cotações internacionais via Alpha Vantage (IBM, AAPL, MSFT, TSLA) |
| 🔄 **Fallback Automático** | Se o provider primário falhar, tenta o próximo automaticamente |
| 📊 **Métricas de Latência** | Monitoramento por provider via Spring Boot Actuator + Micrometer |
| ⏱️ **Timeouts Estritos** | Chamadas HTTP com timeout configurável (padrão: 2s) |
| 🛡️ **Tratamento de Erros** | Respostas padronizadas com handler global de exceções |

---

## 🏗️ Arquitetura

A aplicação segue uma **arquitetura em camadas** com separação clara de responsabilidades:

```
┌─────────────────────────────────────────────────────────────┐
│                   📱 Presentation Layer                     │
│              MarketDataController (REST API)                │
│              GlobalExceptionHandler (Erros)                 │
├─────────────────────────────────────────────────────────────┤
│                   ⚙️ Application Layer                      │
│          MarketDataService (Orquestrador + Fallback)        │
│              ProviderMetrics (Métricas)                     │
├─────────────────────────────────────────────────────────────┤
│                   🔧 Infrastructure Layer                   │
│   ┌──────────────┐  ┌────────────────┐  ┌───────────────┐  │
│   │  CoinGecko   │  │     Brapi      │  │ Alpha Vantage │  │
│   │  Provider    │  │    Provider    │  │   Provider    │  │
│   │   (CRYPTO)   │  │ (STOCK + FII)  │  │   (STOCK)     │  │
│   └──────────────┘  └────────────────┘  └───────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    🏛️ Domain Layer                          │
│        MarketData │ Candle │ MarketType │ ProviderType      │
│                   ProviderException                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔌 Provedores de Dados

### Ordem de Prioridade (Strategy + Fallback)

| Prioridade | Provider | Mercados | Cobertura | Token |
|:----------:|----------|----------|-----------|:-----:|
| 🥇 `@Order(1)` | **Brapi** | `STOCK`, `FII` | 🇧🇷 B3 — Ações e FIIs brasileiros | ✅ Obrigatório |
| 🥈 `@Order(2)` | **Alpha Vantage** | `STOCK` | 🌍 Ações globais (NYSE, NASDAQ, LSE) | ✅ Obrigatório |
| — | **CoinGecko** | `CRYPTO` | 🪙 Criptomoedas (3000+ coins) | ❌ Não precisa |

### Fluxo de Decisão

```
Requisição: GET /market/STOCK/PETR4.SA

    ┌──────────────────────┐
    │  MarketDataService   │
    │   (Orquestrador)     │
    └──────────┬───────────┘
               │
               ▼
    ┌──────────────────────┐
    │  1️⃣ BrapiProvider     │ ← @Order(1) - Prioridade
    │     Suporta STOCK?   │
    │     ✅ SIM → Tenta   │
    └──────────┬───────────┘
               │
          ✅ Sucesso? ──→ Retorna MarketData
               │
          ❌ Falhou?
               │
               ▼
    ┌──────────────────────┐
    │  2️⃣ AlphaVantage      │ ← @Order(2) - Fallback
    │     Suporta STOCK?   │
    │     ✅ SIM → Tenta   │
    └──────────┬───────────┘
               │
          ✅ Sucesso? ──→ Retorna MarketData
               │
          ❌ Falhou? ──→ Lança ProviderException
```

### Detalhes dos Provedores

#### 🟢 CoinGecko (Criptomoedas)

| Item | Detalhe |
|------|---------|
| **API** | `https://api.coingecko.com/api/v3/coins/{id}` |
| **Documentação** | [CoinGecko API Docs](https://www.coingecko.com/en/api/documentation) |
| **Autenticação** | Sem token (free tier público) |
| **Rate Limit** | ~30 requests/minuto |
| **Símbolos** | ID do CoinGecko: `bitcoin`, `ethereum`, `solana`, `cardano` |

#### 🟡 Brapi (B3 — Ações e FIIs Brasileiros)

| Item | Detalhe |
|------|---------|
| **API** | `https://brapi.dev/api/quote/{symbol}?token={token}` |
| **Documentação** | [Brapi Docs](https://brapi.dev/docs) |
| **Autenticação** | Token gratuito obrigatório |
| **Rate Limit** | Depende do plano |
| **Símbolos** | Ticker da B3: `PETR4`, `VALE3`, `HGLG11`, `MXRF11` |
| **Observação** | Aceita ticker com ou sem `.SA` — o provider remove automaticamente |

#### 🔵 Alpha Vantage (Ações Globais)

| Item | Detalhe |
|------|---------|
| **API** | `https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={symbol}&apikey={key}` |
| **Documentação** | [Alpha Vantage Docs](https://www.alphavantage.co/documentation/) |
| **Autenticação** | API Key gratuita obrigatória |
| **Rate Limit** | ⚠️ **25 requests/dia**, 5 requests/minuto (free tier) |
| **Símbolos** | Ticker global: `IBM`, `AAPL`, `MSFT`, `TSLA` |
| **B3** | Usa sufixo `.SAO`: `PETR4.SAO` (convertido automaticamente de `.SA`) |

---

## 🚀 Como Executar

### Pré-requisitos

- ☕ **Java 17+** (compatível com Java 21)
- 📦 **Maven 3.8+** (ou use o Maven Wrapper incluso)

### 1️⃣ Clone o repositório

```bash
git clone https://github.com/gotardokaique/market-data-api.git
cd market-data-api
```

### 2️⃣ Configure as API Keys

Edite o arquivo `src/main/resources/application.properties`:

```properties
# Obtenha em: https://brapi.dev/dashboard
brapi.token=SEU_TOKEN_DA_BRAPI

# Obtenha em: https://www.alphavantage.co/support/#api-key
alphavantage.apikey=SUA_API_KEY_ALPHA_VANTAGE
```

### 3️⃣ Execute a aplicação

```bash
./mvnw spring-boot:run
```

O servidor inicia na porta **8080** por padrão.

### 4️⃣ Teste

```bash
curl http://localhost:8080/market/CRYPTO/bitcoin
```

---

## 🔑 Configuração de API Keys

### Como obter os tokens (gratuitos)

| Provider | Onde obter | Tempo |
|----------|-----------|-------|
| **Brapi** | [brapi.dev/dashboard](https://brapi.dev/dashboard) | ~30 segundos |
| **Alpha Vantage** | [alphavantage.co/support/#api-key](https://www.alphavantage.co/support/#api-key) | ~20 segundos |
| **CoinGecko** | Não precisa de token | — |

### ⚠️ Segurança

As API Keys ficam no `application.properties` e **NÃO devem ser commitadas no Git**.

Recomendações:
- Adicione `application.properties` ao `.gitignore` (ou use um `application-local.properties`)
- Em produção, use **variáveis de ambiente**:

```bash
export BRAPI_TOKEN=seu_token_aqui
export ALPHAVANTAGE_APIKEY=sua_key_aqui
```

```properties
brapi.token=${BRAPI_TOKEN}
alphavantage.apikey=${ALPHAVANTAGE_APIKEY}
```

---

## 📡 Endpoints

### Dados de Mercado

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/market/{type}/{symbol}` | Busca o preço atual de um ativo |

**Path Parameters:**

| Parâmetro | Tipo | Valores aceitos | Exemplo |
|-----------|------|-----------------|---------|
| `type` | `String` | `CRYPTO`, `STOCK`, `FII` | `STOCK` |
| `symbol` | `String` | Ticker ou ID do ativo | `PETR4.SA` |

### Monitoramento (Actuator)

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `GET` | `/actuator/health` | Status da aplicação |
| `GET` | `/actuator/metrics` | Lista de métricas disponíveis |
| `GET` | `/actuator/metrics/market.provider.latency` | Latência por provider |
| `GET` | `/actuator/metrics/market.provider.errors` | Contagem de erros por provider |

---

## 📊 Exemplos de Uso

### 🪙 Criptomoedas (CoinGecko)

```bash
# Bitcoin
curl http://localhost:8080/market/CRYPTO/bitcoin

# Ethereum
curl http://localhost:8080/market/CRYPTO/ethereum
```

**Resposta:**
```json
{
  "symbol": "BTC",
  "name": "Bitcoin",
  "currentPrice": 66454.00,
  "currency": "USD",
  "changePercent24h": -2.44,
  "marketCap": 1328060882140,
  "volume24h": 36856022338,
  "marketType": "CRYPTO",
  "providerType": "COINGECKO",
  "timestamp": "2026-02-19T15:45:47.579Z"
}
```

### 🇧🇷 Ações Brasileiras (Brapi)

```bash
# Petrobras
curl http://localhost:8080/market/STOCK/PETR4.SA

# Vale
curl http://localhost:8080/market/STOCK/VALE3.SA
```

**Resposta:**
```json
{
  "symbol": "PETR4",
  "name": "PETROBRAS PN N2",
  "currentPrice": 38.15,
  "currency": "BRL",
  "changePercent24h": 0.926,
  "marketCap": 500000000000,
  "volume24h": 42000000,
  "marketType": "STOCK",
  "providerType": "BRAPI",
  "timestamp": "2026-02-19T16:00:00.000Z"
}
```

### 🏢 Fundos Imobiliários (Brapi)

```bash
# CSHG Logística FII
curl http://localhost:8080/market/FII/HGLG11.SA

# Maxi Renda FII
curl http://localhost:8080/market/FII/MXRF11.SA
```

### 🌍 Ações Globais (Alpha Vantage)

```bash
# IBM
curl http://localhost:8080/market/STOCK/IBM

# Apple
curl http://localhost:8080/market/STOCK/AAPL
```

**Resposta:**
```json
{
  "symbol": "IBM",
  "name": "IBM",
  "currentPrice": 260.79,
  "currency": "USD",
  "changePercent24h": 0.9601,
  "marketCap": 0,
  "volume24h": 3949229,
  "marketType": "STOCK",
  "providerType": "ALPHA_VANTAGE",
  "timestamp": "2026-02-19T16:40:05.209Z"
}
```

---

## 📏 Métricas e Monitoramento

A API expõe métricas via **Spring Boot Actuator + Micrometer**, permitindo monitorar a performance de cada provider individualmente.

### Métricas Disponíveis

| Métrica | Tipo | Descrição |
|---------|------|-----------|
| `market.provider.latency` | Timer | Tempo de resposta de cada provider |
| `market.provider.errors` | Counter | Contagem de erros por provider |

### Exemplos de Consulta

```bash
# Latência geral de todos os providers
curl http://localhost:8080/actuator/metrics/market.provider.latency

# Filtrar por provider específico
curl "http://localhost:8080/actuator/metrics/market.provider.latency?tag=provider:CoinGeckoProvider"
curl "http://localhost:8080/actuator/metrics/market.provider.latency?tag=provider:BrapiProvider"
curl "http://localhost:8080/actuator/metrics/market.provider.latency?tag=provider:AlphaVantageProvider"

# Filtrar por status (sucesso ou erro)
curl "http://localhost:8080/actuator/metrics/market.provider.latency?tag=status:success"
curl "http://localhost:8080/actuator/metrics/market.provider.latency?tag=status:error"

# Contagem de erros por provider
curl http://localhost:8080/actuator/metrics/market.provider.errors
```

### Resposta de Métricas

```json
{
  "name": "market.provider.latency",
  "description": "Latência das chamadas aos providers de dados de mercado",
  "baseUnit": "seconds",
  "measurements": [
    { "statistic": "COUNT", "value": 5.0 },
    { "statistic": "TOTAL_TIME", "value": 3.842 },
    { "statistic": "MAX", "value": 1.205 }
  ],
  "availableTags": [
    { "tag": "provider", "values": ["CoinGeckoProvider", "BrapiProvider"] },
    { "tag": "symbol", "values": ["bitcoin", "PETR4.SA"] },
    { "tag": "status", "values": ["success"] }
  ]
}
```

---

## ⚙️ Configurações

Todas as configurações ficam em `src/main/resources/application.properties`:

```properties
# ===== Tokens dos Provedores =====
brapi.token=SEU_TOKEN
alphavantage.apikey=SUA_KEY

# ===== Timeouts HTTP =====
provider.timeout.connect-ms=2000   # Timeout para conexão TCP (ms)
provider.timeout.read-ms=2000      # Timeout para leitura da resposta (ms)

# ===== Actuator =====
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
```

### ⏱️ Timeouts

Os timeouts são **estritos por design**. Se um provider externo demorar mais que o limite configurado, a requisição é cancelada automaticamente para não travar as threads do servidor:

| Timeout | Padrão | Função |
|---------|--------|--------|
| `connect-ms` | `2000ms` | Tempo máximo para estabelecer conexão TCP |
| `read-ms` | `2000ms` | Tempo máximo para receber a resposta |

---

## 🧱 Estrutura do Projeto

```
src/main/java/com/kaique/marketdata/
│
├── 📄 MarketDataApiApplication.java          # Entry point
│
├── 📂 domain/                                # 🏛️ Camada de Domínio
│   ├── enums/
│   │   ├── MarketType.java                   # CRYPTO, STOCK, FII
│   │   └── ProviderType.java                 # COINGECKO, BRAPI, ALPHA_VANTAGE
│   ├── exception/
│   │   └── ProviderException.java            # Exceção customizada
│   └── model/
│       ├── MarketData.java                   # Record (contrato principal)
│       └── Candle.java                       # Record (OHLCV)
│
├── 📂 application/                           # ⚙️ Camada de Aplicação
│   └── service/
│       └── MarketDataService.java            # Orquestrador (Strategy + Fallback)
│
├── 📂 infrastructure/                        # 🔧 Camada de Infraestrutura
│   ├── config/
│   │   └── RestTemplateConfig.java           # HTTP client com timeouts
│   ├── metrics/
│   │   └── ProviderMetrics.java              # Métricas Micrometer por provider
│   └── provider/
│       ├── MarketDataProvider.java            # Interface (Strategy Pattern)
│       ├── brapi/
│       │   ├── BrapiProvider.java             # @Order(1) — B3
│       │   └── BrapiResponse.java            # DTO (Record)
│       ├── alphavantage/
│       │   ├── AlphaVantageProvider.java      # @Order(2) — Global
│       │   └── AlphaVantageResponse.java     # DTO (Record)
│       └── coingecko/
│           ├── CoinGeckoProvider.java         # Crypto
│           └── CoinGeckoDTO.java             # DTO
│
└── 📂 presentation/                          # 📱 Camada de Apresentação
    ├── controller/
    │   └── MarketDataController.java          # REST endpoint
    └── handler/
        └── GlobalExceptionHandler.java        # Tratamento global de erros
```

---

## 🛡️ Tratamento de Erros

A API retorna respostas padronizadas para todos os tipos de erro:

### Provider indisponível (`502 Bad Gateway`)

```json
{
  "error": "Falha ao consultar provedor de dados",
  "provider": "BrapiProvider",
  "message": "[Brapi] Erro do cliente ao buscar PETR4: 401 UNAUTHORIZED",
  "timestamp": "2026-02-19T16:00:00.000Z"
}
```

### Tipo de mercado inválido (`400 Bad Request`)

```json
{
  "error": "Parâmetro inválido",
  "message": "No enum constant com.kaique.marketdata.domain.enums.MarketType.FOREX",
  "timestamp": "2026-02-19T16:00:00.000Z"
}
```

### Tipo não suportado (`400 Bad Request`)

```json
{
  "error": "Tipo de mercado não suportado",
  "message": "Nenhum provider disponível para o tipo de mercado: FOREX",
  "timestamp": "2026-02-19T16:00:00.000Z"
}
```

### Erro interno (`500 Internal Server Error`)

```json
{
  "error": "Erro interno do servidor",
  "message": "Ocorreu um erro inesperado. Tente novamente.",
  "timestamp": "2026-02-19T16:00:00.000Z"
}
```

---

## 🧩 Design Patterns

### Strategy Pattern

A interface `MarketDataProvider` define o contrato que todo provider deve implementar. O `MarketDataService` **nunca conhece** as implementações concretas — ele só conhece a interface:

```java
public interface MarketDataProvider {
    MarketData fetchCurrentPrice(String symbol);
    boolean supports(MarketType marketType);
}
```

**Benefício:** Para adicionar um novo provider (ex: Binance), basta:
1. Criar uma classe que implemente `MarketDataProvider`
2. Anotá-la com `@Component` e `@Order(n)`
3. Pronto — o Spring injeta automaticamente no service

### Records (Java 17+)

Os modelos de domínio (`MarketData`, `Candle`) e DTOs (`BrapiResponse`, `AlphaVantageResponse`) usam **Java Records** para:
- ✅ Imutabilidade garantida
- ✅ Zero boilerplate (sem getters/setters/equals/hashCode)
- ✅ Código limpo e expressivo

### Fallback Chain

O `MarketDataService` itera por todos os providers que suportam o `MarketType` solicitado, em ordem de `@Order`. Se o primeiro falhar com `ProviderException`, tenta o próximo:

```
Brapi falhou? → Tenta Alpha Vantage → Todos falharam? → Lança exceção
```

---

## 🛠️ Tech Stack

| Tecnologia | Versão | Função |
|------------|--------|--------|
| Java | 17+ | Linguagem |
| Spring Boot | 3.5.11 | Framework |
| Spring Web | — | REST API |
| Spring Boot Actuator | — | Métricas e monitoramento |
| Micrometer | — | Instrumentação de métricas |
| RestTemplate | — | Cliente HTTP |
| Maven | — | Build e dependências |

---

## 🛣️ Roadmap

- [ ] 🧪 Testes unitários e de integração
- [ ] 💾 Cache com Spring Cache (Redis/Caffeine)
- [ ] 🔐 Autenticação via API Key própria
- [ ] 📈 Endpoint de histórico (candles/OHLCV)
- [ ] 🐳 Dockerfile e Docker Compose
- [ ] 📖 Documentação Swagger/OpenAPI
- [ ] 🔔 Webhooks para alertas de preço

---

## 📄 Licença

Este projeto é open source e está disponível sob a [MIT License](LICENSE).

---

<div align="center">

Feito com ☕ e muita vontade de aprender

**[⬆ Voltar ao topo](#-market-data-api)**

</div>
