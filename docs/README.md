# Documentação do Androide PY

Esta pasta concentra a documentação de produto, arquitetura, planejamento e decisões técnicas do projeto.

## Documentos principais

- [Visão do projeto](PROJECT_VISION.md): propósito, público, objetivos, escopo e princípios.
- [Arquitetura](ARCHITECTURE.md): visão lógica, componentes, contratos, execução, persistência e segurança.
- [Roadmap](ROADMAP.md): fases de desenvolvimento, entregáveis e critérios de aceite.
- [ADRs](adr/README.md): registro das decisões arquiteturais relevantes.\n- [Checklist operacional P0](operations/P0_EXECUTION_CHECKLIST.md): fases, subetapas, critérios de aceite e evidências.\n- [Instruções para agentes](../AGENTS.md): rotina obrigatória de trabalho e conferência.

## Organização

```text
docs/
├── README.md
├── PROJECT_VISION.md
├── ARCHITECTURE.md
├── ROADMAP.md
└── adr/
    ├── README.md
    ├── 0001-android-native-host.md
    ├── 0002-python-runtime-chaquopy.md
    ├── 0003-local-persistence-room-sqlite.md
    └── 0004-capability-based-android-bridge.md
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
