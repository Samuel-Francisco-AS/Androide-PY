# Arquitetura do Sistema

**Projeto:** Androide PY  
**Status:** P0 e P1 concluídas e integradas em `main`; contratos e decisões da P2 aceitos; implementação inicial da P2.1-A publicada na branch, ainda sem build/testes.

## 1. Visão geral

Androide PY será um aplicativo Android nativo que incorpora um runtime Python. A arquitetura divide responsabilidades entre a aplicação hospedeira Android, o motor de automação Python, a ponte de integração Android, os serviços de execução e a persistência local.

Princípio central: Python implementa a lógica programável e a orquestração; Kotlin integra a aplicação às APIs e ao ciclo de vida do Android.

```text
┌─────────────────────────────────────────────────────┐
│                 ANDROID APPLICATION                 │
│             Kotlin · Jetpack Compose                │
│ Dashboard · Editor · Workflows · Permissions         │
└──────────────────────────┬──────────────────────────┘
                           │ contratos internos
┌──────────────────────────▼──────────────────────────┐
│                 ANDROID APPLICATION CORE            │
│ Runtime Manager · Execution Manager · Event Manager │
│ Permission/Capability Manager · Lifecycle           │
└──────────────────────────┬──────────────────────────┘
                           │ bridge tipada
┌──────────────────────────▼──────────────────────────┐
│                 PYTHON AUTOMATION ENGINE            │
│ Script Runtime · Workflow Engine · Scheduler        │
│ Event Dispatcher · Logging · Artifact Manager       │
└──────────────────────────┬──────────────────────────┘
                           │ adapters / API Python
┌──────────────────────────▼──────────────────────────┐
│                    ANDROID BRIDGE                   │
│ Apps · Screen · Notifications · Files · System      │
│ Sensors · Network · Optional Shizuku/ADB adapters   │
└──────────────────────────┬──────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────┐
│                 LOCAL PERSISTENCE                   │
│ Room/SQLite · Script files · Logs · Artifacts       │
└─────────────────────────────────────────────────────┘
```

O diagrama representa responsabilidades lógicas, não exige que cada caixa seja um processo ou módulo Gradle separado. A separação física será decidida conforme a implementação evoluir.

## 2. Stack inicial

| Área | Tecnologia proposta | Observações |
|---|---|---|
| Aplicativo Android | Kotlin | Host, ciclo de vida e integração |
| Interface | Jetpack Compose | Interface nativa |
| Runtime Python | Chaquopy 17.2.0 + Python 3.13 | Runtime inicial validado no Moto G06 durante P0 |
| Persistência relacional | Room sobre SQLite | Acesso ao banco mediado por repositórios |
| Agendamento | WorkManager e AlarmManager | Escolha depende da precisão e natureza da tarefa |
| Interação com interface | AccessibilityService | Serviço nativo, consentimento explícito |
| Notificações | NotificationListenerService | Acesso especial concedido pelo usuário |
| Integração avançada | Shizuku / ADB / root | Adaptadores opcionais, sem pressupor disponibilidade |
| Build | Gradle Wrapper 8.14.8 + AGP 8.13.2 | Matriz inicial registrada no ADR-0005 
| Kotlin | 2.4.21 | KGP e Compose Compiler Plugin na mesma versão 
| Runtime Python | Chaquopy 17.2.0 + Python 3.13 | Python host 3.13.x para tarefas de build 
| Persistência | Room 2.8.5 + KSP 2.3.12 | Acesso mediado por repositórios Kotlin 
| Compose | BOM 2026.06.01 (Compose 1.11.4) + Activity Compose 1.13.0 | Compatível com compileSdk 36; Compose 1.12+ exige API 37 e AGP 9.2+ |

As versões de ferramentas e dependências deverão ser fixadas e verificadas no início da implementação. Compatibilidade de pacotes Python será validada individualmente.

## 3. Componentes e responsabilidades

### 3.1 Android Application

Responsável pela experiência de uso e pelo ciclo de vida do aplicativo.

Responsabilidades:
- Navegação, dashboard e editor.
- Gerenciamento de permissões e configurações.
- Exibição de execuções, logs e artefatos.
- Inicialização e encerramento controlados dos componentes.
- Comunicação com o núcleo por contratos internos.

A interface não deve executar código Python pesado na thread principal.

### 3.2 Android Application Core (Kotlin)

Coordena recursos que dependem diretamente do Android.

**Runtime Manager**
- Inicializa e mantém o runtime Python conforme necessário.
- Controla o ciclo de vida da integração.
- Encaminha solicitações e resultados.

**Execution Manager**
- Recebe solicitações de execução.
- Aplica política de concorrência.
- Controla cancelamento, timeout e estado.
- Associa cada execução a um identificador.

**Event Manager**
- Recebe eventos de serviços Android.
- Normaliza eventos para o contrato interno.
- Encaminha eventos elegíveis ao Python.

**Permission/Capability Manager**
- Informa quais capacidades estão disponíveis.
- Verifica permissões e autorizações especiais.
- Não deve prometer operações que o sistema não permite.

### 3.3 Python Automation Engine

É o núcleo funcional programável.

**Script Runtime**
- Recebe um script e parâmetros.
- Executa o código no contexto gerenciado.
- Captura stdout, stderr e exceções.
- Produz resultado estruturado.

**Workflow Engine**
- Coordena etapas e dependências.
- Avalia condições.
- Gerencia falhas e políticas de repetição.
- Registra o resultado de cada etapa.

**Scheduler**
- Mantém definições de agendamento e recorrência.
- Solicita ao núcleo Android o registro de trabalhos.
- Não deve depender de um loop Python permanentemente ativo para acordar o dispositivo.

**Event Dispatcher**
- Distribui eventos normalizados para automações inscritas.
- Aplica filtros e regras de roteamento.

**Logging e Artifact Manager**
- Padronizam logs e resultados.
- Registram arquivos produzidos e suas referências.

### 3.4 Android Bridge

Biblioteca Python de alto nível que abstrai as APIs nativas.

Módulos conceituais:

| Módulo | Capacidade pretendida |
|---|---|
| `mobile.apps` | Consultar e iniciar aplicativos/atividades permitidas |
| `mobile.screen` | API planejada: estado do serviço, interação geométrica e Mapas de Coordenadas; consulta semântica será ampliada nas prioridades posteriores |
| `mobile.notifications` | Consultar/reagir a notificações conforme autorização |
| `mobile.files` | Operar sobre arquivos acessíveis ao aplicativo |
| `mobile.system` | Consultar informações e recursos do dispositivo |
| `mobile.sensors` | Acessar sensores suportados e autorizados |
| `mobile.network` | Operações de rede e conectividade |
| `mobile.integrations` | Adaptadores opcionais como Shizuku/ADB |

A API pública deve oferecer funções Python estáveis e tipos de retorno previsíveis. Detalhes de Java/Kotlin não devem vazar para os scripts. Atualmente, `mobile.capabilities.list_available()` anuncia somente `device.get_info` e `apps.launch`, implementadas na P1. As operações `mobile.screen` descritas nos contratos P2 ainda não existem e só poderão ser anunciadas após implementação e testes correspondentes. O catálogo descreve implementação, não garante disponibilidade universal em qualquer dispositivo.

### 3.5 Persistência e arquivos

Room/SQLite será a fonte de verdade dos metadados. O acesso será mediado por repositórios Kotlin e serviços internos; Python não deve escrever diretamente nas tabelas do Room.

Entidades iniciais:
- Scripts e metadados.
- Snapshots imutáveis de versões de scripts.
- Execuções, parâmetros, status, horários, stdout, stderr e erro estruturado.
- Workflows e nós (fases futuras).
- Agendamentos (fase futura).
- Referências a artefatos (fase futura).

Arquivos de scripts, arquivos temporários, backups e artefatos serão armazenados em diretórios privados apropriados, com referências persistidas no banco. Na P1, importação/exportação de arquivos Python usa Storage Access Framework; a importação aceita texto UTF-8 de até 1 MiB e não executa automaticamente o conteúdo.

#### Modelo P1 de biblioteca e histórico

- `scripts`: código atual, nome, descrição, tags JSON, definições de parâmetros JSON, manifesto de dependências e timestamps.
- `script_versions`: snapshots imutáveis por script e número sequencial de versão.
- `script_executions`: histórico desacoplado do ciclo de vida do script, com nome/version snapshot, parâmetros resolvidos, estado, horários, duração, stdout/stderr, erro e flags de truncamento. Exceção de retenção futura: em execução que recebeu autorização de acesso a campos sensíveis, stdout/stderr/tracebacks não serão persistidos por padrão; persistência exige consentimento separado e explícito por execução, conforme ADR-0009.
- A exclusão de um script remove seus metadados e snapshots de versão em transação; registros de execução permanecem para preservar o histórico.
- Durante o desenvolvimento, Room utiliza `fallbackToDestructiveMigration()` para recriar o banco quando não houver migração compatível; isso pode descartar scripts, versões e execuções locais. Conforme ADR-0007, migrações de preservação só serão exigidas mediante solicitação expressa. O acesso permanece exclusivamente mediado por DAOs/repositórios Kotlin.
- Mapas de Coordenadas P2 deverão usar a mesma infraestrutura Room, por repositórios Kotlin, armazenando apenas pontos e metadados técnicos mínimos no armazenamento privado. Não serão exportados junto com scripts nem transmitidos automaticamente. Regras de backup/transferência deverão ser revisadas e validadas no Android 15/API 35 antes de afirmar exclusão de backup.
- O script recebe valores de entrada por um dicionário Python chamado `params`; a validação do esquema e dos valores ocorre no host antes da execução.
- O manifesto de dependências é declarativo. Não há promessa de instalação pip genérica em runtime; compatibilidade precisa ser confirmada para Chaquopy, CPython 3.13 e ABI Android alvo.

### 3.6 Acessibilidade e interação de interface — direção P2

A P2.1-A publicou a declaração do AccessibilityService como serviço Android nativo, com ciclo de vida gerenciado exclusivamente pelo sistema. A configuração inicial recebe somente `typeWindowStateChanged`, não recupera conteúdo de janelas e não executa gestos. O serviço não interpreta nem retém o payload dos eventos. A implementação ainda aguarda build, testes e validação no dispositivo. O usuário habilita/desabilita manualmente o serviço nas configurações. Um gateway application-scoped recebe a instância apenas pelos callbacks do serviço; não instancia o serviço nem retém Activity, e limpa a referência por identidade no teardown. A primeira implementação publica somente o estado interno de conexão; autorização habilitada, estados públicos e falhas operacionais ficam para P2.1-B.

A fronteira pública permanece Python-first: scripts chamam funções síncronas em mobile.screen; a Bridge Kotlin e o gateway trabalham assincronamente, encaminhando solicitações ao serviço. A espera finita ocorre somente no worker serial Python existente, nunca na main thread Android. Fila nativa limitada e gestos serializados evitam sobreposição de operações que possam cancelar umas às outras. Timeout e cancelamento são lógicos: uma ação já despachada pode continuar e não há promessa de rollback.

A primeira API pública planejada inclui get_status, tap, type_text, swipe, scroll, capture_coordinates, list_coordinate_maps, get_coordinate_map, delete_coordinate_map e tap_point, com mobile.screen.ScreenError para falhas operacionais. Os estados públicos distinguem not_enabled, disconnected e connected. O contrato completo e os limites estão em [P2_PYTHON_API.md](operations/P2_PYTHON_API.md) e [P2_ASYNC_PROTOCOL.md](operations/P2_ASYNC_PROTOCOL.md). Essas funções não estão implementadas.

TYPE_ACCESSIBILITY_OVERLAY será usado somente durante a captura explícita e cancelável do Mapa de Coordenadas; a camada intercepta intencionalmente toques nesse período. Não solicitar SYSTEM_ALERT_WINDOW para esse caso. Não haverá observação contínua, screenshots, OCR ou persistência de snapshots/árvores na P2 inicial. A consulta de conteúdo será pontual e sob demanda.

## 4. Contratos entre Kotlin e Python

A comunicação deve ser explícita, versionável e baseada em mensagens estruturadas.

Operações conceituais:
- Inicializar runtime.
- Executar script.
- Solicitar cancelamento.
- Consultar estado.
- Entregar evento.
- Invocar capacidade Android.
- Retornar resultado ou erro.

Uma solicitação de execução deve carregar, no mínimo:
- Identificador da execução.
- Identificador do script/automação.
- Código ou referência ao script.
- Parâmetros serializáveis.
- Metadados de contexto e versão do contrato.

Um resultado deve conter:
- Identificador da execução.
- Estado final.
- Valor de retorno serializável.
- stdout e stderr.
- Exceção estruturada, quando houver.
- Referências a artefatos.

Não compartilhar objetos Android arbitrários com o código Python. Preferir DTOs, primitivas e estruturas serializáveis.

## 5. Modelo de execução

Estados iniciais:

`QUEUED` → `RUNNING` → `SUCCEEDED` | `FAILED` | `CANCELLED`

Cada execução deve registrar:
- `execution_id`
- `automation_id` ou `script_id`
- horários de início e término
- parâmetros de entrada
- estado
- saída
- logs
- artefatos
- erro estruturado

A primeira versão terá concorrência conservadora: uma execução Python ativa por vez, com fila. A implementação deverá evitar bloquear a thread principal e oferecer cancelamento cooperativo. Encerrar à força uma thread Python não é uma estratégia segura de cancelamento.

A evolução para processos ou workers isolados depende de uma prova técnica. Um subprocesso com o mesmo UID Android não constitui, por si só, uma sandbox de segurança.

## 6. Execução em segundo plano e eventos

A interface e o motor de execução são responsabilidades distintas. A interface pode ser encerrada sem que isso implique cancelar automaticamente uma tarefa autorizada.

Mecanismos possíveis:
- **WorkManager:** trabalho persistente e adiável, sujeito às políticas do Android.
- **AlarmManager:** horários específicos, quando permitidos e autorizados.
- **Foreground Service:** trabalho prolongado que exige execução visível ao usuário e notificação.
- **AccessibilityService:** eventos e ações de interface autorizadas.
- **NotificationListenerService:** observação de notificações após autorização especial.

Nenhum desses mecanismos garante execução ininterrupta em todas as condições. Forçar parada pelo usuário, restrições de bateria, reinicializações, políticas do fabricante e versões do Android podem alterar o comportamento.

O agendador Python registra regras; a camada Android é responsável por solicitar e iniciar trabalhos usando os mecanismos apropriados. Não manter um processo Python permanentemente ativo apenas para verificar horários.

## 7. Adaptadores e capacidades

Cada integração deve declarar:
- Identificador da capacidade.
- Descrição.
- Requisitos de autorização.
- Disponibilidade atual.
- Limitações conhecidas.
- Implementação/adaptador responsável.

Exemplos de identificadores:
- `apps.launch`
- `screen.accessibility`
- `notifications.observe`
- `files.read_scoped`
- `system.shizuku`
- `system.root`

Shizuku, ADB e root são mecanismos opcionais. Não são equivalentes entre si, não estão sempre disponíveis e não devem ser requisitos do funcionamento básico.

## 8. Segurança

Scripts Python executam código. Um script pode usar a rede, consumir recursos e acessar dados e capacidades disponíveis ao processo do aplicativo. Por isso:

1. A primeira versão é destinada a scripts confiáveis e controlados pelo usuário.
2. Scripts importados devem ser identificados como externos e exigir confirmação antes da primeira execução.
3. Permissões Android devem ser solicitadas no contexto da capacidade que as utiliza.
4. Credenciais e segredos não devem ser gravados em código, logs ou parâmetros expostos.
5. Execuções devem possuir timeout, limites razoáveis e cancelamento cooperativo.
6. Logs devem evitar registrar conteúdo sensível por padrão.
7. Artefatos devem ser armazenados em locais privados e com nomes controlados.
8. Ações de alto impacto devem exigir configuração e autorização explícitas.
9. O produto não deve tentar contornar o modelo de segurança do Android.
10. Na P2, conteúdo comum de interface será consultado sob demanda e limitado à operação. Campos sensíveis permanecem bloqueados por padrão; leitura sensível e preenchimento/alteração sensível exigem consentimentos independentes, concedidos pela UI hospedeira e limitados à execução e ao aplicativo de destino. O script não pode conceder a própria autorização.
11. Concessões sensíveis são invalidadas ao trocar o app de destino, encerrar/cancelar a execução ou interromper/desconectar o serviço. Classificação ambígua deve falhar de forma conservadora; não há promessa de detecção perfeita.
12. Em execuções que receberam acesso sensível, stdout, stderr e tracebacks ficam apenas no console durante a execução e não são persistidos por padrão. Salvar essas saídas exige consentimento adicional, separado e limitado à execução. Essa política ainda exige evolução do executor/histórico P1.
13. Mapas armazenam somente coordenadas e metadados técnicos mínimos, em Room privado, sem exportação/transmissão automática. Regras Android de backup/transferência precisam de validação específica.

A execução em outro processo com o mesmo UID não isola o script das permissões do aplicativo. Uma sandbox real exigirá desenho específico, restrições de UID/permissões e avaliação própria. Não declarar isolamento forte até que ele seja implementado e testado.

## 9. Limitações conhecidas

- Nem todo elemento visual de outros aplicativos é exposto à árvore de acessibilidade.
- Conteúdos protegidos, telas seguras e operações sensíveis podem impedir captura ou interação.
- O acesso a notificações e acessibilidade exige autorização manual.
- Acesso a arquivos é limitado pelas APIs e pelo escopo de armazenamento do Android.
- Execução em segundo plano é condicionada pelas políticas do sistema e do fabricante.
- Alarmes exatos podem exigir autorização especial.
- Nem todo pacote Python possui distribuição binária compatível com Android/Chaquopy.
- Bibliotecas que dependem de processos, extensões nativas ou recursos específicos do desktop podem não funcionar.
- Shizuku e root ampliam capacidades, mas não garantem acesso universal.

## 10. Estratégia de testes

- Testes unitários do motor Python e dos contratos.
- Testes de integração Kotlin/Python.
- Testes instrumentados dos adaptadores Android.
- Testes de persistência e recuperação.
- Testes de cancelamento, timeout e falha.
- Testes físicos em dispositivo Android real.
- Registro de versão do Android, fabricante e permissões nos relatórios de validação.

A implementação P2 deverá ser validada em etapas, distinguindo revisão documental, testes automatizados, build e validação física. Nenhuma capacidade P2 descrita acima deve ser apresentada como pronta antes dessas evidências. Consulte [ROADMAP.md](ROADMAP.md), [ADR-0008](adr/0008-accessibility-service-bridge-and-lifecycle.md) e [ADR-0009](adr/0009-sensitive-data-consent-and-retention.md).
