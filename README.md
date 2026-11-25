# Androide PY

**Uma plataforma local de automação Android programável em Python.**

Androide PY é um projeto experimental de aplicativo Android que permite criar, editar, organizar, executar e orquestrar automações Python capazes de interagir com recursos do dispositivo e, mediante autorizações apropriadas, com interfaces de outros aplicativos.

O projeto adota uma arquitetura *Python-first* com integração Android nativa: Python concentra a lógica de automação e orquestração; Kotlin fornece a integração com o sistema operacional, os serviços Android e a interface do aplicativo.

> **Status:** implementação P0 publicada na branch `feat/p0-runtime-bridge`. A fundação Android, runtime Python, executor, Bridge, persistência e primeira interface estão no repositório. Build, testes locais e validação física ainda estão pendentes; código publicado não é código validado.

## Objetivos

- Oferecer um ambiente local para criação e gerenciamento de scripts Python.
- Executar automações sob demanda, por agendamento ou em resposta a eventos.
- Disponibilizar uma biblioteca Python própria para recursos Android.
- Compor scripts em fluxos com dependências, condições e tratamento de falhas.
- Registrar execuções, logs e arquivos produzidos.
- Evoluir por módulos, mantendo permissões e capacidades explícitas.

## Arquitetura em poucas palavras

- **Android:** Kotlin e Jetpack Compose.
- **Runtime Python:** Chaquopy 17.2.0 configurado para CPython 3.13; a inicialização real ainda depende do primeiro build e teste no aparelho.
- **Persistência:** Room 2.8.5 sobre SQLite, com acesso mediado por `ScriptRepository`; nenhum acesso Python direto ao banco.
- **Integração:** Android Bridge com `mobile.device.get_info()` e `mobile.apps.launch(package_name)` implementados; Accessibility e Shizuku permanecem fora do escopo P0.
- **Execução:** gerenciador de execuções, workers e mecanismos Android apropriados para tarefas em segundo plano.

A arquitetura completa, suas fronteiras e suas limitações estão documentadas em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Documentação

| Documento | Conteúdo |
|---|---|
| [Visão do projeto](docs/PROJECT_VISION.md) | Problema, proposta, objetivos, escopo e princípios |
| [Arquitetura](docs/ARCHITECTURE.md) | Componentes, responsabilidades, comunicação e segurança |
| [Roadmap](docs/ROADMAP.md) | Marcos, entregas e critérios de validação |
| [Decisões arquiteturais](docs/adr/README.md) | Índice dos Architecture Decision Records |
| [Checklist operacional P0](docs/operations/P0_EXECUTION_CHECKLIST.md) | Fases, subetapas e evidências |
| [Build e testes](docs/operations/P0_BUILD_AND_TEST.md) | Sincronização e build sem ADB |
| [Plano de integração](docs/operations/P0_INTEGRATION_TEST_PLAN.md) | Testes host/JVM e roteiro físico |

## Estado atual

O repositório já contém a primeira implementação funcional planejada para P0: editor Compose, script default persistido, execução serial Python, captura de stdout/stderr/exceções e duas capacidades Android. Ainda não existe evidência de build ou execução real. A matriz de validação e o estado detalhado estão em [P0_EXECUTION_CHECKLIST.md](docs/operations/P0_EXECUTION_CHECKLIST.md).

O primeiro protótipo deverá demonstrar, no mínimo:

1. Inicialização do runtime Python dentro do aplicativo.
2. Execução de um script a partir da interface.
3. Captura de saída, erros e exceções.
4. Consulta de informações básicas do dispositivo por uma API Python própria.
5. Abertura de um aplicativo instalado por meio de uma API Android autorizada.
6. Persistência do script entre sessões.

## Build e testes

O projeto usa Gradle Wrapper 8.14.3 e Python host 3.13. Não é necessário instalar Gradle globalmente nem usar ADB.

Consulte [Build e testes no Fedora](docs/operations/P0_BUILD_AND_TEST.md) para sincronizar a branch, executar os testes e gerar `app-debug.apk`. O APK será transferido ao Moto G06 pelo fluxo manual já utilizado no My-CT.

## Segurança e limitações

Automações executam código e podem acessar recursos concedidos ao aplicativo. Scripts devem ser tratados como código confiável do usuário nesta primeira etapa. Acessibilidade, notificações, armazenamento, alarmes, execução em segundo plano, Shizuku e root possuem requisitos e limitações próprios. Nenhum mecanismo concede controle irrestrito sobre o Android.

Consulte a seção de segurança em [ARCHITECTURE.md](docs/ARCHITECTURE.md) antes de implementar novos adaptadores.

## Licença

A licença do projeto ainda não foi definida.
