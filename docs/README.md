# Documentação do Androide PY

Esta pasta concentra a documentação de produto, arquitetura, planejamento e decisões técnicas do projeto.

## Documentos principais

- [Visão do projeto](PROJECT_VISION.md): propósito, público, objetivos, escopo e princípios.
- [Arquitetura](ARCHITECTURE.md): visão lógica, componentes, contratos, execução, persistência e segurança.
- [Roadmap](ROADMAP.md): fases de desenvolvimento, entregáveis e critérios de aceite.
- [ADRs](adr/README.md): registro das decisões arquiteturais relevantes.
- [Checklist operacional P0](operations/P0_EXECUTION_CHECKLIST.md): fases, subetapas, critérios de aceite e evidências.
- [Smoke check do runtime Python](operations/P0_RUNTIME_SMOKE.md): contrato inicial e validação manual sem ADB.
- [Contrato de execução P0](operations/P0_EXECUTION_CONTRACT.md): estados, payload JSON e limites do executor.
- [Contrato da Android Bridge P0](operations/P0_ANDROID_BRIDGE_CONTRACT.md): APIs Python, dados, erros e package visibility.
- [Persistência P0](operations/P0_PERSISTENCE.md): Room, script inicial, gravação e tratamento de falhas.
- [Plano de testes de integração P0](operations/P0_INTEGRATION_TEST_PLAN.md): comandos sem ADB e roteiro físico no Moto G06.
- [Build e testes no Fedora](operations/P0_BUILD_AND_TEST.md): sincronização segura da branch, testes e geração do APK sem ADB.
- [Relatório da implementação P0](operations/P0_IMPLEMENTATION_REPORT.md): escopo entregue, evidências e limites.
- [Checklist operacional P1](operations/P1_EXECUTION_CHECKLIST.md): subetapas, critérios de aceite, estado e evidências.
- [Relatório da implementação P1](operations/P1_IMPLEMENTATION_REPORT.md): decisões, alterações, testes, riscos e validação.
- [Checklist operacional P2](operations/P2_EXECUTION_CHECKLIST.md): prioridades A/B/C, decomposição, critérios de aceite, estados e evidências da automação de interfaces Android.
- [Relatório da implementação P2](operations/P2_IMPLEMENTATION_REPORT.md): decisões acordadas, arquitetura pretendida, riscos, validações e estado da fase.
- [Protocolo assíncrono P2](operations/P2_ASYNC_PROTOCOL.md): envelopes JSON, correlação, timeout, cancelamento lógico, lifecycle, filas e fronteiras de thread.
- [Instruções para agentes](../AGENTS.md): rotina obrigatória de trabalho e conferência.

## Organização

```text
docs/
├── README.md
├── PROJECT_VISION.md
├── ARCHITECTURE.md
├── ROADMAP.md
├── operations/
│   ├── P0_EXECUTION_CHECKLIST.md
│   ├── P0_RUNTIME_SMOKE.md
│   ├── P0_EXECUTION_CONTRACT.md
│   ├── P0_ANDROID_BRIDGE_CONTRACT.md
│   ├── P0_PERSISTENCE.md
│   ├── P1_EXECUTION_CHECKLIST.md
│   ├── P1_IMPLEMENTATION_REPORT.md
│   ├── P2_EXECUTION_CHECKLIST.md
│   ├── P2_IMPLEMENTATION_REPORT.md
│   └── P2_ASYNC_PROTOCOL.md
└── adr/
    ├── README.md
    ├── 0001-android-native-host.md
    ├── 0002-python-runtime-chaquopy.md
    ├── 0003-local-persistence-room-sqlite.md
    ├── 0004-capability-based-android-bridge.md
    └── 0005-p0-toolchain-baseline.md
```

## Convenções documentais

- Documentação normativa deve descrever o comportamento pretendido, não funcionalidades ainda inexistentes como se estivessem prontas.
- Decisões importantes devem ser registradas em ADRs.
- Cada ADR deve apresentar contexto, decisão, consequências e estado.
- Mudanças relevantes no escopo ou na arquitetura devem atualizar os documentos afetados.
- Critérios de aceite devem ser verificáveis, preferencialmente por testes reproduzíveis em dispositivo físico.

## Estados usados

- **Proposto:** ainda precisa de validação ou aprovação.
- **Aceito:** decisão adotada para orientar o desenvolvimento.
- **Validado:** comportamento confirmado por implementação e testes.
- **Substituído:** decisão posteriormente substituída por outra.
