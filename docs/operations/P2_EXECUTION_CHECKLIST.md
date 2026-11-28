# P2 — Checklist operacional de implementação

**Projeto:** Androide PY  
**Branch de trabalho:** `feat/p2-accessibility-automation`  
**Base:** `main`  
**Marco:** P2 — Automação de interfaces Android  
**Data de abertura:** 2026-11-25  
**Responsável pela implementação remota:** assistente, via integração GitHub  
**Responsável por builds e validação física:** Sam, no Fedora e no Moto G06  
**Estado geral:** P2.0 documental e P2.1-A/B/C concluídas no escopo acordado. P2.2-A fechada administrativamente com ressalvas: testes automatizados/build aprovados e validação física parcial (4 PASS; type_text() adiado). P2.2-B autorizada por Sam; decomposição registrada e B1 em andamento. P2.2-C não iniciada. Branch de trabalho: `feat/p2-accessibility-automation`; nenhum merge em `main` autorizado.

> Este checklist é a fonte operacional de verdade da P2. Publicar documentação ou código não equivale a executar testes, compilar o APK ou validar o comportamento no Moto G06. Marcar cada item somente após conferir evidências concretas.

## 1. Decisões de escopo aceitas

As decisões abaixo foram acordadas por Sam em 25/11/2026 e orientam a P2:

1. A P2 implementará automação de interfaces Android por integração nativa com `AccessibilityService`, conectada à Android Bridge e à biblioteca Python.
2. A ordem de prioridade funcional é:
   - **Prioridade A — Interação:** cliques/toques, digitação e rolagem; também inclui o Mapa de Coordenadas como ferramenta de calibração visual reutilizável.
   - **Prioridade B — Consulta e coleta:** consulta controlada da interface, obtenção de informações disponíveis e coleta de dados para variáveis Python, console e, quando houver API de arquivos autorizada, arquivos de saída.
   - **Prioridade C — Leitura e identificação:** leitura mais completa da árvore de acessibilidade e localização de elementos por atributos.
3. O **Mapa de Coordenadas** é parte da Prioridade A, não um extra futuro. Deve permitir selecionar posições visualmente sobre o aplicativo de destino e reutilizar os pontos em scripts/perfis.
4. O fluxo imaginado para o mapa inclui espera configurável (exemplo inicial: 30 segundos), abertura de uma camada translúcida sobre o app de destino, seleção por toque, registro da posição e retorno dos dados ao script. O tempo de 30 segundos é um exemplo, não um valor fixo.
5. O mapa deve permitir registrar múltiplos pontos nomeados e reutilizáveis, funcionando como ferramenta de calibração visual.
6. Coordenadas devem suportar pixels e representação normalizada (frações entre 0 e 1), registrando dimensões de referência e contexto suficiente para detectar incompatibilidades. Coordenadas normalizadas ajudam entre resoluções, mas não garantem compatibilidade entre layouts diferentes.
7. A camada de seleção poderá usar uma janela `TYPE_ACCESSIBILITY_OVERLAY` fornecida pelo AccessibilityService, sujeita à confirmação de viabilidade por versão Android e testes. Durante a seleção, a camada intercepta o toque; não deve ser descrita como transparente aos eventos.
8. Não depender de root, ADB ou Shizuku para o núcleo P2. Esses mecanismos continuam opcionais e fora deste marco.
9. O usuário concede explicitamente a autorização de Acessibilidade nas configurações Android. O aplicativo deve explicar a finalidade, expor o estado de autorização e não tentar contornar controles do sistema.
10. A automação semântica (por elementos acessíveis) e a automação geométrica (por coordenadas) são métodos complementares. Nenhum deles promete controle universal de todo app ou tela.
11. A lógica de automação e a API pública de alto nível permanecem Python-first; Kotlin implementa o serviço e os adaptadores Android. Scripts não recebem objetos Android arbitrários.
12. Dados coletados devem ser devolvidos como estruturas simples serializáveis ao Python. Saída no console faz parte do escopo B; gravação em arquivo deve reutilizar ou estender uma API de arquivos com limites e autorização explícitos, sem presumir que ela já exista.
13. O escopo será implementado incrementalmente, priorizando uma operação ponta a ponta antes de ampliar a superfície de API.

## 2. Regras operacionais

Para cada subetapa, seguir obrigatoriamente:

1. **CONFERE:** reler este checklist, AGENTS.md, arquitetura, roadmap, ADRs aplicáveis, contrato da Bridge e código relacionado.
2. **DEFINE:** registrar contrato, comportamento esperado, limitações Android, permissões, tratamento de erros e estratégia de teste antes de codificar.
3. **IMPLEMENTA:** alterar somente a subetapa selecionada e dependências estritamente necessárias.
4. **CONFERE:** revisar diff, chamadas entre Kotlin/Python, ciclo de vida, concorrência, segurança, testes e documentação.
5. **TESTA:** executar apenas testes realmente disponíveis; distinguir revisão estática, testes host/JVM, build e validação física.
6. **MARCA:** atualizar este checklist com commit, caminhos, comando e resultado observável.
7. **CONFERE:** reler o checklist publicado antes de selecionar a próxima subetapa.

Estados:
- ⬜ Pendente
- 🟡 Em andamento
- ⏸ Bloqueado
- ⚠️ Inconclusivo
- ☑️ Implementado e revisado
- 🧪 Testado automaticamente
- 📱 Validado fisicamente

Não marcar uma capacidade como validada fisicamente com base apenas em código, testes unitários ou instalação do APK.

## 3. P2.0 — Preparação, baseline e contratos

- [x] **P2.0.1** Criar branch dedicada a partir de `main`: `feat/p2-accessibility-automation`.
- [x] **P2.0.2** Criar checklist operacional e relatório da P2.
- [x] **P2.0.3** Reler integralmente AGENTS.md, README.md, docs/README.md, PROJECT_VISION.md, ARCHITECTURE.md, ROADMAP.md, ADRs 0001–0007 aplicáveis e contratos operacionais P0/P1. Revisão documental concluída em 2026-11-25; inconsistências e atualizações necessárias registradas abaixo.
- [x] **P2.0.4** Inspecionar a estrutura real da branch, AndroidManifest, AndroidBridgeAdapter, PythonRuntime, ScriptExecutor, módulos `mobile`, registro de capacidades, testes e APIs de arquivo existentes. Revisão estática concluída em 2026-11-25; nenhum serviço de acessibilidade ou API `mobile.screen` existe ainda.
- [x] **P2.0.5** Registrar baseline de SDK mínimo/alvo, confirmar versão/API real do Moto G06 via `mobile.device.get_info()`, ciclo de vida do serviço e compatibilidade das APIs oficiais utilizadas. Concluído em 2026-11-25: Gradle confirma minSdk 24/targetSdk 36/compileSdk 36; Sam executou `mobile.device.get_info()` no Moto G06 e reportou Android 15/API 35, ABI `arm64-v8a`; lifecycle e APIs conferidos na documentação oficial Android. Evidência física identificada como relato do usuário; nenhum teste/build P2 foi executado.
- [x] **P2.0.6** Definir o protocolo assíncrono Python → Kotlin → AccessibilityService → resultado. Contrato v1 publicado em `docs/operations/P2_ASYNC_PROTOCOL.md`; revisão estática concluída em 2026-11-25. Define envelope JSON versionado, request_id UUID, espera síncrona apenas no worker Python, gateway nativo assíncrono, timeout monotônico, cancelamento lógico/callbacks tardios, estados de autorização/conexão, erros estáveis, fila limitada e serialização de gestos. Nenhum código foi implementado ou testado.
- [x] **P2.0.7** Definir a API pública Python e seus contratos antes da implementação. Contrato v1 publicado em `docs/operations/P2_PYTHON_API.md`; revisão estática concluída em 2026-11-25. Define `ScreenError`, validação local, `get_status`, toque/digitação/deslize/rolagem, Mapas de Coordenadas persistentes e seus métodos de consulta/reutilização, retornos, timeouts, limites e erros. Nenhum código foi implementado ou testado.
- [x] **P2.0.8** Revisar implicações de privacidade e segurança. Revisão documental/estática concluída em 2026-11-25 e publicada em docs/operations/P2_SECURITY_REVIEW.md: abordagem B para leitura comum sob demanda; campos sensíveis bloqueados por padrão, com autorização expressa separada para leitura e escrita, limitada à execução/aplicativo; screenshots e notificações fora do escopo P2 inicial; mapas mínimos em Room privado; overlay temporário/cancelável; logs automáticos sem conteúdo; limitações do histórico P1 e ausência de sandbox documentadas. Nenhum controle foi implementado ou testado.
- [x] **P2.0.9** Registrar as decisões arquiteturais e de segurança em ADRs. Revisão documental concluída em 2026-11-25: ADR-0008 define AccessibilityService, gateway application-scoped, fronteira Python/Kotlin, lifecycle, concorrência, overlay e minimização; ADR-0009 define consentimento separado para leitura/escrita sensível, escopo por execução/aplicativo, retenção efêmera de stdout/stderr/tracebacks em execuções sensíveis por padrão e consentimento adicional para persistência, além da política de mapas e backup. Índice ADR atualizado. Nenhuma implementação/teste foi executado.
- [x] **P2.0.10** Harmonizar README, visão, arquitetura, roadmap, ADRs/índices e contratos com as decisões aprovadas e o estado real de P1/P2. Revisão documental concluída em 2026-11-25: status P1 atualizado; prioridades A/B/C e Mapa de Coordenadas incorporados ao roadmap; fronteira mobile.screen, lifecycle, protocolo assíncrono, overlay, consentimento sensível e retenção harmonizados; referências antigas de decisões abertas corrigidas. Nenhum código de produção foi alterado; nenhum teste, build APK ou validação física P2 foi executado. Evidências detalhadas na seção 3.7 do relatório P2.

**Gate P2.0:** contratos e limites técnicos definidos com base no código real e documentação Android oficial; nenhuma API pública presumida como implementada.


### P2.0.5 — Baseline Android, compatibilidade e lifecycle — 2026-11-25

**Baseline do projeto conferida no arquivo** `android/app/build.gradle.kts`:
- `minSdk = 24`; `targetSdk = 36`; `compileSdk = 36`.
- ABI configurada: `arm64-v8a`.
- JVM target 17; Chaquopy 17.2.0 com Python 3.13.

**Dispositivo físico — resultado reportado por Sam após executar `mobile.device.get_info()` no Moto G06:**
- Manufacturer/brand: Motorola.
- Model: `moto G06`; device: `lagos`.
- Android release: `15`; API level: `35`.
- ABI: `arm64-v8a`.
- App package: `com.samuel.androidepy`; app version: `0.1.0`.
- Schema version: 1.

O resultado confirma que o dispositivo de validação está acima do minSdk 24 e abaixo do targetSdk 36; o APK deve respeitar o comportamento real de API 35 no teste físico. O dado é um relato físico explícito do usuário, não uma execução observada diretamente pela integração GitHub.

**Matriz de compatibilidade:**

| Recurso | API mínima documentada | Baseline do projeto/dispositivo | Conclusão |
|---|---:|---|---|
| `AccessibilityService` e lifecycle | API 4+ | min 24 / aparelho 35 | Compatível |
| `ACTION_SET_TEXT` | API 21 | min 24 / aparelho 35 | Compatível; suporte depende do nó/app |
| `TYPE_ACCESSIBILITY_OVERLAY` | API 22 | min 24 / aparelho 35 | Compatível para overlay do serviço conectado |
| Consulta de conteúdo/árvore | API 18 para capability | min 24 / aparelho 35 | Compatível; exige `canRetrieveWindowContent` |
| `dispatchGesture()` | API 24 | min 24 / aparelho 35 | Compatível; exige `canPerformGestures` |

**Lifecycle consolidado a partir da documentação oficial:**
1. O ciclo de vida é administrado exclusivamente pelo Android. O serviço só é iniciado quando o usuário o habilita nas configurações.
2. Após o sistema realizar o bind, chama `onServiceConnected()`; esse é o ponto de inicialização de recursos e publicação do estado conectado.
3. Enquanto conectado, o sistema encaminha eventos conforme o filtro declarado; `onInterrupt()` deve tratar interrupção e `onUnbind()` representa a desconexão.
4. O serviço pode parar quando o usuário o desabilita ou quando chama `disableSelf()`.
5. A aplicação deve distinguir autorização habilitada nas configurações de conexão efetiva/disponibilidade operacional. A referência do serviço deve ser removida do gerenciador application-scoped na desconexão e também protegida contra limpeza incompleta no encerramento.
6. Não criar/instanciar o serviço manualmente nem reter Activity. O gerenciador application-scoped recebe a instância apenas pelos callbacks do ciclo de vida.

**Decisões/limites para as próximas etapas:**
- Manter minSdk 24; não há motivo de compatibilidade para elevá-lo.
- Declarar somente capacidades necessárias: `canPerformGestures` para gestos e `canRetrieveWindowContent` para consultas; evitar filtros de eventos amplos sem necessidade.
- Não solicitar screenshot, touch exploration, captura de teclas ou outras capabilities sem requisito aprovado.
- A compatibilidade por nível de API não garante suporte universal dos aplicativos: `ACTION_SET_TEXT` depende do componente; conteúdo pode não estar exposto; gestos podem ser cancelados por gestos concorrentes.
- `dispatchGesture()` cancela gestos atualmente em andamento; o protocolo deverá serializar gestos/ações incompatíveis e reportar falha de despacho separadamente de confirmação de resultado na UI.
- `TYPE_ACCESSIBILITY_OVERLAY` não exige adicionar `SYSTEM_ALERT_WINDOW` para esse uso; durante a seleção do Mapa de Coordenadas o overlay interceptará intencionalmente os toques.
- A API 35 do Moto G06 passa a ser a baseline física concreta para validação P2; targetSdk 36 permanece como configuração de compilação/alvo do app, não como versão do aparelho.

**Referências oficiais consultadas:**
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService — lifecycle administrado pelo sistema, habilitação explícita pelo usuário, bind e `onServiceConnected()`, declaração e `dispatchGesture()`.
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo — capabilities de gestos e consulta de conteúdo.
- https://developer.android.com/guide/topics/ui/accessibility/views/service — sequência dos callbacks de lifecycle, incluindo `onServiceConnected()`, `onAccessibilityEvent()`, `onInterrupt()` e `onUnbind()`.

**Verificação:** revisão estática da configuração Gradle e documentação oficial; confirmação do dispositivo por execução reportada por Sam. Nenhum teste automatizado, build APK ou nova validação física da P2 foi executado nesta subetapa.

---

### Resultado da revisão técnica inicial — 2026-11-25

**Escopo da revisão:** leitura documental de AGENTS.md, README.md, docs/README.md, PROJECT_VISION.md, ARCHITECTURE.md, ROADMAP.md, ADRs 0001–0007, contratos operacionais P0/P1 e inspeção estática dos arquivos Android/Python listados abaixo.

**Baseline Git conferida:**
- `main`: `205044caffc9df7acb94f43eb85cbeea0787d9f3`.
- `feat/p2-accessibility-automation`: `281060199fd68122ec0f0b762ccc8d7214a15ad0`.
- Comparação main → branch: 5 commits à frente, 0 atrás; somente 3 arquivos documentais alterados (`docs/README.md`, checklist P2 e relatório P2). Nenhuma alteração de código de produção na branch P2.
- Árvore remota examinada: 96 entradas, sem arquivos de AccessibilityService, configuração XML de acessibilidade ou módulo Python `mobile.screen`.

**Arquivos de código inspecionados:**
- `android/app/src/main/AndroidManifest.xml`: declara somente a Activity principal e queries limitadas para intents MAIN/INFO e MAIN/LAUNCHER. Não declara serviço de acessibilidade, permissão BIND_ACCESSIBILITY_SERVICE ou metadata XML.
- `android/app/src/main/java/com/samuel/androidepy/bridge/AndroidBridgeAdapter.kt`: adaptador recebe applicationContext e expõe somente `getDeviceInfoJson()` e `launchAppJson()`; não possui gateway/dispatcher de comandos de tela.
- `android/app/src/main/java/com/samuel/androidepy/runtime/PythonRuntime.kt`: singleton de processo Chaquopy; operações Python exigem thread de trabalho; `configureBridge(bridge: Any)` injeta o adaptador no módulo Python.
- `android/app/src/main/java/com/samuel/androidepy/execution/ScriptExecutor.kt`: executor application-scoped com um único worker Python, fila limitada a 16, atualizações via SharedFlow e configuração da Bridge antes de cada execução. A thread de execução é adequada para aguardar resultados nativos com timeout, desde que a thread principal/handler do serviço nunca aguarde o worker Python.
- `android/app/src/main/java/com/samuel/androidepy/execution/ExecutionModels.kt`: request/result serializáveis em JSON, IDs de execução e estados QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED.
- `android/app/src/main/python/mobile/__init__.py`: fachada com Bridge global configurável; erro explícito quando executada fora do executor.
- `mobile/capabilities.py`: catálogo honesto lista somente `device.get_info` e `apps.launch`.
- `mobile/device.py`, `mobile/apps.py`, `runner.py`: chamadas Kotlin via objeto Bridge, retorno JSON→dict, execução serial com captura stdout/stderr e limite de 65.536 caracteres.
- `MainScreen.kt`: tela hospeda biblioteca/editor/console e observa updates do executor; não deve ser usada como proprietária do AccessibilityService nem retida pelo serviço.
- `AndroidManifest.xml`, `app/build.gradle.kts`, `android/build.gradle.kts`, `settings.gradle.kts` e wrapper foram conferidos.
- Testes existentes: Kotlin/JVM para Bridge de apps, repositórios e parâmetros; Python host para API mobile e runner. Não há testes de acessibilidade ainda.

**Baseline de build observada no repositório:**
- Gradle Wrapper 8.14.8; AGP 8.13.2; Kotlin/Compose Compiler 2.4.21; Chaquopy 17.2.0; Python 3.13; compileSdk/targetSdk 36; minSdk 24; ABI arm64-v8a; JVM target 17; Room 2.8.5; KSP 2.3.12; Coroutines 1.11.0.
- O Moto G06 é o dispositivo físico de validação. A versão/API instalada deve ser lida novamente do aparelho por `mobile.device.get_info()`; não inferir o nível exato do dispositivo apenas pelo modelo.
- Nenhum build ou teste foi executado durante esta revisão.

**Conclusões arquiteturais preliminares para P2:**
1. O AccessibilityService deve ser instanciado e gerenciado pelo Android; o app não deve tentar construí-lo como objeto comum. O serviço publica seu estado/conexão em um gerenciador application-scoped, limpa a referência em desconexão e nunca retém Activity.
2. O `AndroidBridgeAdapter` deve receber uma interface/gateway Kotlin testável para comandos de acessibilidade, em vez de chamar diretamente uma instância de Service.
3. O worker Python serial pode aguardar um resultado nativo assíncrono com timeout/cancelamento lógico. A chamada deve ser despachada para o handler/thread principal do serviço; o callback completa uma promessa/future que acorda somente o worker. Nunca bloquear a thread principal esperando Python ou callback.
4. Cada comando precisa de requestId/correlationId, timeout, estado do serviço, erro estável e resultado serializável. Serializar gestos e ações concorrentes; a API Android `dispatchGesture()` cancela gestos que já estejam em andamento.
5. Separar `tap(x, y)` geométrico de ação semântica `click(element)`. A primeira pode ser implementada antes da árvore completa; a segunda depende de nós acessíveis.
6. Digitação não é garantida por coordenadas sozinhas. Primeiro será necessário foco em um campo editável e tentar `ACTION_SET_TEXT` no nó focado/alvo quando a ação for suportada. Nem todos os apps/campos aceitam essa ação; documentar falha e não prometer fallback universal.
7. Rolagem pode tentar ação de nó (`ACTION_SCROLL_FORWARD/BACKWARD` ou direcional) e usar gesto/swipe por coordenadas como fallback explícito.
8. Consulta/coleta deve criar snapshots DTO/dict serializáveis e limitados, não expor `AccessibilityNodeInfo` ao Python nem reter nós para uso tardio. A árvore pode estar ausente ou obsoleta quando a interface muda.
9. O Mapa de Coordenadas deve ser controlado pelo próprio AccessibilityService conectado. `TYPE_ACCESSIBILITY_OVERLAY` é candidato adequado: sobrepõe a interface e pode interceptar toques; durante seleção captura o toque e, após conclusão/cancelamento, deve ser removido. Não presumir necessidade de `SYSTEM_ALERT_WINDOW` para esse tipo de janela.
10. Guardar pixels, coordenadas normalizadas, dimensões de referência, orientação e área útil/insets quando necessário. Coordenadas normalizadas ajudam em escalas/resoluções, mas não tornam mapas compatíveis com layouts diferentes.
11. A persistência dos mapas reutilizáveis continua decisão em aberto: comparar JSON em armazenamento privado versus entidade Room; manter desacoplada de scripts/histórico de execução e não criar banco separado sem justificativa.
12. A API Python para arquivos de saída ainda não existe. O SAF de MainScreen cobre import/export de scripts, não constitui automaticamente uma API Python genérica de escrita. P2.3 deve projetar destino/permissão/limites explicitamente.

**Documentação desatualizada ou inconsistente identificada na conferência inicial P2.0.10 (corrigida e reconferida):**
- `README.md`, `docs/PROJECT_VISION.md`, `docs/ARCHITECTURE.md` e cabeçalho de `docs/ROADMAP.md` ainda dizem que P1 está em implementação ou citam a antiga branch `feat/p1-script-library`, embora P1 esteja merged na main.
- `docs/ROADMAP.md` descreve a P2 antiga sem prioridades A/B/C nem Mapa de Coordenadas.
- `docs/README.md` já indexa os documentos P2, mas sua árvore ilustrativa ainda omite os dois arquivos P2.
- `docs/adr/README.md` e ADR-0006 ainda registram validação/build P1 pendentes em trechos antigos, apesar do relatório/checklist final e merge.
- `docs/operations/P1_IMPLEMENTATION_REPORT.md` contém uma linha histórica na tabela que ainda diz PR #2 aberta em draft, contradizendo a seção final que confirma merge. Corrigir a linha obsoleta, sem apagar o histórico de testes diferidos.
- Harmonizar esses documentos após a definição dos contratos P2; não alterar a main durante a fase.

**Referências oficiais Android consultadas nesta revisão:**
- AccessibilityService: https://developer.android.com/guide/topics/ui/accessibility/service
- API AccessibilityService / `dispatchGesture()`: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- WindowManager.LayoutParams / `TYPE_ACCESSIBILITY_OVERLAY`: https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- AccessibilityNodeInfo / `ACTION_SET_TEXT` e ações de rolagem: https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo

Pontos confirmados nas referências: `dispatchGesture()` requer `canPerformGestures` e existe desde API 24; `TYPE_ACCESSIBILITY_OVERLAY` existe desde API 22 e é reservado a AccessibilityService conectado; `ACTION_SET_TEXT` existe desde API 21; consulta da árvore requer `canRetrieveWindowContent`. A documentação alerta que `typeAllMask` pode ser oneroso e que a hierarquia pode mudar/ficar obsoleta. A configuração deve solicitar somente eventos e capacidades necessários. A política de distribuição deverá ser reavaliada se o projeto futuramente buscar publicação na Play Store.

**Status na data da revisão técnica inicial:** P2.0.3 e P2.0.4 concluídas por revisão documental/estática; P2.0.5 ainda aguardava confirmação do nível Android e fechamento da análise de lifecycle. As conclusões posteriores de P2.0.5, P2.0.6 e P2.0.7 estão registradas nas respectivas seções deste checklist. Nenhuma implementação de produção P2, teste, build ou validação física de funcionalidade P2 foi realizada.

### P2.0.7 — Contrato público Python `mobile.screen` — 2026-11-25

Contrato v1 publicado em [P2_PYTHON_API.md](P2_PYTHON_API.md).

**Decisões públicas:**
- API síncrona para scripts, com implementação nativa assíncrona conforme P2.0.6.
- Sucesso retorna estruturas Python simples; falhas operacionais lançam `mobile.screen.ScreenError` (atributos `code`, `retryable`, `request_id`); argumentos inválidos usam `TypeError`/`ValueError`.
- `get_status()` distingue autorização habilitada (`enabled`) de conexão efetiva (`connected`) e estados `not_enabled`, `disconnected`, `connected`.
- Interação: `tap`, `type_text`, `swipe`, `scroll`; pixels como sistema padrão, coordenadas normalizadas como alternativa; timeouts em milissegundos. `type_text` atua somente no campo focado via `ACTION_SET_TEXT` e substitui seu conteúdo.
- Rolagem inicial é gesto geométrico ancorado no centro do display; não promete identificar contêiner.
- Mapa: `capture_coordinates`, `list_coordinate_maps`, `get_coordinate_map`, `delete_coordinate_map`, `tap_point`. Captura múltiplos pontos nomeados, persiste somente coordenadas/metadados mínimos e valida pacote/orientação conhecidos na reutilização.
- Captura não sobrescreve mapa existente; cancelamento não persiste mapa parcial. O atraso inicial é configurável e zero significa início imediato.
- `API_VERSION = 1` permanece compatível por se tratar de adição; o catálogo só anunciará capacidades após implementação e testes.

**Conferência:** contrato revisado contra a fachada Python existente, módulos `device/apps/capabilities`, runner, testes host, adaptador Kotlin, executor serial e protocolo assíncrono. A implementação atual ainda não contém `mobile.screen`; portanto, nenhuma capacidade nova foi adicionada ao catálogo.

**Evidência e limites:** commit do contrato `dc7e4673cccb1070b594805a9163d059bfdd6a41`. Revisão documental/estática; nenhum teste, build APK ou validação física foi executado. P2.0.9 e P2.0.10 foram concluídas documentalmente; P2.1 permanece pendente.

## 4. P2.1 — Fundação nativa e autorização

**Objetivo:** estabelecer a infraestrutura Android do AccessibilityService e seu ciclo de vida, sem declarar as operações de automação como funcionais.

A execução foi simplificada, por decisão de Sam em 25/11/2026, para **três subetapas no máximo**. Os nove requisitos originalmente enumerados foram preservados e redistribuídos; não são nove entregas independentes.

### P2.1-A — Fundação nativa

<badge color="success">Concluída — implementação e build</badge>

**Escopo:** serviço Android e infraestrutura application-scoped.

- [x] Implementar uma classe dedicada derivada de `AccessibilityService` e seu XML de configuração.
- [x] Declarar o serviço no manifesto com `BIND_ACCESSIBILITY_SERVICE`, intent-filter e metadata corretos; solicitar somente capabilities e eventos mínimos justificados pelo estágio atual.
- [x] Criar um gateway/gerenciador application-scoped, integrado à `AndroidePyApplication`, que publique a instância somente a partir dos callbacks reais do Android.
- [x] Implementar limpeza idempotente e protegida por identidade em `onUnbind()`/`onDestroy()`; não instanciar o serviço manualmente, não reter Activity e não criar um segundo runtime/worker Python.
- [x] Manter observação mínima: somente `typeWindowStateChanged`; sem leitura do payload, varredura de árvore, leitura de texto, captura contínua ou capacidades de gesto/consulta.

**Critérios de aceite estática:** classe, manifesto e XML coerentes; gateway application-scoped registra apenas a instância conectada pelo Android e não permite que callback tardio de instância antiga limpe uma conexão nova; referências removidas no teardown. O reconhecimento pelo Android e o lifecycle real permanecem para a validação integrada da P2.1-C.

**Evidência publicada:** implementação inicial em `AndroidePyAccessibilityService.kt`, `AccessibilityGateway.kt`, `AndroidePyApplication.kt`, `AndroidManifest.xml`, `res/xml/accessibility_service_config.xml` e `res/values/strings.xml`. Commits: `9612bc0`, `cbdb98f`, `fc98ea2`, `aa61911`, `add80e6`, `7bdd832` e `9d5b8af`. Revisão estática dos arquivos concluída. Build local executado por Sam em 25/11/2026: `cd android && ./gradlew :app:assembleDebug --no-daemon --max-workers=2` — `BUILD SUCCESSFUL` em 38 s (48 actionable tasks: 18 executed, 30 up-to-date). Nenhum teste automatizado ou validação física foi executado nesta etapa.

**Mapeia os requisitos originais:** P2.1.1, P2.1.2 e a parte de lifecycle de P2.1.5; estabelece a base técnica para P2.1.6.

### P2.1-B — Autorização e integração

<badge color="success">Implementação, revisão estática e build aprovados</badge>

**Escopo:** consentimento explícito, observabilidade e contratos operacionais.

- [x] Disponibilizar explicação de finalidade, capacidades, riscos e instruções para habilitar/desabilitar o serviço; abrir somente a tela oficial de configurações Android após ação explícita do usuário.
- [x] Expor estados distintos `not_enabled`, `disconnected` e `connected` para a camada Kotlin e preparar sua exposição pela Bridge sem ativar autorização automaticamente.
- [x] Definir comportamento mínimo de eventos/janelas de observação e garantir que a desconexão/interrupção invalide o estado operacional; não iniciar coleta contínua.
- [x] Padronizar falhas para serviço não habilitado, serviço desconectado/interrompido, operação não suportada e timeout, preservando os códigos definidos no protocolo P2.
- [x] Manter `mobile.screen` e capacidades de acessibilidade fora do catálogo Python até que as operações existam e sejam testadas.

**Critérios de aceite:** o usuário controla a autorização nas configurações Android; a UI explica a finalidade e não tenta concedê-la; estados de autorização e conexão não são confundidos; erros são estáveis; a Bridge não expõe objetos Android arbitrários.

**Evidência de implementação revisada estaticamente:** `AccessibilitySettingsCard` na biblioteca apresenta finalidade, estado, capacidades atuais, limites de coleta e riscos de acessibilidade, além de instrução de ativação/desativação manual; `Settings.ACTION_ACCESSIBILITY_SETTINGS` só é aberto pelo botão explícito. `AccessibilityGateway` consulta `AccessibilityManager.getEnabledAccessibilityServiceList()` e combina a autorização com a conexão real, expondo `StateFlow<AccessibilityStatus>` com `not_enabled`, `disconnected` e `connected`. O retorno da tela de configurações atualiza o estado. `onInterrupt()` invalida o estado operacional; o próximo evento de janela permitido restaura a disponibilidade sem ler seu payload. `AndroidBridgeAdapter.getAccessibilityStatusJson()` prepara o envelope JSON interno, sem expor ainda função Python. Códigos nativos estáveis foram definidos em `AccessibilityError.kt`. O catálogo `mobile.capabilities` não foi alterado. Arquivos: `AccessibilityGateway.kt`, `AndroidePyAccessibilityService.kt`, `AccessibilityError.kt`, `AndroidBridgeAdapter.kt`, `AndroidePyApplication.kt` e `MainScreen.kt`. Build debug local confirmado por Sam em 25/11/2026: `:app:assembleDebug --no-daemon --max-workers=2` — `BUILD SUCCESSFUL` em 13 s; 48 tarefas (1 executada, 47 atualizadas). Isso comprova compilação, não execução em runtime. Testes automatizados e validação física permanecem reservados à P2.1-C.


**Mapeia os requisitos originais:** P2.1.3, P2.1.4, P2.1.6 e P2.1.7; completa o lifecycle observável de P2.1.5.

### P2.1-C — Testes e validação

<badge color="success">Concluída no escopo acordado</badge>

**Escopo:** verificação integrada da fundação e do comportamento no dispositivo real.

- [x] Criar testes JVM para transições de estado, autorização/revogação, interrupção, sinal de retomada, callbacks tardios, concorrência, valores wire dos estados e metadados dos erros; a lógica foi extraída para reducer Kotlin puro, sem dependência Android adicional.
- [x] Executar os testes JVM criados e os testes host Python existentes. Em 25/11/2026, `:app:testDebugUnitTest` foi executado com sucesso; os testes Python host executados anteriormente nesta subetapa passaram: 13/13. A integração direta com AccessibilityManager/callbacks Android permanece dependente da validação no dispositivo.
- [x] Revisar estaticamente manifesto, configuração XML, lifecycle, referências do serviço, tratamento de interrupção/desconexão e ausência de bloqueio da main thread.
- [x] Confirmar por testes/build que a extração do reducer e a serialização do estado compilaram e passaram nos testes JVM; execução reportada por Sam em 25/11/2026.
- [x] Gerar/conferir novo build APK após os ajustes de testabilidade. Sam executou `./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=2`: `BUILD SUCCESSFUL` em 32 s, 55 tarefas (18 executadas, 37 atualizadas). No Moto G06, confirmou estado inicial `Não Habilitado`, atualização após habilitação, retorno após revogação e preservação da autorização/card `Conectado` após reinicialização normal. Force stop foi testado separadamente e classificado como revogação da autorização pelo Android.
- [x] Registrar evidências e limitações separando revisão estática, testes automatizados, build e validação física; os resultados e pendências desta rodada estão documentados na seção 3.11 do relatório P2.

**Critérios de aceite:** testes JVM/host executados e registrados; build local conferido; habilitação/revogação e reinicialização normal observadas no Moto G06; Force stop caracterizado separadamente como comportamento da plataforma; checklist e relatório revisados. Critérios atendidos no escopo acordado. Não se declara validada a reconexão após Force stop, pois o Android revoga a autorização nesse cenário.

**Mapeia os requisitos originais:** P2.1.8 e P2.1.9; verifica os requisitos P2.1.1–P2.1.7 em conjunto.

**Gate P2.1:** fundação nativa declarada corretamente, autorização explícita, estados observáveis, lifecycle robusto e testes/validação registrados. Este gate não significa que toque, digitação, rolagem, consulta, leitura semântica ou Mapa de Coordenadas estejam implementados.

## 5. P2.2 — Prioridade A: interação e Mapa de Coordenadas

**Objetivo:** permitir interação básica confiável com interfaces Android e registrar posições reutilizáveis.

### Decisões aprovadas para P2.2 — 2026-11-26

- **Decomposição:** P2.2-A (Núcleo de interação), P2.2-B (Captura e persistência dos mapas) e P2.2-C (Reutilização e validação integrada) aprovadas por Sam.
- **Digitação:** `type_text()` será inicialmente restrito a campos comuns; campos sensíveis ou ambíguos serão bloqueados. Consentimento excepcional de escrita sensível fica para fase futura, conforme ADR-0009.
- **Persistência:** scripts e dados atuais são descartáveis/de teste. Conforme ADR-0007, usar a solução destrutiva existente se for a opção de menor custo e maior simplicidade/confiabilidade; comunicar a perda potencial antes do APK com novo schema. Não criar migração de preservação sem necessidade.
- **Ordem:** concluir saneamento documental antes de iniciar a P2.2-A.
- **Autorização e limites:** em 26/11/2026 Sam autorizou explicitamente a implementação da P2.2-A na branch dedicada. A autorização não inclui alteração de schema, implementação P2.2-B/C, merge ou alteração da `main`; build, instalação e validação física continuam sob responsabilidade de Sam.

### P2.2-A — Núcleo de interação

- [x] Conferir configuração do serviço e habilitar somente `canPerformGestures` e `canRetrieveWindowContent` estritamente necessários às operações aprovadas; manter eventos mínimos. XML atualizado e revisão estática concluída.
- [x] Implementar coordenador nativo, fila limitada (16 solicitações totais), correlação por UUID, timeout monotônico, cancelamento lógico e descarte de callbacks tardios. Implementação publicada; execução automatizada ainda pendente.
- [x] Implementar `get_status`, `tap`, `swipe`, `scroll` e `type_text` na Bridge e na fachada Python, mantendo o worker serial existente. Não foi criado worker adicional.
- [x] Serializar gestos até callback terminal/teardown; não bloquear main thread nem criar executor Python adicional. O coordenador despacha no handler principal e aguarda somente no worker Python.
- [x] Restringir `type_text()` a campos comuns, focados e que aceitem `ACTION_SET_TEXT`; bloquear campos sensíveis e ambíguos, sem registrar o texto em logs. Consentimento excepcional para escrita sensível fica para fase futura.
- [ ] Testar argumentos, coordenadas, timeout incluindo fila, serviço não habilitado/desconectado, callback tardio, gesto concorrente, campo sem foco, somente leitura, senha/sensível, Unicode e limitações por aplicativo.
- [ ] Anunciar no catálogo somente cada operação que já exista e tenha testes correspondentes.

#### Registro de implementação P2.2-A — 2026-11-26

**Estado:** 🟡 implementação publicada e revisada estaticamente; testes da branch, build e validação física pendentes.

- XML do serviço agora habilita `canPerformGestures` e `canRetrieveWindowContent`; o filtro de eventos continua restrito a `typeWindowStateChanged`.
- `AccessibilityOperationCoordinator` mantém uma fila nativa limitada a 16 solicitações totais, usa `SystemClock.elapsedRealtime()` para o prazo, correlação por UUID e `CompletableFuture` aguardado exclusivamente fora da main thread. Timeout/cancelamento lógico não prometem desfazer gesto já despachado; o slot de gesto só é liberado por callback terminal ou teardown.
- `AndroidBridgeAdapter` valida o envelope JSON v1, tipos de campos e timeout inteiro finito, e devolve erros estruturados. O serviço também valida tipos do payload nativo. `AccessibilityGateway` verifica autorização/conexão e invalida pendências em interrupção/desconexão.
- `scroll(direction=...)` representa a direção do movimento do conteúdo; o dedo percorre o sentido oposto. `tap` usa um traço mínimo de 1 px para evitar um caminho de gesto de comprimento zero.
- `mobile.screen` implementa `get_status`, `tap`, `type_text`, `swipe` e `scroll`. A fachada valida tipos, coordenadas, duração e timeout.
- `type_text()` consulta somente o campo de entrada focado, verifica editabilidade, habilitação, `ACTION_SET_TEXT` e uma allow-list conservadora de `inputType`; senha, sinalização sensível, tipos numéricos/desconhecidos e classificações ambíguas são bloqueados. Não há leitura do texto atual, hint ou rótulo e o conteúdo digitado não é registrado pelo adaptador.
- Foram adicionados `test_screen_api.py` (9 testes) e `AccessibilityTextInputPolicyTest.kt` (6 testes). **Os testes ainda não foram executados no checkout da branch.** Uma verificação Python isolada da lógica da fachada foi executada separadamente, mas não substitui a suíte oficial do projeto.
- O catálogo `mobile.capabilities` permanece inalterado e não anuncia nenhuma operação `screen.*` até que os testes correspondentes sejam executados e aprovados.
- Nenhum build Gradle, APK ou teste Kotlin/JVM foi executado nesta rodada; validação física no Moto G06 permanece pendente.

**Commits de implementação publicados:** `8eda85b` (coordenador), `b2aeb3a` (política de digitação), `caa51d9` (fachada Python), `23709f3` (erros), `bb13c92` (gateway), `ca89e4d` (capabilities XML), `0723c00` (operações do serviço), `8ff5321` (Bridge), `b2fd34b` (capacidade da fila), `8cbd418` (testes Python), `b402eff` (testes JVM) e ajustes `7f36551`/`c8cecb5`.
**Gate P2.2-A:** operações básicas implementadas ponta a ponta, testes JVM/Python aprovados, build debug aprovado e validação física de toque/deslize/rolagem/digitação comum no Moto G06, com limites registrados.

### P2.2-B — Captura e persistência dos mapas

- [ ] Implementar timer configurável, captura imediata e cancelamento da sessão.
- [ ] Implementar overlay visível `TYPE_ACCESSIBILITY_OVERLAY`, controlado pelo serviço conectado, com seleção, nomeação, correção/refazer ponto, conclusão e cancelamento de múltiplos pontos.
- [ ] Garantir remoção do overlay em conclusão, cancelamento, timeout, interrupção, desconexão e teardown; nenhum mapa parcial pode ser persistido.
- [ ] Definir modelo mínimo versionado: nome/ID, data, dimensões, densidade/orientação/insets necessários, pacote em primeiro plano quando disponível e pontos nomeados em pixels/normalizados.
- [ ] Persistir por Room/DAO/repositório Kotlin; Python recebe somente DTOs serializáveis. Não criar banco separado nem exportar mapas automaticamente.
- [ ] Aplicar a decisão de dados descartáveis conforme ADR-0007: não implementar migração de preservação se fallback destrutivo for mais simples e confiável. Antes do primeiro APK com alteração de schema, avisar que scripts, versões e execuções locais podem ser apagados.
- [ ] Testar modelo, validação, transações, nomes duplicados, captura parcial/cancelamento, timeout, teardown e persistência após reabertura.

**Gate P2.2-B:** overlay e captura múltipla funcionam no Moto G06; cancelamento/erros não deixam overlay ou dados parciais; persistência local é testada; impacto destrutivo do schema foi comunicado.

### P2.2-C — Reutilização e validação integrada

- [ ] Implementar `list_coordinate_maps`, `get_coordinate_map`, `delete_coordinate_map` e `tap_point`.
- [ ] Reprojetar coordenadas normalizadas para o display atual e validar pacote/orientação/contexto quando disponível; divergências produzem erro estável.
- [ ] Testar mapas/pontos inexistentes, nomes, exclusão, resolução diferente, rotação, pacote divergente e mudanças de layout.
- [ ] Executar suíte JVM/host Python, build APK e roteiro físico completo no Moto G06; separar resultados automatizados, build e validação física.
- [ ] Atualizar relatório, checklist, arquitetura e roadmap com commits/evidências; revisar diff antes de propor qualquer integração.

**Gate P2.2-C:** API completa de mapas e reutilização validada ponta a ponta, com limitações documentadas. P2.2 somente poderá ser marcada concluída após evidências de cada subetapa.

**Restrições transversais:** não criar outro worker Python ou banco; não bloquear a main thread; não solicitar `SYSTEM_ALERT_WINDOW`; screenshots, OCR, monitoramento contínuo e leitura semântica ampla permanecem fora da P2.2.

## 6. P2.3 — Prioridade B: consulta e coleta de dados

**Objetivo:** permitir que scripts consultem informações que o Android disponibiliza por acessibilidade e utilizem os dados coletados.

- [ ] **P2.3.1** Definir snapshot limitado da janela/tela ativa e metadados permitidos.
- [ ] **P2.3.2** Implementar consulta pontual sob demanda, evitando monitoramento contínuo como padrão.
- [ ] **P2.3.3** Retornar estrutura Python serializável com campos explicitamente definidos e limites de profundidade/tamanho.
- [ ] **P2.3.4** Permitir filtrar/selecionar informações úteis sem exigir ainda o mecanismo completo da Prioridade C.
- [ ] **P2.3.5** Permitir que scripts processem os valores em variáveis e os exibam no console de execução existente.
- [ ] **P2.3.6** Avaliar exportação para arquivo usando API de arquivos existente ou proposta específica, com escolha do destino/permissão do usuário, limites e tratamento de falhas.
- [ ] **P2.3.7** Evitar coletar/armazenar senhas, conteúdo de campos sensíveis ou dados pessoais desnecessários; definir redaction e consentimento.
- [ ] **P2.3.8** Tratar telas vazias, janela indisponível, conteúdo não exposto, texto truncado, app bloqueado e timeout.
- [ ] **P2.3.9** Testar coleta no console e, se a API de arquivo for aprovada/implementada, verificar arquivo real de saída no Moto G06.

**Gate P2.3:** dados acessíveis retornam de forma previsível ao Python; console comprovado e exportação em arquivo comprovada apenas se incluída no escopo implementado.

## 7. P2.4 — Prioridade C: leitura e identificação de elementos

**Objetivo:** fornecer leitura estruturada e localização semântica mais completa.

- [ ] **P2.4.1** Definir modelo de elemento com atributos disponíveis: texto, contentDescription, className, resourceId, bounds, clickable, editable, focusable, selected, enabled e demais atributos justificados.
- [ ] **P2.4.2** Percorrer a árvore com limite de profundidade, quantidade de nós e tamanho total.
- [ ] **P2.4.3** Implementar busca por texto, descrição, identificador, classe e combinação de critérios.
- [ ] **P2.4.4** Definir identificadores/referências de elementos com validade curta e proteção contra nós obsoletos após mudança de tela.
- [ ] **P2.4.5** Integrar referências semânticas com ações de clique, digitação e rolagem quando a ação de nó for suportada.
- [ ] **P2.4.6** Retornar explicitamente quando o elemento não estiver exposto ou não puder ser acionado.
- [ ] **P2.4.7** Testar elementos duplicados, nós sem texto, árvores grandes, mudança de tela e apps com componentes customizados.
- [ ] **P2.4.8** Documentar que Canvas, jogos, conteúdo desenhado e algumas WebViews podem não expor elementos úteis.

**Gate P2.4:** leitura e busca funcionam em cenários suportados e reportam limitações sem prometer reconhecimento universal.

## 8. P2.5 — Integração, testes e validação final

- [ ] **P2.5.1** Atualizar catálogo de capacidades mobile para listar somente APIs efetivamente implementadas.
- [ ] **P2.5.2** Criar testes host Python com adaptadores falsos para contratos, serialização e erros.
- [ ] **P2.5.3** Criar testes JVM/Kotlin para serviço, ações, protocolo e lifecycle.
- [ ] **P2.5.4** Executar testes Python host e JVM; registrar comandos e resultados observados.
- [ ] **P2.5.5** Executar build debug e registrar APK/commit exato.
- [ ] **P2.5.6** Transferir/instalar no Moto G06 e validar primeiro autorização e ciclo de vida.
- [ ] **P2.5.7** Validar Prioridade A: clique, digitação, rolagem e mapa de coordenadas (pontos nomeados, persistência e reutilização).
- [ ] **P2.5.8** Validar Prioridade B: consulta, retorno Python, console e arquivo de saída se implementado.
- [ ] **P2.5.9** Validar Prioridade C: leitura e busca de elementos nos apps de teste.
- [ ] **P2.5.10** Registrar limitações por app/versão Android e não generalizar resultados de um único app.
- [ ] **P2.5.11** Revisar segurança, permissões, privacidade, consumo, estabilidade e comportamento quando o serviço é revogado.
- [ ] **P2.5.12** Atualizar relatório, checklist, documentação principal e PR com evidências; só então propor gate de integração.

**Gate P2.5:** todos os itens obrigatórios do escopo acordado possuem evidência apropriada ou são explicitamente adiados por decisão de Sam. Nenhuma capacidade pendente pode ser apresentada como aprovada.


### P2.0.6 — Protocolo assíncrono Python/Kotlin/AccessibilityService — 2026-11-25

**Contrato normativo:** [P2_ASYNC_PROTOCOL.md](P2_ASYNC_PROTOCOL.md), versão 1.

**Decisões fechadas:**
1. A API Python inicial pode ser síncrona para o autor do script; a implementação Kotlin é assíncrona internamente.
2. A chamada Python/Kotlin acontece no worker serial `AndroidePY-Python`. O adaptador pode aguardar um `CompletableFuture.get(timeout, unit)` nesse worker, nunca na main thread.
3. O gateway application-scoped encaminha comandos ao handler/thread do AccessibilityService; callbacks completam a solicitação pendente.
4. A fronteira usa JSON versionado e envelopes de request/response com `request_id` UUID, operação, payload, timeout e códigos de erro estáveis.
5. Timeout usa prazo monotônico. Solicitação expirada é terminal; callbacks tardios são descartados. Uma ação já despachada pode continuar fisicamente, sem promessa de rollback.
6. Autorização habilitada e conexão efetiva são estados diferentes. Desconexão/interrupção falham pendências e reconexão não revive solicitações antigas.
7. A fila nativa é limitada a 16 pendências; timeout inclui espera na fila. Gestos usam um único slot em andamento até callback terminal/teardown.
8. O retorno distingue despacho, conclusão da API Android e efeito efetivo no app de destino.

**Conferência técnica:** código atual de `PythonRuntime`, `ScriptExecutor`, `AndroidBridgeAdapter`, `runner.py` e fachada `mobile` revisado. Chaquopy 17.2 documenta liberação do GIL em chamadas Java. Android documenta `CompletableFuture` e `get(timeout, unit)` desde API 24, compatível com minSdk 24; não usar `completeOnTimeout` (API 31) como base do timeout.

**Evidência:** revisão estática e publicação documental. Nenhum código de produção alterado; nenhum teste automatizado, build APK ou validação física executado nesta subetapa. ADR arquitetural será registrado na P2.0.9, conforme decomposição aprovada.



### P2.0.9 — ADRs de arquitetura e segurança — 2026-11-25

**ADRs aceitos para orientar a implementação:**
- [ADR-0008 — AccessibilityService, gateway e fronteira da Android Bridge](../adr/0008-accessibility-service-bridge-and-lifecycle.md): serviço gerenciado pelo Android; gateway application-scoped; API Python como fronteira; protocolo assíncrono; fila/gestos serializados; overlay somente para captura; coleta pontual; persistência via Room/Kotlin.
- [ADR-0009 — Consentimento para dados sensíveis e retenção de saídas](../adr/0009-sensitive-data-consent-and-retention.md): campos sensíveis bloqueados por padrão; consentimentos independentes para leitura e escrita, limitados à execução e ao app; sem concessão pelo script; stdout/stderr/tracebacks não persistidos por padrão em execuções com acesso sensível; persistência exige consentimento adicional separado e explícito; mapas mínimos em Room privado e verificação de regras de backup/transferência.

**Conferência:** ADRs 0001–0007, arquitetura, visão, roadmap, contratos P2.0.6/P2.0.7 e revisão P2.0.8 consultados. Confirmado que ADR-0008 e ADR-0009 não existiam antes desta subetapa. Índice `docs/adr/README.md` atualizado.

**Evidência:** ADR-0008 commit `c6b9be89327243ed6164e7b529474d918819a3d8`; ADR-0009 commit `f73f2b0729b683786c2ef4891872472413c06870`; índice atualizado nesta subetapa. Revisão documental/estática apenas; nenhum código de produção alterado, teste executado, build APK ou validação física P2.

### P2.0.8 — Revisão de privacidade e segurança — 2026-11-25

**Documento normativo:** [P2_SECURITY_REVIEW.md](P2_SECURITY_REVIEW.md).

**Decisões consolidadas:**
1. Leitura de conteúdo comum somente sob demanda e limitada à operação solicitada; sem observação contínua ou snapshots persistidos.
2. Campos sensíveis bloqueados por padrão. A interface hospedeira deverá permitir autorização expressa, com concessões independentes para leitura e preenchimento/alteração, limitadas à execução atual e ao aplicativo de destino. Scripts não podem conceder autorização a si mesmos.
3. Classificação conservadora por sinais disponíveis; não prometer detecção perfeita nem contornar proteções do Android.
4. Screenshots e notificações fora do escopo inicial da P2.
5. Mapas armazenam somente coordenadas e metadados técnicos mínimos, usando Room/repositórios Kotlin e armazenamento privado; sem exportação/compartilhamento automático.
6. Overlay existe somente durante captura e é removido em concluir, cancelar, timeout, interrupção ou desconexão.
7. O adaptador não registra conteúdo de tela, texto digitado ou payloads. A saída que scripts imprimem pode continuar persistida pelo histórico P1; essa limitação foi identificada e não é mascarada como resolvida.
8. O produto não transmite automaticamente dados de acessibilidade. Scripts não são sandbox e podem usar a rede disponível ao processo; esse limite deve ser comunicado.

**Conferência:** manifesto, Bridge, runtime, executor, runner, Room, histórico de execução, fachada Python, catálogo de capacidades, testes existentes e ADRs 0003/0004/0006/0007 revisados. Confirmado que ainda não há serviço, overlay, gateway, configuração XML ou API mobile.screen implementados.

**Evidência:** revisão estática e publicação de P2_SECURITY_REVIEW.md; commit documental af09745bf87055dcfd3382de7cbc60bf742b1882. Nenhum código de produção alterado; nenhum teste automatizado, build APK ou validação física executado nesta subetapa. ADRs-0008 e 0009 foram publicados na P2.0.9; controles permanecem pendentes de implementação e validação.

## 9. Fora do escopo da P2

- Serviço de notificações (`NotificationListenerService`) — fase posterior.
- Agendamento, execução recorrente e eventos — fase posterior.
- Workflows/fluxos visuais e orquestração avançada — fase posterior.
- Integração obrigatória com Shizuku, ADB ou root — não faz parte do núcleo.
- OCR/captura de tela como requisito do mapa — não assumido; qualquer necessidade deve ser avaliada separadamente.
- Controle universal de jogos, apps bancários, campos protegidos, telas seguras ou componentes que bloqueiem acessibilidade.
- Execução em background irrestrita ou contorno de políticas Android.
- Coleta indiscriminada de conteúdo de tela.

## 9.1 P2.2-B — Decomposição autorizada e execução incremental — 2026-11-28

Sam autorizou iniciar a P2.2-B após o fechamento administrativo da P2.2-A. A decomposição abaixo foi registrada antes de qualquer implementação da B1. O processo obrigatório para cada subetapa permanece CONFERE → DEFINE → IMPLEMENTA → CONFERE → TESTA → MARCA → CONFERE, com conferência do planejamento entre passos.

### B1 — Modelo de dados e persistência Room
- [ ] Definir e implementar entidades Room separadas para mapas e pontos nomeados, com relação referencial e exclusão em cascata.
- [ ] Persistir somente dados mínimos: nome/identidade, timestamps, pacote de destino opcional, dimensões do display, densidade, rotação/insets e coordenadas em pixels e normalizadas.
- [ ] Criar DAO e repositório Kotlin com operações transacionais para criar/consultar/listar/excluir mapas e pontos; impedir nomes duplicados dentro do mapa.
- [ ] Integrar as novas entidades/DAO à instância Room existente, sem banco separado e sem acesso Python direto.
- [ ] Adicionar testes JVM de validação, integridade relacional, atomicidade, exclusão e conversão/limites de coordenadas.
- [ ] Documentar mudança de schema e risco de descarte local antes de qualquer instalação de APK. A política ADR-0007 permite migração destrutiva durante desenvolvimento, mas exige comunicação prévia.

**Limites B1:** não implementar overlay, sessão de captura, timer, seleção de pontos na tela, Bridge/API Python, mobile.capabilities ou tap_point. Não armazenar screenshots, texto de tela, valores de campos ou credenciais. Não declarar a captura funcional.

### B2 — Sessão de captura e overlay
- [ ] Implementar sessão explícita e temporária, timer configurável e overlay TYPE_ACCESSIBILITY_OVERLAY pertencente ao AccessibilityService conectado.
- [ ] Capturar contexto de destino antes do overlay; tratar lifecycle, cancelamento, timeout, interrupção e desconexão com remoção garantida do overlay.
- [ ] Testar estados e teardown antes da integração com seleção de pontos.

### B3 — Seleção, nomeação e conclusão do mapa
- [ ] Implementar seleção de múltiplos pontos, nomeação, correção/refazer, conclusão e cancelamento.
- [ ] Persistir somente mapas completos e válidos; nenhum dado parcial em cancelamento/falha.
- [ ] Integrar os contratos Python documentados e validar captura, consulta, exclusão e reutilização ponta a ponta no Moto G06.

**Orçamento de commits:** no máximo 5 commits para a implementação B1 nesta rodada. Agrupar mudanças coerentes; não criar commits por arquivo ou por microajuste. Se o limite ameaçar a rastreabilidade, interromper e pedir decisão em vez de ultrapassá-lo.

**Estado inicial da P2.2-B:** B1 autorizada/em andamento; B2 e B3 pendentes. Nenhuma alteração de código foi feita no momento do registro desta decomposição.

---
## 10. Riscos e pontos de atenção

1. **Autorização especial:** AccessibilityService é sensível; finalidade e capacidades solicitadas devem ser transparentes e mínimas.
2. **Ciclo de vida:** o serviço pode ser desconectado/revogado a qualquer momento; chamadas Python precisam falhar de forma controlada.
3. **Assincronia:** chamadas à Bridge não podem bloquear a thread principal nem aguardar indefinidamente.
4. **Gestos:** `dispatchGesture` e ações de nós possuem limitações de versão, janela, concorrência e app de destino; validar documentação/API antes de fixar contrato.
5. **Overlay:** confirmar compatibilidade de `TYPE_ACCESSIBILITY_OVERLAY`, coordenadas/insets e captura de toque nas versões Android suportadas.
6. **Digitação:** ações de texto variam conforme componente e aplicativo; campos sensíveis precisam de proteção.
7. **Coleta:** conteúdo da interface pode conter dados pessoais; minimizar, limitar e não persistir automaticamente.
8. **Mapas:** coordenadas podem ficar obsoletas com alterações de layout, escala, rotação, teclado e barras do sistema.
9. **Compatibilidade:** não inferir suporte universal a partir de sucesso em um aplicativo de teste.
10. **API pública:** manter métodos e retornos pequenos, documentados e testáveis; não expor classes Android ao Python.

## 11. Registro de evidências e histórico

| Data | Subetapa | Evidência/resultado | Commit |
|---|---|---|---|
| 2026-11-25 | P2.0.1–P2.0.2 | Branch criada a partir de `main`. Checklist: commit inicial `8c23d5b79a9e6d0de86a4ce4287ed6c7996af302`; relatório: `d8c2e409cf11077a8dd7da596a3380d339603eb8`; índice `docs/README.md`: `3017ad8fa0983feb2d0950b366242eae0349786c`. | Ver commits |
| 2026-11-25 | P2.0.6 | Contrato assíncrono v1 publicado em `docs/operations/P2_ASYNC_PROTOCOL.md`; checklist e relatório atualizados. Revisão estática; sem implementação, testes, build ou validação física. | Commit documental desta subetapa |
| 2026-11-25 | P2.0.8 | Revisão de privacidade e segurança publicada em `docs/operations/P2_SECURITY_REVIEW.md`; política de campos sensíveis, mapas, overlay, logs e limites residuais documentada. Revisão estática; sem implementação, testes, build ou validação física. | `af09745bf87055dcfd3382de7cbc60bf742b1882` |
| 2026-11-25 | P2.0.10 | Harmonização documental dos documentos principais, ADRs e contratos; checklist conferido. Sem alterações de produção, testes, build ou validação física. | Commit documental desta subetapa |
| 2026-11-25 | P2.1-A | Serviço, configuração XML, manifesto e gateway application-scoped publicados; revisão estática e XML PASS. Build/testes e validação física pendentes. | `9d5b8af5624142719a8712f39e7087b65929ef8b` |
| 2026-11-25 | P2.1-A | Build local informado por Sam: `:app:assembleDebug --no-daemon --max-workers=2` — `BUILD SUCCESSFUL`, 38 s, 48 tarefas (18 executadas/30 atualizadas). Nenhum teste JVM/host ou teste físico registrado. | Build local — terminal Fedora do usuário |
| 2026-11-25 | P2.1-B | UI de autorização, estado Kotlin/Bridge interno, interrupção e códigos de erro publicados; revisão estática PASS. Sam confirmou build local `:app:assembleDebug --no-daemon --max-workers=2`: `BUILD SUCCESSFUL` em 13 s; 48 tarefas (1 executada/47 atualizadas). Testes automatizados e validação física pendentes para P2.1-C. | `be5f91df1f868ab6f9071b3767385c3bf7f09288` + terminal Fedora do usuário |
| 2026-11-25 | P2.1-C | Início da validação: reducer puro de lifecycle, serialização das transições e testes JVM de estados, revogação, interrupção, callbacks tardios/concorrentes e erros adicionados; revisão estática de manifesto/XML/lifecycle. Execução dos testes, novo build e validação física ainda pendentes. | commits `e6c085e`, `9683b86`, `6ce34f0`, `86c26c5`, `c798475`, `916a81e`, `cfe96e3`, `db31478` |
| 2026-11-25 | P2.1-C | Sam executou testes JVM + build; compilação falhou antes de iniciar testes JVM por `AccessibilityStatus` ausente após extração do reducer. Causa identificada comparando com `be5f91d`: enum compartilhado não foi movido para arquivo próprio. Enum restaurado em `AccessibilityStatus.kt`, teste dos valores wire acrescentado. Testes Python host: 13/13 PASS. Reexecutar testes JVM/build após correção. | falha reportada no terminal Fedora; correção `c044818`, teste `de6271c` |
| 2026-11-25 | P2.1-C | Após a correção de `AccessibilityStatus`, Sam executou `./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=2`: `BUILD SUCCESSFUL` em 32 s; 55 tarefas (18 executadas, 37 atualizadas). Testes JVM executados com sucesso; testes Python host previamente reportados: 13/13 PASS. Validação física parcial no Moto G06: card inicialmente `Não Habilitado`; após habilitar o serviço e permitir as funcionalidades restritas exigidas pelo Android, card mudou para `Habilitado`; após desativar o serviço, voltou a `Não Habilitado`. Sam relatou que precisou desativar temporariamente o Play Protect para atualizar o APK. Isso foi registrado como fricção do processo de instalação, não como requisito do app nem como orientação de segurança. Reconexão e reabertura/reinicialização do app ainda não foram testadas. | execução local reportada por Sam; HEAD anterior `552d2cd` |
| 2026-11-26 | P2.1-C / Forçar parada (F2) | Sam iniciou com o serviço `Conectado`, forçou a parada do Androide PY nas configurações do Android e reabriu o app. O card permaneceu `Não Habilitado` após 30 s; nas configurações do sistema, Sam confirmou que a autorização do AccessibilityService havia sido revogada pelo Android. Após habilitar novamente a autorização, o card voltou a `Conectado`. Revisão estática não encontrou código do app que revogue a autorização. Investigação do comportamento da plataforma identificou que o Android remove serviços do pacote forçado a parar da lista de serviços de acessibilidade habilitados. **Classificação: comportamento esperado da plataforma; F2 não mede reconexão com autorização preservada e não é falha do Androide PY. Nenhuma correção de código necessária.** Próximo teste físico: reinicialização normal, mantendo a autorização habilitada. | relato físico de Sam + revisão estática/técnica da assistente |

Atualizações devem acrescentar linhas com caminhos, comandos, resultados e autoria da evidência. Relatos físicos do usuário devem ser identificados como tais.

## 12. Critério de encerramento da P2

A P2 só poderá ser proposta para encerramento depois de:
- Prioridade A implementada e validada no dispositivo físico, incluindo o Mapa de Coordenadas reutilizável;
- Prioridade B implementada no escopo aprovado e coleta/saída comprovada;
- Prioridade C implementada ou explicitamente reduzida/adiada por decisão de Sam;
- testes host/JVM e build registrados sem alegações não verificadas;
- permissões, privacidade, limitações e contratos documentados;
- revisão final e autorização explícita para integração em `main`.

A aprovação de um bloco não aprova automaticamente os demais.


### P2.0.10 — Conferência final documental — 2026-11-25

- [x] README e docs/README.md: estado e links P0/P1/P2 conferidos; árvore inclui P2 e ADR-0008/0009.
- [x] PROJECT_VISION.md: estado dos marcos atualizado, distinguindo contratos P2 de capacidades implementadas.
- [x] ARCHITECTURE.md: API pública, fronteira Python/Kotlin, lifecycle, gateway, overlay, persistência de mapas e segurança alinhados aos contratos/ADRs.
- [x] ROADMAP.md: P1 concluída com pendências diferidas; P2.0–P2.5 e prioridades A/B/C descritas.
- [x] ADR-0003/0006 e índice ADR alinhados às evidências finais da P1 e às decisões P2.
- [x] P2_PYTHON_API.md e P2_SECURITY_REVIEW.md não apresentam mais P2.0.8/P2.0.9 como decisões futuras/abertas.
- [x] P2_IMPLEMENTATION_REPORT.md registra P2.0.10 e mantém P2.1 pendente.

**Resultado:** revisão estática/documental aprovada. Nenhuma implementação, teste automatizado, build ou validação física P2 foi executada. A próxima subetapa é P2.1 — Fundação nativa e autorização.


### P2.1-C — F2: Forçar parada do aplicativo — comportamento da plataforma — 2026-11-26

**Roteiro e observação física — Moto G06 / Android 15 (API 35), relato de Sam:**
1. Estado inicial do card: `Conectado`.
2. Em Configurações Android → Apps → Androide PY, Sam acionou **Forçar parada**.
3. Após reabrir o Androide PY e entrar na Biblioteca, o card permaneceu em `Não Habilitado`, inclusive após aguardar 30 segundos.
4. Sam conferiu diretamente nas Configurações de Acessibilidade que a autorização do serviço havia sido desativada pelo próprio Android.
5. Ao reativar a autorização nas configurações, o card voltou a `Conectado`.

**Investigação técnica:** a revisão estática dos arquivos `AndroidManifest.xml`, `accessibility_service_config.xml`, `AccessibilityGateway.kt`, `AndroidePyAccessibilityService.kt`, `AccessibilityLifecycleState.kt`, `AccessibilityStatus.kt`, `AndroidePyApplication.kt`, `MainActivity.kt` e `MainScreen.kt` não encontrou lógica do Androide PY que desabilite o serviço, revogue a autorização ou altere configurações seguras do sistema. A análise do comportamento do Android/AOSP identificou que o tratamento de pacotes submetidos a force-stop remove o serviço correspondente da lista de serviços de acessibilidade habilitados. Isso explica a mudança observada no dispositivo.

**Classificação:** comportamento esperado do sistema Android após `Force stop`; não é evidência de falha na reconexão do Androide PY. O teste não preservou a pré-condição de autorização habilitada e, portanto, **não deve ser usado como critério de reconexão**. Não implementar tentativa de reabilitação automática: um aplicativo comum não pode conceder a própria autorização de Acessibilidade nem contornar a decisão do sistema/usuário.

**Decisão:** nenhum ajuste de código. O teste F2 fica registrado como caracterização da plataforma, fora dos critérios de PASS/FAIL de reconexão. Não recomendar desativar proteções do Android nem solicitar ADB, root ou Shizuku.

**Próximo teste físico aprovado:** reiniciar normalmente o Moto G06 pelo menu de energia, mantendo o serviço habilitado antes do reboot; após o boot, desbloquear, abrir o Androide PY e conferir se a autorização continua habilitada e se o card retorna a `Conectado` sem reativação manual nas configurações. Registrar o resultado antes de alterar qualquer estado.


### P2.1-C — Reinicialização normal — PASS físico — 2026-11-26

**Relato de Sam — Moto G06 / Android 15 (API 35):** antes da reinicialização normal, a autorização de Acessibilidade estava ativada e o card da Biblioteca apresentava `Conectado`. Após reiniciar o aparelho, a autorização continuou ativada e o card permaneceu `Conectado`.

**Resultado: PASS** para os critérios definidos: o Android preservou a autorização e o serviço estava conectado após o reboot, sem necessidade de reativação manual nas configurações. A evidência é um relato de validação física no dispositivo indicado; não deve ser generalizada para todos os fabricantes/versões Android.

**Escopo do resultado:** este teste valida a preservação da autorização e a disponibilidade após reinicialização normal. Não valida reconexão após Force stop (que foi classificado separadamente como comportamento esperado do Android), nem gestos, leitura de conteúdo, digitação, captura de tela, overlay ou API pública `mobile.screen`.

**Estado da P2.1-C:** testes JVM, testes Python host, build debug e validações físicas combinadas da fundação/autorização concluídos conforme evidências registradas. Nenhuma alteração de código foi necessária nesta validação. Não houve merge ou alteração na `main`.
