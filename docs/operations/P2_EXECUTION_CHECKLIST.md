# P2 — Checklist operacional de implementação

**Projeto:** Androide PY  
**Branch de trabalho:** `feat/p2-accessibility-automation`  
**Base:** `main`  
**Marco:** P2 — Automação de interfaces Android  
**Data de abertura:** 2026-11-25  
**Responsável pela implementação remota:** assistente, via integração GitHub  
**Responsável por builds e validação física:** Sam, no Fedora e no Moto G06  
**Estado geral:** 🟡 P2.1-A concluída quanto à implementação e build debug local. P2.1-B com implementação publicada, revisão estática e build debug local aprovados; testes automatizados e validação física integrados permanecem para P2.1-C. P2.1-C iniciada: reducer puro de lifecycle e testes JVM criados; execução dos testes, nova compilação e validação física pendentes. P0 e P1 permanecem integradas em `main`; esta branch parte da `main` pós-P1.

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

<badge>🟡 Em andamento — implementação, revisão estática e build local aprovados; testes integrados pendentes</badge>

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

<badge>🟡 Em andamento — testes JVM criados; execução e validação física pendentes</badge>

**Escopo:** verificação integrada da fundação e do comportamento no dispositivo real.

- [x] Criar testes JVM para transições de estado, autorização/revogação, interrupção, sinal de retomada, callbacks tardios, concorrência e metadados dos erros; a lógica foi extraída para reducer Kotlin puro, sem dependência Android adicional.
- [ ] Executar os testes JVM criados e os testes host Python existentes; a integração direta com AccessibilityManager/callbacks Android permanece dependente da validação no dispositivo.
- [x] Revisar estaticamente manifesto, configuração XML, lifecycle, referências do serviço, tratamento de interrupção/desconexão e ausência de bloqueio da main thread.
- [ ] Confirmar por testes/build que a extração do reducer e a serialização do estado não introduziram regressões.
- [ ] Gerar/conferir novo build APK após os ajustes de testabilidade e validar no Moto G06: habilitar, revogar, reconectar e reabrir/reiniciar o aplicativo.
- [ ] Registrar evidências e limitações separando revisão estática, testes automatizados, build e validação física.

**Critérios de aceite:** testes executados com resultados registrados; build local conferido; habilitação, revogação, reconexão e reinicialização observadas no Moto G06; checklist e relatório revisados. Qualquer item que dependa do usuário/dispositivo permanece pendente até haver confirmação.

**Mapeia os requisitos originais:** P2.1.8 e P2.1.9; verifica os requisitos P2.1.1–P2.1.7 em conjunto.

**Gate P2.1:** fundação nativa declarada corretamente, autorização explícita, estados observáveis, lifecycle robusto e testes/validação registrados. Este gate não significa que toque, digitação, rolagem, consulta, leitura semântica ou Mapa de Coordenadas estejam implementados.

## 5. P2.2 — Prioridade A: interação e Mapa de Coordenadas

**Objetivo:** permitir interação básica confiável com interfaces Android e registrar posições reutilizáveis.

### A1 — Toques e cliques

- [ ] **P2.2.1** Definir API de toque por coordenadas em pixels e normalizadas, com validação de limites e contexto de tela.
- [ ] **P2.2.2** Implementar toque simples usando APIs de gesto do AccessibilityService (`dispatchGesture`) quando suportadas.
- [ ] **P2.2.3** Retornar resultado estruturado: aceito/enviado, falha, motivo e metadados relevantes; não prometer que o app de destino concluiu a ação apenas porque o gesto foi despachado.
- [ ] **P2.2.4** Testar bordas, coordenadas inválidas, mudança de orientação, serviço desabilitado e falha de despacho.

### A2 — Digitação

- [ ] **P2.2.5** Definir API para inserir texto em elemento focado e/ou alvo identificado, usando ações de acessibilidade apropriadas.
- [ ] **P2.2.6** Definir política para campos não editáveis, foco ausente, teclado virtual, seleção/substituição de conteúdo e texto Unicode.
- [ ] **P2.2.7** Não registrar texto digitado em logs por padrão; tratar campos de senha e dados sensíveis com restrições explícitas.
- [ ] **P2.2.8** Testar texto comum, acentos, Unicode, campo sem foco, campo somente leitura e campo sensível.
- [ ] **P2.2.9** Documentar limitações: nem todo app/campo aceita ação de texto via acessibilidade; não prometer digitação universal.

### A3 — Rolagem e gestos básicos

- [ ] **P2.2.10** Definir rolagem vertical/horizontal e parâmetros de direção, distância/duração ou gesto equivalente.
- [ ] **P2.2.11** Implementar rolagem/gesto por ações de nó quando disponíveis e fallback por gesto coordenado quando apropriado.
- [ ] **P2.2.12** Distinguir operação solicitada de confirmação de que o conteúdo realmente se moveu.
- [ ] **P2.2.13** Testar contêiner rolável, tela sem rolagem, limites superior/inferior, orientação e gesto interrompido.
- [ ] **P2.2.14** Avaliar toque longo e deslize como gestos adicionais do mesmo núcleo, sem bloquear entregas fundamentais.

### A4 — Mapa de Coordenadas (Prioridade A)

- [ ] **P2.2.15** Definir modelo de dados do mapa: identificador, nome, pontos nomeados, x/y em pixels, x/y normalizados, largura/altura de referência, orientação, densidade/insets quando necessários e observações.
- [ ] **P2.2.16** Implementar espera configurável antes de iniciar a seleção; 30 segundos é exemplo inicial, com opção de iniciar manualmente/cancelar.
- [ ] **P2.2.17** Implementar overlay translúcido de seleção, preferencialmente `TYPE_ACCESSIBILITY_OVERLAY` após validação técnica; mostrar grade/mira e feedback do ponto tocado.
- [ ] **P2.2.18** Capturar um ponto por interação e permitir nomear/salvar múltiplos pontos no mesmo mapa.
- [ ] **P2.2.19** Permitir concluir, cancelar, refazer ponto e encerrar automaticamente a camada; garantir que a camada não bloqueie a navegação após o término.
- [ ] **P2.2.20** Devolver os pontos ao Python como dados serializáveis e permitir referenciá-los pelo nome em execuções futuras.
- [ ] **P2.2.21** Definir persistência de mapas/perfis: local, versionada e sem acoplamento ao histórico de execução de scripts; avaliar repositório existente antes de criar armazenamento novo.
- [ ] **P2.2.22** Implementar operação para executar toque a partir de um ponto salvo, com checagem de compatibilidade de tela e opção explícita de coordenada normalizada.
- [ ] **P2.2.23** Testar seleção com timer, seleção imediata, cancelamento, múltiplos pontos, reinício do app, rotação e diferenças de resolução.
- [ ] **P2.2.24** Registrar limites: coordenadas não reconhecem semanticamente o botão; mudanças de layout, escala, barras do sistema, teclado, densidade ou conteúdo dinâmico podem invalidar o mapa.

**Gate P2.2:** cliques/toques, digitação e rolagem validados individualmente; mapa visual seleciona e persiste pontos reutilizáveis no Moto G06. Capacidades não testadas permanecem pendentes.

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
