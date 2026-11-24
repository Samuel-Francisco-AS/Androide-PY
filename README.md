# Androide PY

**Uma plataforma local de automação Android programável em Python.**

Androide PY é um projeto experimental de aplicativo Android que permite criar, editar, organizar, executar e orquestrar automações Python capazes de interagir com recursos do dispositivo e, mediante autorizações apropriadas, com interfaces de outros aplicativos.

O projeto adota uma arquitetura *Python-first* com integração Android nativa: Python concentra a lógica de automação e orquestração; Kotlin fornece a integração com o sistema operacional, os serviços Android e a interface do aplicativo.

> **Status:** planejamento e validação arquitetural. O primeiro marco de implementação é o protótipo P0, descrito em [ROADMAP.md](docs/ROADMAP.md).

## Objetivos

- Oferecer um ambiente local para criação e gerenciamento de scripts Python.
- Executar automações sob demanda, por agendamento ou em resposta a eventos.
- Disponibilizar uma biblioteca Python própria para recursos Android.
- Compor scripts em fluxos com dependências, condições e tratamento de falhas.
- Registrar execuções, logs e arquivos produzidos.
- Evoluir por módulos, mantendo permissões e capacidades explícitas.

## Arquitetura em poucas palavras

- **Android:** Kotlin e Jetpack Compose.
- **Runtime Python:** Chaquopy como candidato inicial, sujeito à validação no P0.
- **Persistência:** Room sobre SQLite, com acesso mediado por uma camada de repositório.
- **Integração:** Android Bridge com adaptadores nativos e capacidades opcionais, como Accessibility e Shizuku.
- **Execução:** gerenciador de execuções, workers e mecanismos Android apropriados para tarefas em segundo plano.

A arquitetura completa, suas fronteiras e suas limitações estão documentadas em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Documentação

| Documento | Conteúdo |
|---|---|
| [Visão do projeto](docs/PROJECT_VISION.md) | Problema, proposta, objetivos, escopo e princípios |
| [Arquitetura](docs/ARCHITECTURE.md) | Componentes, responsabilidades, comunicação e segurança |
| [Roadmap](docs/ROADMAP.md) | Marcos, entregas e critérios de validação |
| [Decisões arquiteturais](docs/adr/README.md) | Índice dos Architecture Decision Records |

## Estado atual

O repositório está na fase de documentação e preparação do protótipo. As tecnologias indicadas são decisões iniciais de arquitetura, não evidência de que a integração já foi implementada ou validada em dispositivo físico.

O primeiro protótipo deverá demonstrar, no mínimo:

1. Inicialização do runtime Python dentro do aplicativo.
2. Execução de um script a partir da interface.
3. Captura de saída, erros e exceções.
4. Consulta de informações básicas do dispositivo por uma API Python própria.
5. Abertura de um aplicativo instalado por meio de uma API Android autorizada.
6. Persistência do script entre sessões.

## Plataforma de desenvolvimento

O alvo inicial de validação física é um dispositivo Android real. O desenvolvimento será realizado com ferramentas Android/Gradle e código Python, mantendo testes automatizados e documentação das decisões técnicas.

## Segurança e limitações

Automações executam código e podem acessar recursos concedidos ao aplicativo. Scripts devem ser tratados como código confiável do usuário nesta primeira etapa. Acessibilidade, notificações, armazenamento, alarmes, execução em segundo plano, Shizuku e root possuem requisitos e limitações próprios. Nenhum mecanismo concede controle irrestrito sobre o Android.

Consulte a seção de segurança em [ARCHITECTURE.md](docs/ARCHITECTURE.md) antes de implementar novos adaptadores.

## Licença

A licença do projeto ainda não foi definida.
