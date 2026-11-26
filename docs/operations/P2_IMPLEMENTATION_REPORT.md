# P2 — Relatório operacional de implementação

**Projeto:** Androide PY  
**Branch:** `feat/p2-accessibility-automation`  
**Base:** `main`  
**Marco:** P2 — Automação de interfaces Android  
**Data do registro:** 2026-11-25  
**Estado:** P2.0.1–P2.0.10 concluídas documentalmente. P2.1-A — Fundação nativa — implementada e compilada em build debug local. P2.1-B — Autorização e integração — implementação publicada, revisão estática e build debug local aprovados. P2.1-C — testes JVM/Python host e build debug aprovados; validação física do ciclo de autorização PASS; F2 (Forçar parada) caracterizado como comportamento esperado do Android, não como falha do app. **Validação física P2.1-C concluída no escopo acordado:** ciclo de autorização PASS, Force stop caracterizado como comportamento esperado do Android e reinicialização normal PASS no Moto G06.

## 1. Objetivo

Evoluir a Android Bridge do Androide PY para permitir que scripts Python interajam de forma controlada com interfaces de outros aplicativos Android, utilizando um serviço nativo de acessibilidade autorizado pelo usuário.

A P2 deve oferecer dois meios complementares de interação:

- **Geométrico:** operar em posições da tela por coordenadas, inclusive por meio de mapas visuais calibrados e reutilizáveis.
- **Semântico:** consultar e localizar elementos que a árvore de acessibilidade do aplicativo de destino efetivamente disponibiliza.

A lógica de automação continua Python-first. Kotlin hospeda a integração nativa e o serviço Android; os scripts recebem estruturas simples e resultados estáveis, nunca objetos Android arbitrários.

## 2. Baseline e contexto arquitetural

A P2 parte da branch `main` após a integração da P1.

A arquitetura existente estabelece:
- Kotlin e Jetpack Compose como host Android.
- Chaquopy/Python como runtime de automação.
- Android Bridge baseada em capacidades específicas e contratos Python estáveis.
- Acesso a APIs Android por adaptadores Kotlin, sem expor `Context`, `Activity`, `PackageManager` ou objetos Android arbitrários aos scripts.
- Shizuku/ADB/root como alternativas opcionais, não dependências do núcleo.
- Execução Python serial como política inicial; não interromper threads Python arbitrariamente.
- Scripts tratados como código confiável do usuário, sem alegação de sandbox universal.

A ADR-0004 já estabelece a Bridge baseada em capacidades e reconhece AccessibilityService como mecanismo apropriado para eventos e ações de interface autorizadas, sem prometer acesso irrestrito. A P2 concretiza essa direção e deve respeitar as limitações do Android.

A inspeção detalhada do código atual e a confirmação de versões/API Android são tarefas de P2.0; este relatório não presume detalhes ainda não verificados.

## 3. Decisões de escopo aceitas em 25/11/2026

### 3.1 Ordem de prioridades

| Prioridade | Escopo aceito | Objetivo |
|---|---|---|
| A — Interação | Cliques/toques, digitação, rolagem e Mapa de Coordenadas | Tornar possível executar ações essenciais em interfaces |
| B — Consulta e coleta | Consultar informações disponíveis, coletar dados, retornar ao Python, console e possível arquivo | Permitir automações orientadas por dados da interface |
| C — Leitura de elementos | Leitura estruturada e busca na árvore de acessibilidade | Oferecer identificação semântica mais elaborada |

A ordem é intencional: as ações fundamentais vêm primeiro; a consulta/coleta vem em seguida; a leitura completa da árvore e busca avançada são complementares.

### 3.2 Mapa de Coordenadas — parte da Prioridade A

O Mapa de Coordenadas foi aceito como ferramenta de calibração visual reutilizável, e não apenas como seletor pontual.

Fluxo pretendido:
1. Script abre o aplicativo de destino ou aguarda o usuário chegar à tela desejada.
2. Um timer configurável aguarda antes de ativar a seleção (30 segundos foi citado como exemplo).
3. Uma camada translúcida sobrepõe a interface de destino.
4. O usuário toca em uma posição e recebe indicação visual do ponto.
5. O ponto é nomeado e salvo; o usuário pode registrar vários pontos no mesmo mapa.
6. A camada pode ser concluída ou cancelada e deixa de interceptar toques.
7. O script recebe os pontos e pode reutilizá-los em execuções futuras.

A decisão aceita no ADR-0008 é usar `TYPE_ACCESSIBILITY_OVERLAY` associado ao AccessibilityService durante a captura explícita. A camada intercepta intencionalmente o toque durante a seleção; não é transparente aos eventos nesse intervalo. Não se solicita `SYSTEM_ALERT_WINDOW` para esse caso.

Modelo de coordenadas:
- armazenar x/y em pixels e normalizados;
- guardar largura/altura de referência e contexto de orientação/insets/densidade quando necessário;
- verificar compatibilidade antes de reutilizar;
- reconhecer que coordenadas normalizadas não corrigem mudanças de layout ou conteúdo.

O mapa poderá conter pontos nomeados (por exemplo, entrada, pesquisa, confirmar e rolagem) e ser salvo como perfil local reutilizável.

### 3.3 Consulta e saída de dados

A Prioridade B deve devolver dados em tipos Python simples e serializáveis para processamento em variáveis e exibição no console existente. A gravação em arquivo é desejada quando tecnicamente viável, mas depende de uma API de arquivos existente ou de uma extensão aprovada com destino/permissões/limites explícitos. Não se deve prometer exportação antes de verificar e implementar esse contrato.

### 3.4 Segurança e limites

- O usuário deve habilitar explicitamente o serviço nas configurações do Android.
- A aplicação deve explicar finalidade e capacidades e informar quando o serviço estiver indisponível.
- Não solicitar root, ADB ou Shizuku para o funcionamento central.
- Não prometer controle universal de aplicativos, telas seguras, campos protegidos ou conteúdo não exposto pela acessibilidade.
- Minimizar observação e coleta; não persistir conteúdo de tela automaticamente.
- Não registrar texto digitado ou conteúdo sensível em logs por padrão.
- Tratar desconexão, revogação, timeout e operação não suportada com erros claros.


## 3.1 Revisão técnica e conferência da arquitetura — 25/11/2026

A revisão documental e estática inicial foi realizada contra a branch `main` e o estado remoto da branch P2. O resultado detalhado, com evidências, arquivos inspecionados, referências oficiais e inconsistências documentais, está registrado em [P2_EXECUTION_CHECKLIST.md — Resultado da revisão técnica inicial](P2_EXECUTION_CHECKLIST.md).

### Baseline Git

- `main`: `205044caffc9df7acb94f43eb85cbeea0787d9f3`.
- `feat/p2-accessibility-automation`: `281060199fd68122ec0f0b762ccc8d7214a15ad0` antes do registro da revisão.
- A comparação indicou 5 commits à frente, 0 atrás; os únicos arquivos divergentes eram os documentos de abertura da P2. Nenhum código de produção foi alterado na branch.

### Situação técnica encontrada

- O manifesto atual declara somente a Activity principal e consultas de intents para abertura de apps. Não há AccessibilityService, configuração XML, autorização especial ou overlay implementados.
- `AndroidBridgeAdapter` implementa somente `device.get_info` e `apps.launch`.
- O runtime Chaquopy é singleton de processo e exige chamadas fora da thread principal.
- `ScriptExecutor` usa um único worker Python serial e fila limitada; a Bridge é configurada antes de cada execução.
- O módulo Python `mobile` usa um adaptador global e um catálogo de capacidades explícito, que hoje lista apenas as duas funções existentes.
- Não existe API `mobile.screen`, gateway nativo de acessibilidade, persistência de mapas de coordenadas ou API Python genérica para gravar arquivos.
- A arquitetura mantém Room/SQLite atrás de DAOs/repositórios Kotlin. A importação/exportação SAF da UI P1 não equivale a uma API Python de arquivos.
- Os testes existentes cobrem Bridge de apps, repositórios, parâmetros e runner; não há testes P2.

### Direção técnica a preservar

O serviço deverá ser gerenciado pelo Android e expor seu estado a um gerenciador application-scoped; o adaptador Python/Kotlin deverá depender de uma interface/gateway testável, não de uma Activity ou instância criada manualmente do Service. O worker Python pode aguardar uma operação nativa assíncrona com timeout, desde que o serviço processe comandos no handler apropriado e nunca bloqueie a thread principal. Usar request IDs, resultados serializáveis, erros estáveis, timeout, cancelamento lógico e serialização de gestos.

Separar toque geométrico por coordenadas de ação semântica por nó. Digitação depende de campo editável/foco e de suporte a `ACTION_SET_TEXT`; rolagem pode usar ações de nó ou gesto coordenado. Consultas devem produzir snapshots limitados de dados serializáveis, nunca expor nós Android ao Python nem reter referências obsoletas.

O Mapa de Coordenadas deve ser uma janela `TYPE_ACCESSIBILITY_OVERLAY` do serviço conectado, com captura intencional do toque durante a seleção, múltiplos pontos nomeados e persistência reutilizável. A persistência dos mapas foi decidida: Room existente, por repositórios Kotlin, dados mínimos e armazenamento privado, sem exportação/transmissão automática. Regras de backup/transferência ainda precisam ser configuradas e validadas na implementação. A API Python de arquivo de saída continua sem contrato aprovado.

### Compatibilidade e referências Android

A baseline observada no Gradle é minSdk 24, compileSdk/targetSdk 36, ABI arm64-v8a, Chaquopy 17.2.0/Python 3.13 e Kotlin 2.4.21. As referências oficiais consultadas indicam `dispatchGesture()` desde API 24 (exige `canPerformGestures`), `TYPE_ACCESSIBILITY_OVERLAY` desde API 22, `ACTION_SET_TEXT` desde API 21 e consulta da árvore condicionada a `canRetrieveWindowContent`. A documentação alerta contra `typeAllMask` indiscriminado e informa que a árvore pode mudar ou ficar obsoleta. O usuário confirmou por execução de `mobile.device.get_info()` no Moto G06 Android 15/API 35; o registro está detalhado na seção P2.0.5.

### Pendências documentais identificadas

Na revisão P2.0.10 foram confirmadas e corrigidas referências desatualizadas de status P1, a descrição de acessibilidade/`mobile.screen` ainda como fase futura genérica, o roadmap sem prioridades A/B/C e Mapa de Coordenadas, estados antigos dos ADRs-0003/0006, a política de retenção sensível ainda descrita como decisão aberta e referências antigas de preparação ainda pendente. O índice `docs/README.md` já continha os quatro contratos/relatórios P2 e a árvore ADR-0008/0009; foi conferido e mantido.

**Estado:** revisão documental/estática inicial concluída para P2.0.3 e P2.0.4. P2.0.5 ainda depende da confirmação do SDK real no Moto G06 e da conclusão da análise de lifecycle. Nenhum teste, build ou validação física P2 foi executado.

## 3.2 P2.0.5 — Baseline Android e lifecycle — 2026-11-25

### Baseline de build
Conferida estaticamente em `android/app/build.gradle.kts`: compileSdk 36, targetSdk 36, minSdk 24, ABI `arm64-v8a`, JVM target 17, Chaquopy 17.2.0/Python 3.13.

### Dispositivo físico
Sam executou `mobile.device.get_info()` no Moto G06 e reportou:
- Motorola `moto G06`, codinome `lagos`.
- Android 15, API 35.
- ABI `arm64-v8a`.
- Pacote `com.samuel.androidepy`, versão `0.1.0`, schema 1.

A versão real do dispositivo está, portanto, dentro da faixa suportada pela baseline configurada (API 35 >= minSdk 24). Este registro é evidência física reportada pelo usuário; não foi capturada diretamente pela integração de repositório.

### Compatibilidade das APIs
| Recurso | API mínima | Resultado com minSdk 24 / Moto G06 API 35 |
|---|---:|---|
| `ACTION_SET_TEXT` | 21 | API disponível; cada campo/app pode não aceitar a ação |
| `TYPE_ACCESSIBILITY_OVERLAY` | 22 | API disponível para overlay do AccessibilityService conectado |
| Consulta da árvore (capability) | 18 | API disponível; requer `canRetrieveWindowContent` |
| `dispatchGesture()` | 24 | API disponível; requer `canPerformGestures` |

Não há necessidade de elevar o minSdk para P2. O nível API não garante que todo aplicativo exponha seus elementos ou aceite operações de texto/gesto.

### Lifecycle aprovado como direção arquitetural
O Android gerencia exclusivamente o ciclo de vida do AccessibilityService; o usuário precisa habilitá-lo explicitamente nas configurações. Após o bind, o sistema chama `onServiceConnected()`. O serviço pode parar quando o usuário o desabilita ou quando chama `disableSelf()`; os callbacks de interrupção e desconexão devem limpar/atualizar o estado publicado pela aplicação.

A aplicação deverá:
- separar autorização habilitada de conexão efetivamente disponível;
- publicar conexão/desconexão em gerenciador application-scoped;
- registrar a instância apenas a partir dos callbacks do serviço e remover a referência na desconexão/encerramento;
- nunca instanciar manualmente o serviço ou reter Activity;
- solicitar somente capabilities necessárias a cada operação;
- serializar gestos, pois `dispatchGesture()` cancela gestos que já estejam em andamento;
- não interpretar retorno positivo de despacho como confirmação de que a interface de destino concluiu a ação.

Para o Mapa de Coordenadas, `TYPE_ACCESSIBILITY_OVERLAY` permanece a opção prioritária; a camada interceptará intencionalmente o toque durante a seleção. Não há requisito para adicionar `SYSTEM_ALERT_WINDOW` para esse overlay específico.

### Referências oficiais
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo
- https://developer.android.com/guide/topics/ui/accessibility/views/service

**Evidência e limites:** revisão estática do Gradle e documentação oficial; versão/API do aparelho confirmada por execução reportada pelo usuário. Nenhum teste automatizado, build APK ou validação física de funcionalidade P2 foi realizado nesta subetapa.

## 3.3 P2.0.6 — Protocolo assíncrono Python/Kotlin/AccessibilityService — 2026-11-25

O contrato completo e normativo foi publicado em [P2_ASYNC_PROTOCOL.md](P2_ASYNC_PROTOCOL.md), versão 1.

### Decisão de execução

A API Python inicial será síncrona do ponto de vista do script, para preservar uma experiência simples e sequencial. Internamente, a chamada será assíncrona: a Bridge Kotlin registra uma solicitação no gateway application-scoped, o gateway encaminha o comando ao handler do AccessibilityService e o callback completa um `CompletableFuture`. O adaptador aguarda com timeout finito exclusivamente no worker serial Python.

Essa escolha reutiliza o executor existente e não cria outro runtime Python, worker ou thread por chamada. O código atual confirma que o executor possui um único worker, configura a Bridge antes da execução e já executa Chaquopy fora da main thread.

### Contrato v1

- Solicitação/resposta trafegam como strings JSON versionadas; scripts não recebem objetos Android.
- Cada solicitação possui UUID `request_id`, operação, payload validado e `timeout_ms`.
- Resposta terminal contém `request_id`, `ok`, `result` ou `error` com código estável e mensagem.
- O prazo é calculado com relógio monotônico. O timeout abrange fila e execução.
- Timeout/cancelamento são lógicos: callbacks tardios são ignorados; uma ação já despachada pode continuar no Android e não há garantia de rollback.
- Autorização habilitada é distinta de serviço conectado. Interrupção/desconexão falha pendências; reconexão não restaura solicitações antigas.
- Gateway nativo mantém fila limitada a 16 solicitações; gestos são serializados e o slot físico só é liberado após callback terminal ou teardown.
- Resultado distingue aceitação do despacho, conclusão da API Android e efeito real na interface de destino.

### Compatibilidade Chaquopy/Android

A documentação oficial Chaquopy 17.2 informa que o GIL é liberado durante chamadas Python → Java. A documentação Android confirma `CompletableFuture` e `get(timeout, unit)` desde API 24, igual ao minSdk do projeto. O contrato não utiliza `completeOnTimeout`, introduzido na API 31. Portanto, a estratégia proposta não exige elevar minSdk nem adicionar dependência Kotlin.

### Estado e evidência

P2.0.6 foi concluída como definição documental e revisão estática. O contrato foi conferido contra `PythonRuntime`, `ScriptExecutor`, `AndroidBridgeAdapter`, `runner.py`, fachada `mobile`, contrato P0 da Bridge e contrato P0 de execução. Nenhum código de produção foi alterado; nenhum teste automatizado, build APK ou validação física foi executado.

Os ADRs-0008/0009 foram publicados na P2.0.9; a API pública e seus defaults foram definidos na P2.0.7. Ambos permanecem contratos/decisões, não funcionalidades implementadas.

## 3.4 P2.0.7 — Contrato público Python `mobile.screen` — 25/11/2026

O contrato público v1 foi publicado em [P2_PYTHON_API.md](P2_PYTHON_API.md).

A API será síncrona para os scripts e assíncrona internamente conforme o protocolo P2.0.6. Falhas operacionais serão representadas por `mobile.screen.ScreenError`, derivada de `RuntimeError`, com `code`, `retryable` e `request_id`. Erros locais de argumentos serão `TypeError`/`ValueError`. A API legada `mobile.apps.launch()` permanece inalterada.

A superfície definida inclui:
- Estado de autorização/conexão por `get_status()`, sem abrir Configurações nem solicitar autorização.
- Interação geométrica por `tap`, `type_text`, `swipe` e `scroll`, com pixels como padrão, coordenadas normalizadas opcionais e timeouts finitos.
- Mapa de Coordenadas por `capture_coordinates`, `list_coordinate_maps`, `get_coordinate_map`, `delete_coordinate_map` e `tap_point`; captura de vários pontos nomeados, persistência local e verificação de contexto conhecido.
- Retornos simples serializáveis e distinção explícita entre conclusão da API Android e efeito real no aplicativo de destino.

`type_text` atua apenas no campo focado por `ACTION_SET_TEXT`, substituindo o conteúdo atual; não há promessa de fallback universal. A primeira operação `scroll` é um gesto geométrico a partir do centro do display. Coordenadas normalizadas não garantem compatibilidade entre layouts.

O catálogo `mobile.capabilities` só deverá listar cada operação depois de sua implementação e testes. `API_VERSION = 1` permanece compatível por adição não disruptiva; a versão da biblioteca será atualizada quando o módulo for efetivamente implementado.

**Evidência:** commit documental `dc7e4673cccb1070b594805a9163d059bfdd6a41`; revisão estática contra os módulos Python atuais, testes host, Bridge Kotlin, executor serial e protocolo P2.0.6. Nenhum código de produção foi alterado; nenhum teste, build ou validação física foi executado.


## 3.5 P2.0.8 — Revisão de privacidade e segurança — 25/11/2026

A revisão foi publicada em [P2_SECURITY_REVIEW.md](P2_SECURITY_REVIEW.md). Foram examinados o manifesto, a Bridge, o runtime Python, o executor, runner, persistência Room, histórico, testes e ADRs aplicáveis. Na comparação feita no início desta revisão, a branch estava 20 commits à frente e 0 atrás de main; o diff então observado continha apenas documentação P2.

### Decisões
- Conteúdo comum pode ser lido sob demanda, quando explicitamente solicitado e disponibilizado pelo Android; não haverá coleta contínua nem persistência automática de snapshots.
- Campos sensíveis são bloqueados por padrão. A futura UI hospedeira deverá permitir consentimento expresso e separado para leitura e escrita, limitado à execução e ao aplicativo de destino. O script não pode conceder a própria autorização.
- A classificação usa sinais disponíveis e comportamento conservador; não existe garantia de detecção perfeita nem autorização para contornar proteções do sistema/app.
- Screenshots e notificações permanecem fora do escopo inicial da P2.
- Mapas guardam coordenadas e metadados mínimos em Room, no armazenamento privado, sem exportação ou transmissão automática.
- Overlay é temporário e removido em todos os caminhos terminais previstos.
- O adaptador não deve registrar conteúdo de interface. Foi identificada, porém, uma limitação P1: stdout/stderr, parâmetros e exceções/tracebacks são persistidos no histórico; scripts podem imprimir dados que obtiveram. Essa limitação não foi resolvida nesta subetapa.
- O produto não envia dados de acessibilidade automaticamente, mas scripts não possuem sandbox e podem utilizar rede/bibliotecas disponíveis ao processo.

### Evidência e limites

Revisão documental/estática concluída e documento publicado. Nenhum código de produção foi alterado; nenhum teste automatizado, build APK ou validação física P2 foi executado. A autorização sensível, classificação, overlay, política de retenção e persistência ainda precisam ser implementados e testados nas subetapas seguintes. As decisões arquiteturais foram registradas na P2.0.9 pelos ADRs-0008 e 0009; a harmonização dos documentos principais foi concluída na P2.0.10.


## 3.6 P2.0.9 — ADRs de arquitetura e segurança — 25/11/2026

Foram criados dois ADRs aceitos para orientar a implementação futura:

- [ADR-0008 — AccessibilityService, gateway e fronteira da Android Bridge](../adr/0008-accessibility-service-bridge-and-lifecycle.md): define lifecycle gerenciado pelo Android, gateway application-scoped, fronteira Python/Kotlin, uso do protocolo assíncrono, concorrência/gestos, overlay restrito, minimização e persistência Room por repositórios Kotlin.
- [ADR-0009 — Consentimento para dados sensíveis e retenção de saídas](../adr/0009-sensitive-data-consent-and-retention.md): define bloqueio sensível por padrão, autorizações separadas para leitura e escrita, escopo por execução/aplicativo, revogação, classificação conservadora, retenção efêmera por padrão de stdout/stderr/tracebacks em execuções que receberam acesso sensível e consentimento adicional para persistir essas saídas. Também consolida mapas mínimos e verificação explícita das regras de backup/transferência.

O índice `docs/adr/README.md` foi atualizado. As decisões foram conferidas contra ADRs 0001–0007, visão, arquitetura, roadmap, contratos P2.0.6/P2.0.7 e revisão de segurança P2.0.8.

**Evidência e limites:** ADR-0008 commit `c6b9be89327243ed6164e7b529474d918819a3d8`; ADR-0009 commit `f73f2b0729b683786c2ef4891872472413c06870`. Revisão documental/estática; nenhum código de produção foi alterado e nenhum teste, build APK ou validação física foi executado. Os ADRs estão aceitos como decisões de projeto, não validados por implementação. A harmonização documental dos documentos principais foi concluída na P2.0.10.

## 4. Decomposição aprovada

A decomposição de execução está detalhada no [Checklist Operacional P2](P2_EXECUTION_CHECKLIST.md). Por decisão de Sam em 25/11/2026, cada fase terá no máximo três subetapas de execução; requisitos internos permanecem como critérios verificáveis, não como entregas fragmentadas.

1. **P2.0 — Preparação, baseline e contratos:** leitura da documentação e código real, confirmação das versões/API, protocolo assíncrono, API Python, privacidade e ADRs.
2. **P2.1 — Fundação nativa e autorização**, dividida em:
   - **P2.1-A — Fundação nativa:** AccessibilityService, XML/manifesto, gateway application-scoped e lifecycle.
   - **P2.1-B — Autorização e integração:** explicação e acesso às configurações Android, estados `not_enabled`/`disconnected`/`connected`, política mínima de eventos e erros estáveis.
   - **P2.1-C — Testes e validação:** testes JVM/host, revisão integrada, build local e validação no Moto G06.
   
   Os nove requisitos originais P2.1.1–P2.1.9 foram mapeados nas três entregas e permanecem preservados no checklist.
3. **P2.2 — Prioridade A: interação e Mapa de Coordenadas:** toques/cliques, digitação, rolagem, gestos básicos e mapa visual com timer, overlay, múltiplos pontos, persistência e reutilização.
4. **P2.3 — Prioridade B: consulta e coleta:** snapshot/consulta sob demanda, estruturas Python, filtros, console, opção de arquivo e proteção de dados sensíveis.
5. **P2.4 — Prioridade C: leitura e identificação:** árvore limitada, atributos, busca semântica, referências temporárias e ações sobre elementos.
6. **P2.5 — Integração, testes e validação:** catálogo de capacidades, testes host/JVM, build, validação física no Moto G06, documentação e gate de integração.

A sequência preserva a prioridade funcional sem eliminar dependências técnicas: a fundação do serviço e o protocolo precisam existir antes das ações Python.

## 5. Critérios gerais de aceite

- Cada operação deve ter contrato Python documentado, tipos/retornos previsíveis, timeout e erros estáveis.
- O estado de autorização do AccessibilityService deve ser consultável e a revogação deve falhar de forma controlada.
- Cliques/toques, digitação e rolagem devem ser demonstrados individualmente no Moto G06.
- O Mapa de Coordenadas deve permitir selecionar e nomear múltiplos pontos, persistir o perfil e reutilizá-lo após reabrir o aplicativo.
- Coordenadas em pixels e normalizadas devem ser documentadas; mudanças de orientação/tela devem ser detectadas ou tratadas.
- Consulta/coleta deve retornar dados acessíveis ao Python e exibir resultado no console; arquivo de saída só será considerado aceito se sua implementação for incluída e validada.
- Leitura semântica deve respeitar limites de profundidade/tamanho e indicar quando o app não expõe os elementos.
- Testes host/JVM, build e validação física devem ser relatados separadamente.
- Nenhuma capacidade pode aparecer como implementada no catálogo mobile antes de existir e ter contrato/testes correspondentes.

## 6. Testes e validações

Nenhum teste P2 foi executado na abertura deste relatório.

| Categoria | Estado atualizado em 26/11/2026 |
|---|---|
| Revisão documental e arquitetural | P2.0.1–P2.0.10 concluídas documentalmente |
| Inspeção estática P2.1 | PASS — manifesto, XML, lifecycle, gateway, interrupção/desconexão e main thread revisados |
| Testes Python host | 13/13 PASS, conforme execução reportada por Sam |
| Testes JVM/Kotlin | PASS em `:app:testDebugUnitTest`, conforme execução reportada por Sam |
| Build APK | `:app:assembleDebug` PASS; build combinado com testes JVM: `BUILD SUCCESSFUL`, 32 s, 55 tarefas |
| Autorização/revogação no Moto G06 | PASS — card e estado do Android acompanharam habilitação/revogação |
| Force stop | Comportamento esperado do Android: autorização desabilitada pelo sistema; não é teste de reconexão com autorização preservada |
| Reinicialização normal | PASS — autorização preservada e card `Conectado` após reboot no Moto G06/Android 15 (API 35) |
| P2.2 — interação/mapas | Não iniciada; revisão técnica, decomposição e saneamento documental concluídos; aguardando autorização de implementação |
| P2.3/P2.4/P2.5 | Não iniciadas |

Os resultados históricos da P0/P1 não são evidência de funcionamento da P2.

## 7. Fora do escopo

- NotificationListenerService e automação de notificações.
- Agendamento e execução recorrente.
- Workflows/fluxos visuais avançados.
- Dependência obrigatória de Shizuku, ADB ou root.
- OCR/captura de tela como requisito inicial do Mapa de Coordenadas.
- Contorno de permissões, políticas Android ou proteções de aplicativos.
- Controle universal de jogos, apps bancários, telas seguras ou componentes não acessíveis.

## 8. Riscos e questões técnicas a resolver

1. Viabilidade e comportamento de `TYPE_ACCESSIBILITY_OVERLAY` nas versões Android suportadas e no Moto G06.
2. Implementar e testar o protocolo assíncrono já especificado entre execução Python serial e serviço Android, evitando bloqueio da UI, callbacks tardios e chamadas indefinidas.
3. Limitações de `dispatchGesture`, ações de texto e ações de nós conforme API level e aplicativo de destino.
4. Conversão correta entre coordenadas físicas, densidade, orientação, barras do sistema, insets e área útil.
5. Persistência e versionamento de mapas de coordenadas sem criar acoplamento desnecessário.
6. Tratamento de dados sensíveis e limites de coleta.
7. Variação da árvore de acessibilidade entre apps, WebViews, componentes customizados e conteúdo desenhado.
8. Estratégia de testes que diferencie “gesto despachado” de “ação confirmada na interface”.

Esses itens não bloqueiam a criação da branch e dos documentos operacionais, mas devem ser resolvidos antes das respectivas implementações.

## 9. Integração

- Branch de trabalho: `feat/p2-accessibility-automation`.
- Base: `main`.
- Estado: branch criada e documentação operacional inicial publicada e conferida no GitHub.
- PR: ainda não criada.
- Merge: não realizado nem autorizado nesta etapa.
- Implementação: não iniciada.

## 10. Histórico de atualização

| Data | Alteração |
|---|---|
| 2026-11-25 | Abertura da P2; prioridades A/B/C, Mapa de Coordenadas na Prioridade A, decomposição e limites registrados conforme decisões de Sam. Checklist commit `8c23d5b79a9e6d0de86a4ce4287ed6c7996af302`; relatório commit `d8c2e409cf11077a8dd7da596a3380d339603eb8`; índice docs commit `3017ad8fa0983feb2d0950b366242eae0349786c`. |
| 2026-11-25 | P2.0.6: contrato assíncrono v1 publicado em `P2_ASYNC_PROTOCOL.md`; decisão de espera no worker Python com gateway Kotlin assíncrono; sem alterações de produção ou execução de testes/build. |
| 2026-11-25 | P2.0.8: revisão de privacidade e segurança publicada em `P2_SECURITY_REVIEW.md`; autorização sensível separada para leitura/escrita, minimização de dados e riscos residuais P1 documentados; sem alterações de produção ou execução de testes/build. |
| 2026-11-25 | P2.0.9: ADR-0008 e ADR-0009 publicados; fronteira AccessibilityService/Bridge, consentimento sensível e retenção independente de saídas formalizados; sem alterações de produção ou execução de testes/build. |
| 2026-11-25 | P2.0.10: visão, arquitetura, roadmap, ADRs/índices e contratos harmonizados com as decisões aceitas; sem alterações de produção, testes, build ou validação física. Próxima etapa: P2.1. |
| 2026-11-25 | Decomposição P2.1 simplificada por aprovação de Sam: os nove requisitos originais foram preservados e agrupados em P2.1-A (fundação nativa), P2.1-B (autorização e integração) e P2.1-C (testes e validação). Nenhuma implementação foi iniciada nesta atualização documental. |
| 2026-11-25 | P2.1-A: implementação inicial do AccessibilityService, configuração mínima, declaração no manifesto e gateway application-scoped publicada e revisada estaticamente. Sem build, testes automatizados ou validação física; esses gates permanecem pendentes. |
| 2026-11-25 | P2.1-A | Sam confirmou build local `:app:assembleDebug --no-daemon --max-workers=2`: `BUILD SUCCESSFUL` em 38 s (48 tarefas; 18 executadas, 30 atualizadas). Sem testes automatizados ou validação física nesta etapa. P2.1-A concluída quanto à implementação e compilação. |
| 2026-11-25 | P2.1-B | UI de autorização e status, estado Kotlin/Bridge interno, política de interrupção e códigos estáveis publicados e revisados estaticamente. Sam confirmou build local `:app:assembleDebug --no-daemon --max-workers=2`: `BUILD SUCCESSFUL` em 13 s; 48 tarefas (1 executada/47 atualizadas). Testes automatizados e validação física pendentes para P2.1-C; nenhuma API Python anunciada. |


## 3.7 P2.0.10 — Harmonização documental — 25/11/2026

A documentação principal e os contratos foram conferidos contra o checklist operacional, AGENTS.md, os ADRs-0003–0009, o estado final da P1 e as decisões aceitas na P2.0.6–P2.0.9.

### Correções harmonizadas
- README, visão, arquitetura e roadmap agora refletem P0/P1 concluídas e integradas, com as pendências P1 diferidas explicitamente preservadas.
- Roadmap detalha P2.0–P2.5 e as prioridades A (interação e Mapa de Coordenadas), B (consulta/coleta) e C (leitura/identificação), sem apresentar contratos como implementação.
- Arquitetura incorpora a fronteira pública mobile.screen, API Python síncrona/Kotlin assíncrono, lifecycle Android, gateway application-scoped, overlay restrito e política de segurança/retenção.
- ADR-0003 e ADR-0006 e o índice ADR foram alinhados às evidências finais da P1 e à decisão de mapas/retenção P2.
- Contrato Python e revisão de segurança deixaram de descrever decisões já aceitas como pendentes/abertas.
- Índice docs/README.md e árvore de documentos foram conferidos; links P2 e ADR-0008/0009 estão listados.

### Limites preservados
Os contratos continuam futuros: não existe ainda AccessibilityService, gateway, overlay, mobile.screen, persistência de mapas ou controles de consentimento. Screenshots, OCR e notificações continuam fora do escopo inicial; não há observação contínua. Scripts não possuem sandbox. Mapas dependem de validação futura das regras Android de backup/transferência. Em execuções com acesso sensível, saídas não serão persistidas por padrão; consentimento adicional será necessário para salvá-las, política ainda não implementada.

**Evidência e limites:** revisão documental/estática dos arquivos principais, contratos e ADRs; nenhum código de produção alterado, teste automatizado executado, build APK executado ou validação física P2 realizada. A P2.0.10 é documental e não conclui a P2.1.

## 3.8 P2.1 — Decomposição aprovada e início da P2.1-A — 25/11/2026

Por aprovação explícita de Sam, os nove requisitos iniciais de P2.1 foram agrupados em três subetapas: P2.1-A (fundação nativa), P2.1-B (autorização e integração) e P2.1-C (testes e validação). O mapeamento integral e os critérios de aceite estão no checklist operacional.

### CONFERE — P2.1-A

- Branch de trabalho: `feat/p2-accessibility-automation`; comparação com `main` confirmou 54 commits à frente e 0 atrás antes da atualização documental, com base `205044caffc9df7acb94f43eb85cbeea0787d9f3`.
- A branch continha somente alterações documentais da P2.0; o manifesto não declarava AccessibilityService, XML de configuração, gateway ou permissões de acessibilidade.
- `AndroidePyApplication` já é o ponto application-scoped para repositórios e `ScriptExecutor`; não há necessidade de criar outro contêiner ou runtime.
- `ScriptExecutor` mantém um único worker Python serial com fila limitada a 16 e injeta `AndroidBridgeAdapter` antes de cada execução. A P2.1-A não altera esse fluxo.
- ADR-0008 exige lifecycle gerenciado pelo Android, gateway application-scoped, nenhuma Activity retida, nenhum serviço instanciado manualmente e minimização de eventos/capabilities.
- A documentação oficial Android confirma que o sistema inicia o AccessibilityService após habilitação explícita pelo usuário, chama `onServiceConnected()` após o bind e exige intent-filter `android.accessibilityservice.AccessibilityService` e permissão `BIND_ACCESSIBILITY_SERVICE`. A configuração XML permite declarar capabilities como acesso ao conteúdo e gestos; essas capabilities não serão solicitadas na fundação inicial.
- O filtro inicial será limitado a `typeWindowStateChanged`, sem percorrer a árvore, ler texto, executar gestos ou manter coleta contínua. A capability de recuperar conteúdo e a de executar gestos ficam desabilitadas até as fases que implementarem essas operações.

### DEFINE — contrato da P2.1-A

Implementar exclusivamente:
1. `AndroidePyAccessibilityService` em pacote dedicado, com callbacks mínimos e sem processamento de conteúdo dos eventos.
2. XML de configuração sob `res/xml`, descrição curta para as configurações Android e filtro mínimo de mudança de janela.
3. Declaração do serviço no manifesto, protegida por `BIND_ACCESSIBILITY_SERVICE`, com intent-filter e metadata.
4. `AccessibilityGateway` application-scoped com referência volátil à instância conectada e estado de conexão observável internamente; registro apenas em `onServiceConnected()`, remoção idempotente e protegida por identidade em teardown.
5. Exposição do gateway por `AndroidePyApplication`.

Fora da P2.1-A: tela explicativa, abertura de Settings, consulta de autorização habilitada, estados públicos Kotlin/Python, erros operacionais da API, bridge `mobile.screen`, testes integrados e validação física. Esses pontos permanecem para P2.1-B/C conforme o checklist.

### Evidência e estado

A revisão documental e estática de entrada foi concluída. A implementação inicial foi publicada nos seguintes arquivos: `AndroidePyAccessibilityService.kt`, `AccessibilityGateway.kt`, `AndroidePyApplication.kt`, `AndroidManifest.xml`, `res/xml/accessibility_service_config.xml` e `res/values/strings.xml`. Commits de implementação: `9612bc04d8b03da5e2138948a9d428ae3c5c6d41`, `cbdb98fcb7fc5274cae1773d5f010e12d697b7e1`, `fc98ea2a41cf242409fab3adbc92f5cb13ff5588`, `aa61911ebbb793b87e0a8408281818971faac7cd`, `add80e659710fcff40a53b209d49b7cb7d38681e`, `7bdd832cbefd6c87b369905c21aef70f91b6ff37` e `9d5b8af5624142719a8712f39e7087b65929ef8b`.

**Conferência estática:** manifesto contém serviço exportado protegido por `BIND_ACCESSIBILITY_SERVICE`, intent-filter e metadata; XML solicita somente `typeWindowStateChanged`, declara `canRetrieveWindowContent=false` e `canPerformGestures=false`; serviço não lê eventos e registra/desregistra sua instância nos callbacks; gateway é application-scoped, usa referência volátil, estado interno `StateFlow` e limpeza protegida por identidade; nenhum `Activity`, novo worker ou runtime foi introduzido. Os arquivos remotos foram relidos após publicação.

**Build local:** Sam executou `cd android && ./gradlew :app:assembleDebug --no-daemon --max-workers=2` no Fedora em 25/11/2026. Resultado informado: `BUILD SUCCESSFUL` em 38 s; 48 tarefas (18 executadas, 30 atualizadas). Isso confirma compilação, não comportamento em runtime.

**Limites:** não há teste automatizado nem validação física registrada. O reconhecimento pelo Android, habilitação/revogação, reconexão e lifecycle em API 35 permanecem para a P2.1-C. P2.1-A está concluída quanto à implementação e ao build; a validação integrada da P2.1 permanece pendente.

## 3.9 P2.1-B — Autorização e integração: CONFERE / DEFINE / IMPLEMENTA — 25/11/2026

### CONFERE

- P2.1-A foi concluída quanto à implementação e ao build debug local; a validação física integrada continua reservada à P2.1-C.
- A UI principal é Compose em `MainScreen.kt`; a tela Biblioteca já possui espaço de configuração e usa Activity Result APIs.
- O serviço é declarado com filtro mínimo `typeWindowStateChanged`; não solicita acesso ao conteúdo de janelas nem gestos. A interrupção precisa invalidar a disponibilidade operacional sem iniciar coleta contínua.
- `AccessibilityManager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)` fornece a lista de serviços habilitados; a identidade é conferida por package/class do serviço próprio. A conexão continua determinada exclusivamente pelos callbacks reais do serviço.
- `P2_ASYNC_PROTOCOL.md` e `P2_PYTHON_API.md` exigem distinção entre autorização e conexão, códigos estáveis e catálogo Python conservador.

### DEFINE

Implementar nesta subetapa:
1. Cartão na Biblioteca explicando finalidade e ativação/desativação manual, com botão explícito para abrir `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
2. Estado Kotlin único e observável com `not_enabled`, `disconnected` e `connected`; combinar consulta do AccessibilityManager com lifecycle real, sem inferir conexão da autorização.
3. Atualizar o estado ao retornar das configurações. `onInterrupt()` invalida disponibilidade; somente o próximo evento de janela permitido restaura estado operacional. O payload do evento permanece intocado.
4. Preparar JSON interno na Android Bridge e definir códigos nativos estáveis, sem disponibilizar `mobile.screen` ou alterar o catálogo Python antes dos testes correspondentes.

### IMPLEMENTA

Implementação publicada:
- `AccessibilityGateway.kt`: recebe applicationContext, consulta a lista de serviços habilitados, publica `AccessibilityStatus` por StateFlow, separa autorização/conexão e trata interrupção.
- `AndroidePyAccessibilityService.kt`: encaminha somente o tipo do evento de janela permitido para restaurar disponibilidade após interrupção; não lê payload.
- `MainScreen.kt`: cartão de status na Biblioteca; a tela oficial de configurações só é aberta após toque explícito; status é atualizado no retorno.
- `AndroidBridgeAdapter.kt`: `getAccessibilityStatusJson()` interno, ainda não chamado pela fachada Python.
- `AccessibilityError.kt`: códigos estáveis de serviço não habilitado/desconectado/interrompido, operação não suportada, ação não suportada/rejeitada e timeout.
- `AndroidePyApplication.kt`: injeta applicationContext no gateway.

### CONFERE — revisão estática

Os arquivos publicados foram relidos. O catálogo `mobile.capabilities` permanece sem capacidades de tela; nenhum módulo público `mobile.screen` foi criado. A UI não habilita o serviço nem tenta alterar Settings sem ação do usuário. Não há leitura de conteúdo de janela, captura de tela, gesto, polling contínuo ou novo worker/runtime.

**Build local — evidência reportada por Sam:** em 25/11/2026, na branch `feat/p2-accessibility-automation`, após `git fetch origin`, `git switch feat/p2-accessibility-automation` e `git pull --ff-only` (branch atualizada, sem alterações a baixar), Sam executou em `android/` o comando `./gradlew :app:assembleDebug --no-daemon --max-workers=2`. Resultado: `BUILD SUCCESSFUL` em 13 s; 48 tarefas (1 executada, 47 atualizadas). O build foi realizado sobre o HEAD publicado `be5f91df1f868ab6f9071b3767385c3bf7f09288` (`be5f91d`).

**Limites e estado:** a compilação confirma que a implementação P2.1-B compila no ambiente Fedora, mas não comprova comportamento em runtime. Nenhum teste automatizado JVM/host nem validação física da P2.1-B foi registrado. A subetapa permanece em andamento quanto à validação integrada; P2.1-C continua responsável pelos testes e pelo Moto G06. Nenhuma API Python `mobile.screen` foi anunciada.

## 3.10 P2.1-C — Testes e validação: início — 25/11/2026

### CONFERE

- P2.1-B compila no Fedora conforme build local reportado por Sam; o resultado anterior é evidência da versão be5f91d, anterior aos ajustes de testabilidade descritos abaixo.
- A implementação inicial não tinha testes JVM específicos para o lifecycle do AccessibilityService.
- O gateway usa APIs Android (AccessibilityManager, ComponentName e callbacks de AccessibilityService), enquanto a lógica de transição entre autorização, conexão, interrupção e callbacks antigos pode ser isolada sem framework Android.
- O módulo já utiliza JUnit 4.13.2; não foi adicionada dependência de teste ou runtime.

### DEFINE

Manter a validação proporcional ao escopo P2.1: testar no JVM as regras determinísticas de estado/identidade e os metadados de erro; revisar estaticamente a integração Android; reservar o comportamento real de AccessibilityManager, habilitação, revogação e lifecycle do serviço para o Moto G06. Não adicionar Robolectric, instrumentação ou dependências externas nesta subetapa sem necessidade comprovada.

### IMPLEMENTA

- Criado AccessibilityLifecycleState.kt, reducer Kotlin puro e genérico, responsável pelas transições not_enabled, disconnected e connected, interrupção, retomada por sinal, revogação e proteção por identidade contra callbacks tardios.
- AccessibilityGateway.kt delega as transições ao reducer e serializa consulta de autorização e callbacks sob o mesmo lock, evitando que uma consulta de estado fique intercalada com a atualização de lifecycle.
- Criado AccessibilityLifecycleStateTest.kt: cobre estado inicial/autorização, conexão/interrupção/retomada, revogação, callbacks de instância antiga, desconexão e corrida entre nova conexão e callback de desconexão antigo.
- Criado AccessibilityErrorTest.kt: cobre códigos wire, política retryable e metadados de AccessibilityOperationException.
- Revisão estática do manifesto e XML: serviço declarado com BIND_ACCESSIBILITY_SERVICE, intent-filter e metadata; configuração limitada a typeWindowStateChanged, canRetrieveWindowContent=false e canPerformGestures=false. O serviço lê somente o tipo do evento e não inspeciona o payload.

### CONFERE — estado após implementação

Os arquivos Kotlin e testes foram relidos após publicação. A lógica pura é testável por JUnit local sem Robolectric. A integração Android real não é simulada por esses testes: a consulta ao registro de serviços habilitados, o bind/desbind, a interrupção do sistema e a atualização da UI precisam ser observados no dispositivo.

**Limites e pendências:** os testes JVM ainda não foram executados; os testes Python host existentes ainda não foram executados nesta subetapa; nenhum novo build foi executado após a extração do reducer e os testes. A validação física no Moto G06 permanece pendente. O build aprovado anteriormente de P2.1-B não substitui a compilação dos commits novos de P2.1-C. Nenhuma API Python mobile.screen foi criada ou anunciada.

## 3.11 P2.1-C — Execução automatizada e validação física parcial — 25/11/2026

### Testes automatizados e build

Após a correção do enum compartilhado `AccessibilityStatus`, Sam atualizou a branch `feat/p2-accessibility-automation` até o commit `552d2cd` e executou no Fedora, em `android/`:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=2
```

**Resultado reportado:** `BUILD SUCCESSFUL` em 32 s; 55 tarefas (18 executadas, 37 atualizadas). A tarefa `:app:testDebugUnitTest` foi executada e concluiu com sucesso; `:app:assembleDebug` também concluiu com sucesso. Os testes Python host executados anteriormente nesta rodada passaram em **13/13** (`unittest discover -s app/src/test/python -v`). Não foi necessário adicionar dependências.

A tentativa anterior de build havia falhado antes de executar os testes JVM devido à ausência de `AccessibilityStatus`, removido acidentalmente durante a extração do reducer. A definição foi restaurada em `AccessibilityStatus.kt`, os valores wire receberam cobertura de teste e a execução seguinte passou.

### Validação física — Moto G06 / Android 15 (API 35)

**Relato de Sam, após atualizar o APK e executar o roteiro:**
1. Ao abrir a Biblioteca, o card apresentou **“Não Habilitado”**, coerente com o serviço ainda desativado.
2. Ao abrir as configurações de Acessibilidade, o Android exigiu permitir **funcionalidades restritas** para o aplicativo antes de habilitar o serviço. Após a autorização, ao retornar à Biblioteca, o card passou a indicar **“Habilitado”**.
3. Sam desativou o serviço nas configurações do Android; ao retornar, o card voltou a **“Não Habilitado”**.

**Resultado:** PASS para o ciclo físico de consulta do estado de autorização, habilitação e revogação, com atualização observável da UI.

**Fricção de instalação reportada:** para atualizar o aplicativo, Sam precisou desativar o Play Protect. Esse fato é registrado apenas como observação do ambiente/processo de instalação. Não é requisito funcional do Androide PY e não constitui recomendação para desativar proteções do dispositivo. O requisito de permitir funcionalidades restritas foi apresentado pelo próprio Android no fluxo de habilitação do serviço.

### Limites que permanecem

- A validação física confirma autorização habilitada/desabilitada e atualização do card. Não confirma ainda o estado operacional `disconnected` em uma desconexão inesperada, interrupção/retomada, reconexão do serviço ou comportamento após reabrir/reiniciar o aplicativo.
- Não foram testados gestos, leitura de conteúdo, digitação, captura de tela, overlay ou qualquer operação da futura API `mobile.screen`; essas capacidades continuam fora do que foi implementado nesta etapa.
- O catálogo Python permanece inalterado, sem anunciar `mobile.screen`.
- A P2.1-C permanece em andamento até a verificação de reconexão e reabertura/reinicialização, ou decisão explícita de Sam para adiar esses casos. Nenhuma integração em `main` foi realizada ou autorizada.

### Evidências desta rodada

- Testes Python host: 13/13 PASS, conforme resultado anterior reportado por Sam.
- Testes JVM + build: comando acima, `BUILD SUCCESSFUL`, 32 s, 55 tarefas.
- Validação física: relato explícito de Sam no Moto G06, Android 15/API 35.
- Checklist operacional atualizado em conjunto com esta seção.


## 3.12 P2.1-C — F2: Forçar parada e comportamento do Android — 26/11/2026

### Relato físico — Moto G06 / Android 15 (API 35)

Sam iniciou o teste com o card em `Conectado`, acionou **Forçar parada** em Configurações Android → Apps → Androide PY e reabriu o aplicativo. O card permaneceu em `Não Habilitado` mesmo após 30 segundos. Sam verificou nas configurações de Acessibilidade que o Android havia desativado a autorização do serviço. Ao habilitar novamente a autorização manualmente, o card retornou a `Conectado`.

### Revisão estática do aplicativo

Foram conferidos: `AndroidManifest.xml`, `res/xml/accessibility_service_config.xml`, `AccessibilityGateway.kt`, `AndroidePyAccessibilityService.kt`, `AccessibilityLifecycleState.kt`, `AccessibilityStatus.kt`, `AndroidePyApplication.kt`, `MainActivity.kt` e `MainScreen.kt`. Não foi encontrado código que desabilite o componente, revogue a autorização de Acessibilidade ou modifique configurações seguras do Android.

### Análise e classificação

O comportamento observado é consistente com o tratamento do Android/AOSP para pacotes submetidos a force-stop: o sistema remove o serviço de acessibilidade do pacote da lista de serviços habilitados. Assim, a autorização deixa de estar habilitada; o estado `Não Habilitado` apresentado pelo app corresponde ao estado reportado pelo sistema.

**Classificação: comportamento esperado da plataforma Android, não defeito do Androide PY.** Como a autorização foi revogada pelo sistema durante o procedimento, o teste não avalia reconexão com a autorização preservada e não pode ser usado como critério de PASS/FAIL de reconexão. Não se deve tentar contornar esse comportamento nem reabilitar o serviço automaticamente: a concessão de Acessibilidade pertence ao usuário e ao sistema operacional.

**Decisão:** nenhuma alteração de código. Registrar F2 como caracterização da plataforma, não como pendência de reconexão.

### Próximo teste físico — reinicialização normal

Sam aprovou somente o teste de reinicialização normal do Moto G06, mantendo a autorização habilitada antes do reboot. Após reiniciar, desbloquear o dispositivo, abrir o Androide PY e entrar na Biblioteca; conferir se a autorização continua habilitada e se o card retorna a `Conectado` sem intervenção manual nas configurações de Acessibilidade. Aguardar até aproximadamente 30 segundos e registrar separadamente o estado da autorização e o estado do card.

O teste ainda não foi executado. Nenhuma integração em `main` foi realizada ou autorizada; a branch de trabalho continua `feat/p2-accessibility-automation`.


## 3.13 P2.1-C — Reinicialização normal — PASS físico — 26/11/2026

### Relato de Sam — Moto G06 / Android 15 (API 35)

Antes da reinicialização normal, a autorização de Acessibilidade estava ativada e o card da Biblioteca apresentava `Conectado`. Após o aparelho reiniciar, Sam observou que a autorização permaneceu ativada e que o status continuou `Conectado`.

### Resultado

**PASS** para os critérios deste teste: a autorização foi preservada pelo Android e o serviço estava conectado após o boot, sem intervenção manual nas configurações de Acessibilidade. Trata-se de evidência física reportada por Sam no Moto G06 / Android 15 (API 35), sem generalização para todos os dispositivos Android.

### Fechamento da validação P2.1-C

Combinando as evidências já registradas — testes JVM e Python host, build debug, ciclo físico de habilitação/revogação, caracterização de Force stop como comportamento esperado da plataforma e reinicialização normal PASS — a validação da fundação nativa e autorização P2.1-C está concluída no escopo acordado.

**Limites preservados:** isso não valida gestos, leitura de conteúdo, digitação, captura de tela, overlay ou a futura API pública `mobile.screen`. Essas capacidades permanecem para as subetapas funcionais posteriores da P2. Nenhuma alteração de código foi necessária nesta rodada. Nenhum merge em `main` foi realizado ou autorizado.


## 3.14 — Fechamento P2.1-C e definição aprovada da P2.2 — 2026-11-26

### Estado consolidado

A P2.1-C foi concluída no escopo acordado. Evidências reportadas por Sam:

- `./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=2`: `BUILD SUCCESSFUL` em 32 s, 55 tarefas (18 executadas/37 atualizadas).
- Testes Python host: 13/13 PASS.
- Revisão estática de manifesto, XML, lifecycle, referências do serviço, interrupção/desconexão e ausência de bloqueio da main thread.
- Moto G06/Android 15 (API 35): habilitação e revogação PASS; Force stop caracterizado como comportamento do Android, que remove a autorização; reinicialização normal PASS, mantendo autorização e card `Conectado`.

Essas evidências não validam gestos, leitura de conteúdo, digitação, captura de tela, overlay ou API pública `mobile.screen`. Nenhuma operação de automação foi anunciada no catálogo.

### Decisões aprovadas para P2.2

1. Decomposição aprovada em três subetapas: **P2.2-A — Núcleo de interação**, **P2.2-B — Captura e persistência dos mapas**, **P2.2-C — Reutilização e validação integrada**. Escopos, critérios e testes constam no checklist operacional.
2. `type_text()` será implementado inicialmente apenas para campos comuns. Campos sensíveis e ambíguos serão bloqueados. Exceções por consentimento explícito para preenchimento sensível ficam para fase futura, conforme ADR-0009.
3. Os scripts e dados atualmente armazenados são apenas de teste e podem ser descartados. Para a mudança de schema Room da P2.2-B, a estratégia destrutiva existente é preferida se exigir menos trabalho e oferecer caminho mais simples/confiável. Não será criada migração de preservação apenas para conservar esses dados. O risco de perda deverá ser comunicado antes do APK que aplicar o novo schema.
4. Saneamento documental e atualização dos estados divergentes devem ocorrer antes da implementação da P2.2-A.

### Limites de autorização e execução

A aprovação acima autoriza somente alterações documentais de saneamento nesta rodada. Não autoriza implementação de código P2.2, alteração de schema, build, instalação, merge ou alterações em `main`. A branch de trabalho continua `feat/p2-accessibility-automation`.
