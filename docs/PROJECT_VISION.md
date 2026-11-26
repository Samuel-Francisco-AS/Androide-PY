# Visão do Projeto

**Projeto:** Androide PY  
**Tipo:** aplicativo Android local de automação programável  
**Status:** P0 e P1 concluídas e integradas em `main`; P2.0 documental e P2.1-A/B/C concluídas no escopo acordado. P2.2-A tem implementação Kotlin/Python publicada e revisada estaticamente na branch dedicada; testes específicos, build e validação física pendentes. P2.2-B/C não iniciadas.

## 1. Resumo

Androide PY é uma plataforma Android local para criação, edição, organização, execução, agendamento e orquestração de automações escritas predominantemente em Python.

O aplicativo oferece um ambiente integrado no qual o usuário desenvolve scripts e os conecta às capacidades implementadas do dispositivo. A evolução planejada inclui abertura de aplicativos, operações com arquivos acessíveis, consulta de informações do sistema, rede, sensores, notificações e interação com interfaces de outros aplicativos, sempre dentro das permissões e restrições impostas pelo Android. Capacidades planejadas não devem ser confundidas com APIs já disponíveis.

A plataforma não pretende ser apenas um editor de código ou um lançador de scripts. Seu objetivo é fornecer um runtime gerenciado, uma biblioteca Python de integração Android e mecanismos para coordenar execuções.

## 2. Problema e oportunidade

Automatizar tarefas no Android frequentemente exige combinar aplicativos diferentes, ferramentas externas, serviços especializados e conhecimento das APIs do sistema. Ferramentas de automação visual podem simplificar tarefas comuns, mas nem sempre oferecem a flexibilidade de uma linguagem de programação geral.

Androide PY explora uma alternativa: oferecer a expressividade de Python em uma central móvel que administra scripts, eventos, agendamentos, fluxos e resultados.

## 3. Proposta de valor

- **Programabilidade:** automações definidas em Python, com uma API de alto nível para recursos Android.
- **Centralização:** scripts, configurações, execuções e resultados organizados em um único aplicativo.
- **Composição:** possibilidade de combinar automações em fluxos reutilizáveis.
- **Integração:** adaptadores para capacidades Android nativas e mecanismos opcionais de acesso avançado.
- **Autonomia local:** funcionamento sem depender de um backend próprio ou serviço em nuvem.
- **Transparência:** permissões, estado, logs e resultados observáveis pelo usuário.

## 4. Público e cenário de uso

O público inicial é o próprio usuário desenvolvedor, que deseja criar e executar automações locais e explorar integração entre Python e Android. O projeto também tem finalidade educacional e de portfólio, demonstrando arquitetura de software, Python, Kotlin, APIs Android, persistência, concorrência e engenharia de integração.

Exemplos de cenários pretendidos:

- Abrir um aplicativo e iniciar uma tarefa.
- Processar arquivos armazenados em locais autorizados.
- Reagir a notificações selecionadas.
- Consultar uma API e produzir um relatório local.
- Interagir com elementos acessíveis da interface de outro aplicativo.
- Executar uma sequência de scripts com dependências e condições.
- Programar rotinas e acompanhar seu histórico.

Esses exemplos descrevem objetivos de produto; cada capacidade dependerá de suporte técnico, permissões e limitações do dispositivo e da versão do Android.

## 5. Objetivos

### 5.1 Objetivos funcionais

1. Criar, importar, editar, organizar e versionar scripts Python.
2. Executar scripts manualmente e acompanhar o estado da execução.
3. Disponibilizar uma biblioteca Python própria para integração com Android.
4. Gerenciar parâmetros, logs, erros e artefatos de execução.
5. Agendar automações e reagir a eventos suportados.
6. Criar fluxos com dependências, condições, repetição e tratamento de falhas.
7. Exibir as capacidades disponíveis e as autorizações necessárias.

### 5.2 Objetivos técnicos

- Manter Python como linguagem principal da lógica de automação.
- Utilizar integração Android nativa onde o sistema exigir ou oferecer APIs específicas.
- Separar interface, runtime, orquestração, adaptadores e persistência.
- Evitar dependência de infraestrutura remota própria.
- Projetar contratos estáveis entre Python e Kotlin.
- Testar comportamento em dispositivo físico desde os primeiros marcos.
- Documentar decisões e limitações de forma verificável.

## 6. Fora do escopo inicial

Não fazem parte do primeiro protótipo:

- Distribuição pela Google Play Store.
- Sincronização em nuvem ou colaboração entre usuários.
- Execução de scripts em dispositivos remotos.
- Compatibilidade irrestrita com todos os pacotes do PyPI.
- Controle garantido de qualquer aplicativo ou tela do Android.
- Execução invisível ou contorno de mecanismos de segurança do sistema.
- Marketplace público de scripts.
- Editor visual completo de fluxos.
- Sandbox universal para executar código Python não confiável.

Esses itens poderão ser reconsiderados em etapas futuras, mediante avaliação técnica e de segurança.

## 7. Princípios de produto

### Python-first

A lógica de scripts, automações, fluxos e APIs de alto nível deve ser escrita em Python. Kotlin será utilizado para interface e integração com o sistema operacional.

### Local-first

Dados e execuções devem permanecer no dispositivo por padrão. Acesso à internet poderá ser utilizado pelas automações quando autorizado.

### Capability-based

Cada integração expõe capacidades específicas. O sistema deve verificar disponibilidade e autorização antes de executar uma operação.

### Modularidade

Novos adaptadores Android não devem exigir alterações no núcleo do motor quando puderem respeitar contratos existentes.

### Observabilidade

Cada execução deve ter estado, logs, duração, entradas, saídas, erros e referências aos arquivos produzidos.

### Segurança por padrão

Código de terceiros será considerado não confiável. A primeira versão será voltada a scripts controlados pelo usuário, sem alegar isolamento forte que ainda não exista.

## 8. Estado dos marcos

- **P0:** concluída e integrada em `main`.
- **P1:** concluída e integrada em `main` por Squash Merge da PR #2 em 25/11/2026. O escopo físico acordado foi validado; P1-TF13 e verificações adicionais permanecem explicitamente diferidas nos documentos operacionais.
- **P2:** P2.1-C concluída no escopo acordado, com testes JVM/Python host, build debug e validação física de autorização/reinicialização normal PASS no Moto G06. Force stop foi caracterizado como revogação executada pelo Android. A P2.2-A tem implementação publicada de `mobile.screen.get_status`, `tap`, `type_text`, `swipe` e `scroll`, com testes específicos adicionados mas ainda não executados no checkout; build e validação física pendentes. O catálogo continua sem anunciar `screen.*`. P2.2-B/C (overlay e mapas) permanecem não iniciadas.

A P2 adota três prioridades funcionais: A — interação geométrica e Mapa de Coordenadas; B — consulta/coleta sob demanda; C — leitura e identificação semântica de elementos. A fundação nativa e autorização precedem essas entregas. Screenshots, OCR e notificações não integram a P2 inicial.

## 9. Indicadores de sucesso

O primeiro sucesso verificável é o P0: um APK executando Python, exibindo saída e erros, consultando informações do dispositivo e abrindo outro aplicativo por uma API própria.

A evolução do produto será avaliada por critérios objetivos em cada marco do [roadmap](ROADMAP.md), incluindo testes automatizados e validação física.
