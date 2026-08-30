# Docfy Frontend

Frontend oficial do Docfy, integrado à API Spring Boot do projeto.

## Stack

- React
- TypeScript
- Vite
- Tailwind CSS
- React Router
- Vitest
- React Testing Library
- MSW

## Architecture

```text
React
  ↓ relative /api requests
Vite development proxy
  ↓
Spring Boot
  ↓
PostgreSQL
```

O backend é a fonte de verdade para dados, autenticação e autorização. O frontend usa autenticação JWT real e interpreta claims somente para melhorar a experiência; todas as decisões de segurança continuam no backend. O MSW é utilizado exclusivamente nos testes automatizados. Não existe banco, seed ou backend simulado no runtime.

## Requirements

- Node.js 24 LTS
- npm
- backend do Docfy em execução para uso local

## Install

```bash
npm ci
```

## Run

```bash
npm run dev
```

Em desenvolvimento, as requisições relativas para `/api` são encaminhadas pelo proxy do Vite ao backend local. Inicie PostgreSQL e Spring Boot conforme as instruções do repositório antes de testar os fluxos reais. A massa local opcional está descrita em [`docs/development/development-seed.md`](../docs/development/development-seed.md).

## Validation

```bash
npm run lint
npm test
npm run build
```
