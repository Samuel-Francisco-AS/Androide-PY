# Architecture Decision Records (ADRs)

Este diretório registra decisões arquiteturais relevantes do Androide PY.

ADRs preservam o contexto de uma escolha, alternativas consideradas, consequências e evidências que possam levar a uma revisão.

## Índice

| ID | Decisão | Estado |
|---|---|---|
| [ADR-0001](0001-android-native-host.md) | Aplicativo hospedeiro Android nativo com Kotlin e Compose | Aceito |
| [ADR-0002](0002-python-runtime-chaquopy.md) | Chaquopy como runtime Python inicial | Validado na P0; pacotes adicionais exigem validação |
| [ADR-0003](0003-local-persistence-room-sqlite.md) | Room sobre SQLite como persistência local | Aceito |
| [ADR-0004](0004-capability-based-android-bridge.md) | Android Bridge baseada em capacidades e adaptadores | Aceito |
| [ADR-0005](0005-p0-toolchain-baseline.md) | Baseline de ferramentas e dependências da P0 | Validado |
| [ADR-0006](0006-script-library-versioning-and-execution-history.md) | Versionamento, parâmetros e histórico P1 | Aceito; validar migração e build P1 |

## Modelo

Cada ADR deve conter:
- **Status:** Proposto, Aceito, Validado ou Substituído.
- **Contexto:** problema e forças que influenciam a decisão.
- **Decisão:** escolha adotada.
- **Alternativas:** opções relevantes consideradas.
- **Consequências:** benefícios, custos, riscos e limitações.
- **Validação:** evidências necessárias ou obtidas.

## Quando criar ou atualizar

Crie um ADR quando uma decisão:
- afeta a arquitetura ou uma fronteira entre componentes;
- introduz dependência estrutural relevante;
- possui consequências difíceis de reverter;
- define um contrato público ou modelo de segurança;
- substitui uma decisão registrada.

Não use ADR para registrar cada detalhe de implementação.
