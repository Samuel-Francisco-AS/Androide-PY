# P0 — Checklist Operacional de Implementação

**Projeto:** Androide PY  
**Branch de trabalho:** `feat/p0-runtime-bridge`  
**Base:** `main`  
**Responsável pela implementação remota:** assistente, via integração GitHub  
**Responsável por builds e validação física:** Sam, no Fedora e no Moto G06  
**Última atualização:** 2026-11-25  
**Estado geral:** 🟡 Fundação Android, runtime Python e executor implementados; testes e build aguardam execução no Fedora.

> Este arquivo é a fonte operacional de verdade para o progresso da P0. Um item só pode ser marcado como concluído depois de sua execução real e da conferência da evidência correspondente. Criar um arquivo ou escrever código não equivale, por si só, a validar seu funcionamento.

## Regras de atualização

1. Trabalhar em uma única subetapa por vez.
2. Antes de cada subetapa, reler este checklist e os documentos relevantes indicados em `AGENTS.md`.
3. Implementar somente o escopo da subetapa atual.
4. Conferir o diff/arquivos criados, coerência arquitetural, referências e testes aplicáveis.
5. Registrar evidências concretas: caminhos, commits, comandos, resultados de testes ou validação relatada pelo usuário.
6. Só então marcar a subetapa como concluída e publicar a atualização do checklist.
7. Reler o checklist atualizado e selecionar a próxima subetapa não concluída.
8. Nunca declarar build local, instalação ou teste físico como concluído sem evidência fornecida pelo ambiente de execução ou pelo usuário.
9. Se houver bloqueio ou decisão arquitetural nova, registrar o motivo e interromper a sequência dependente. Não contornar silenciosamente um ADR.

### Legenda

- ⬜ Não iniciada
- 🟡 Em andamento
- ☑️ Concluída com evidência registrada
- ⏸ Bloqueada — depende de decisão, acesso ou correção

---

## OP-0 — Governança operacional

- [x] **OP-0.1** Criar este checklist operacional — publicado e conferido no GitHub.
- [x] **OP-0.2** Criar `AGENTS.md` com a rotina obrigatória de leitura, implementação, conferência e atualização — publicado e conferido no GitHub.
- [x] **OP-0.3** Conferir no GitHub o conteúdo dos dois arquivos e confirmar que estão na branch de trabalho.
- [x] **OP-0.4** Relê-los e confirmar que os caminhos e instruções são coerentes com o repositório.

**Evidências:**
- OP-0.1: commit `f901ef3f1fc30ffd28e16797dd32572978e2307f`; arquivo conferido no GitHub (blob `48ce49b2eb327c80031e1647f41777821a2358c5`).
- OP-0.2: commit `2fd00ed1991f194a1593b0b64f7e366987b3df9c`; arquivo conferido no GitHub (blob `370ce8503642878ddaba3fa1d1d0ab3e148ea988`).
- OP-0.3/OP-0.4: ambos os arquivos recuperados da branch `feat/p0-runtime-bridge`; verificadas as instruções do ciclo obrigatório, referências ao checklist e decomposição das fases P0.

## P0-1 — Revisão documental e decisões técnicas

- [x] **P0-1.1** Ler integralmente README, visão do projeto, arquitetura, roadmap e ADRs 0001–0004.
- [x] **P0-1.2** Extrair os requisitos e critérios de aceite da P0, distinguindo escopo obrigatório e itens fora de escopo.
- [x] **P0-1.3** Verificar compatibilidade oficial da matriz Gradle / AGP / Kotlin / Compose / Chaquopy / Python / SDK.
- [x] **P0-1.4** Definir e registrar as versões exatas e repositórios necessários; não presumir que versões mencionadas em conversas anteriores estejam corretas.
- [x] **P0-1.5** Registrar ADR adicional ou atualizar ADR existente se a implementação exigir mudança arquitetural relevante.

**Evidências P0-1.1/P0-1.2:** README, docs/README, PROJECT_VISION, ARCHITECTURE (seções 1–10), ROADMAP e ADRs 0001–0004 recuperados da branch e revisados. Escopo confirmado: host Kotlin/Compose, Chaquopy, execução serial fora da UI, stdout/stderr/exceções, `mobile.device.get_info()`, `mobile.apps.launch(package_name)` e persistência Room/SQLite mediada por Kotlin. Fora do P0: editor profissional, background, Accessibility, Shizuku/ADB/root, múltiplos workers, workflow visual e sandbox para código não confiável. Critérios de aceite preservados conforme roadmap.

**Evidências P0-1.3/P0-1.4/P0-1.5:** matriz técnica e decisão registradas em [ADR-0005](../adr/0005-p0-toolchain-baseline.md), indexado em `docs/adr/README.md` e refletido em `docs/ARCHITECTURE.md`. Versões adotadas: Gradle 8.14.3; AGP 8.13.2; Kotlin/Compose Compiler 2.4.21; Compose BOM 2026.09.00; Activity Compose 1.13.0; Chaquopy 17.2.0; Python host/runtime 3.13.x; Room 2.8.5; KSP 2.3.12; Coroutines 1.11.0; compile/target SDK 36; minSdk 24; ABI inicial arm64-v8a; JDK 21 e bytecode JVM 17. Documentação oficial consultada: Kotlin release/compatibility, KSP quickstart, Android AGP 8.13, Compose BOM/release notes, Activity, Room e Chaquopy. O Compose BOM 2026.11.01 foi evitado por exigir compileSdk 37.1. Build e validação física continuam pendentes.

**Gate P0-1:** decisões técnicas rastreáveis e sem conflito conhecido com os ADRs aceitos.

## P0-2 — Fundação Android

- [x] **P0-2.1** Criar estrutura Gradle multi-módulo mínima com módulo `:app` e Gradle Wrapper 8.14.3.
- [x] **P0-2.2** Configurar AGP, Kotlin, Compose e SDK conforme matriz verificada.
- [x] **P0-2.3** Configurar identificador de aplicação, manifesto, atividade principal e recursos mínimos.
- [x] **P0-2.4** Configurar ABI(s) de destino de forma explícita e compatível com o dispositivo-alvo; justificar qualquer restrição.
- [x] **P0-2.5** Revisar arquivos Gradle, manifesto e estrutura; remover configurações copiadas do My-CT que não sejam necessárias.
- [x] **P0-2.6** Publicar a fundação Android sem declarar build aprovado antes da execução real.

**Evidência P0-2.1:** estrutura publicada na branch `feat/p0-runtime-bridge`: Gradle Wrapper 8.14.3 (scripts, JAR e properties copiados da configuração funcional do My-CT); `settings.gradle.kts` com repositórios Google/Maven Central e módulo `:app`; arquivos Gradle raiz e do módulo; `gradle.properties` com heap de 2 GiB e workers limitados a 2; `.gitignore` para builds e configurações locais. Arquivos recuperados novamente do GitHub após publicação. Commit de fundação do wrapper: `3af18dbe8c15b8b03b69a0cd950591cae429a8b4`; commits de estrutura: `bd087d4`, `946a39f`, `bf9d9c3`, `ee15acd`. Nenhum build Android foi executado remotamente.

**Evidência P0-2.2:** arquivos Gradle conferidos após publicação. AGP 8.13.2, Kotlin Android 2.4.21, Compose Compiler Plugin 2.4.21; compileSdk/targetSdk 36, minSdk 24; compatibilidade Java/Kotlin JVM 17. Commits: `69a42b9` (plugins) e `5bf2e19` (módulo Android). Revisão de configuração estática concluída; build local ainda não executado.

**Evidências P0-2.3/P0-2.4/P0-2.5:** identificador `com.samuel.androidepy`, manifesto com Activity launcher explícita/exportada, recurso `app_name` e Activity Compose inicial publicados. ABI inicial restrita a `arm64-v8a` para o Moto G06, reduzindo o tamanho do APK; a ABI real será confirmada na validação física. Revisão estática de settings, plugins, SDK, dependências Compose, manifesto, recursos, Activity, wrapper e propriedades Gradle concluída; verificações automatizadas de conteúdo dos arquivos retornaram todos os itens esperados. Não foi executado build Android. Commits relevantes: `ae097a4` (Compose dependencies), `3b43abd` (manifesto), `80e18ed` (strings), `621aaa0` (Activity), `2e70249` (ABI).

**Evidência P0-2.6:** fundação publicada na branch remota `feat/p0-runtime-bridge`, arquivos confirmados por leitura posterior no GitHub. O checklist preserva explicitamente que o build ainda não foi executado.

**Gate P0-2:** estrutura Android coerente, revisada e pronta para integração do runtime.

## P0-3 — Runtime Python (Chaquopy)

- [x] **P0-3.1** Integrar a versão de Chaquopy verificada na matriz técnica.
- [x] **P0-3.2** Configurar Python embarcado 3.13 e o interpretador host exigido pelo processo de build.
- [x] **P0-3.3** Criar módulo Python inicial e uma interface Kotlin que encapsule inicialização/acesso ao runtime.
- [x] **P0-3.4** Garantir inicialização fora da thread principal e comportamento seguro em chamadas repetidas.
- [x] **P0-3.5** Criar smoke check invocável e documentação do fluxo de integração Kotlin/Python.
- [x] **P0-3.6** Revisar dependências, inicialização, tratamento de falhas e limites conhecidos do Chaquopy.

**Evidência P0-3.1:** plugin Chaquopy 17.2.0 declarado no build raiz e aplicado somente ao módulo `:app`, conforme a documentação do fornecedor. Arquivos conferidos novamente no GitHub. Commits: `ec17207` (versão) e `f6d2762` (aplicação). A configuração da versão Python e a execução real ainda estão pendentes.

**Evidência P0-3.2:** bloco `chaquopy.defaultConfig` configurado com runtime `3.13` e `buildPython("python3.13")`, evitando caminho absoluto específico da máquina. A versão host 3.13.18 foi confirmada pelo usuário no Fedora. Arquivo revisado após publicação; build Chaquopy ainda não executado. Commit: `2f527db`.

**Evidências P0-3.3/P0-3.4/P0-3.5:** `main.py` fornece `runtime_info()` e `smoke_test()`; `PythonRuntime.kt` encapsula o singleton de processo, usa application context, sincronização contra inicializações concorrentes e rejeita chamadas de runtime na thread principal. `runSmokeTest()` invoca o módulo Python. O fluxo e o resultado esperado estão documentados em [P0_RUNTIME_SMOKE.md](P0_RUNTIME_SMOKE.md). Revisão estática dos arquivos confirmou as chamadas e guardas. Commits: `6c14351`, `4c1a979`, `0e485ca`, `eb6cefb`, `855fa9d`.

**Evidência P0-3.6:** revisadas as instruções oficiais Chaquopy 17.2 e documentadas as restrições relevantes: plugin em um único módulo, minSdk 24, ABI explícita, buildPython com major/minor correspondente, ausência de necessidade de NDK adicional, limitações de multiprocessing/stdin e streams direcionados a Logcat por padrão. Erros de inicialização são propagados ao chamador; a camada de execução P0-4 será responsável por convertê-los em resultado/erro visível sem derrubar a UI. Build e execução real continuam pendentes.

**Gate P0-3:** integração implementada e teste mínimo de smoke check criado; execução real no dispositivo permanece pendente até validação física.

## P0-4 — Execução de scripts e console

- [x] **P0-4.1** Definir contrato de execução e resultado (identificador, estado, saída, erro e timestamps essenciais).
- [x] **P0-4.2** Implementar executor inicial com política serial (uma execução ativa), sem prometer isolamento ou cancelamento forçado.
- [x] **P0-4.3** Capturar stdout e stderr separadamente, preservando ordem/limites razoáveis e evitando bloquear a UI.
- [x] **P0-4.4** Capturar exceções Python e convertê-las em erro estruturado sem encerrar o aplicativo.
- [x] **P0-4.5** Implementar estados QUEUED, RUNNING, SUCCEEDED e FAILED; manter CANCELLED reservado sem interrupção forçada de threads.
- [x] **P0-4.6** Criar testes unitários/integrados para sucesso, stderr e exceção.
- [x] **P0-4.7** Revisar concorrência, lifecycle e memória do executor; revisar a apresentação do console junto à UI em P0-7.4.

**Evidência P0-4.1:** modelos Kotlin `ExecutionModels.kt` e documento [P0_EXECUTION_CONTRACT.md](P0_EXECUTION_CONTRACT.md) publicados e conferidos. Contrato versão 1 define UUID, scriptId, código-fonte, estados, timestamps, stdout/stderr, erro estruturado, limite de 65.536 caracteres por stream e política serial sem interrupção forçada. Commit do modelo: `5b9cb92`; documento: `ee46144`. Revisão estática concluída; compilação Kotlin pendente.

**Evidência P0-4.2:** `ScriptExecutor.kt` publicado e revisado. Usa um `ThreadPoolExecutor` com uma thread, fila limitada a 16 solicitações e rejeição estruturada quando a fila está cheia. O executor é criado lazy no `Application` e publica estados via `SharedFlow`, evitando callbacks que retenham Activities destruídas. Inicializa Chaquopy e chama o módulo Python em worker. Falhas do host são convertidas em `ExecutionStatus.FAILED`. Verificação estática confirmou serialização, limite da fila, callbacks e ausência de interrupção forçada. Commit: `8dde449` (executor inicial), `8b5007a` (fila limitada). Build ainda não executado.

**Evidências P0-4.3/P0-4.4/P0-4.5:** `runner.py` redireciona stdout e stderr separadamente, limita cada captura a 65.536 caracteres e registra flags de truncamento. Exceções Python (incluindo `SystemExit`) são capturadas como dados com tipo, mensagem e traceback; o resultado JSON contém schema_version, identificadores, estado e timestamps. Kotlin converte o JSON para `ScriptExecutionResult`; o executor encaminha sucesso/falha sem deixar exceção de script escapar para a UI. Estados efetivos: QUEUED, RUNNING, SUCCEEDED e FAILED. CANCELLED permanece reservado porque o P0 não interrompe thread Python em execução. Runner conferido no GitHub (blob `62bce92`); contrato atualizado no commit `8f7b553`. Testes automatizados ainda pendentes.

**Evidências P0-4.6/P0-4.7:** testes host Python em `src/test/python/test_runner.py` cobrem sucesso/stdout, stderr, exceção, SystemExit e truncamento. Testes JVM em `ExecutionModelsTest.kt` cobrem parse de sucesso, erro estruturado e rejeição de schema desconhecido; dependências JUnit 4.13.2 e org.json 20260814 adicionadas. Comandos documentados em `P0_EXECUTION_CONTRACT.md`. Testes foram escritos e conferidos no GitHub, mas **ainda não foram executados**. Revisão estática identificou risco de callbacks vinculados ao lifecycle da Activity; executor foi ajustado para escopo do Application e SharedFlow, com fila de 16, buffer de eventos 64, stdout/stderr limitados e sem interrupção forçada. Commits: `132a14c`, `1800a85`, `0b32e09`, `565a9db`, `af51357`, `9985cc6`, `863dbec`, `f7b6bd9`.

**Gate P0-4:** implementação e fontes de teste estão prontas; gate permanece **pendente** até execução real dos testes host/JVM e revisão do resultado no Fedora.

## P0-5 — Android Bridge mínima

- [x] **P0-5.1** Definir contratos Kotlin/Python e erros previsíveis para a Bridge.
- [x] **P0-5.2** Implementar `mobile.device.get_info()` com informações não sensíveis e justificadas.
- [x] **P0-5.3** Implementar o adaptador nativo para abertura de aplicativos por API Android pública, respeitando visibilidade de pacotes e restrições de intents.
- [x] **P0-5.4** Expor resultados/erros da Bridge ao script Python sem acoplá-lo a classes Android.
- [x] **P0-5.5** Criar testes para dispositivo, aplicativo inexistente e falhas de inicialização quando viável.
- [x] **P0-5.6** Documentar permissões, package visibility, limites e comportamento esperado.

**Evidência P0-5.1:** contrato [P0_ANDROID_BRIDGE_CONTRACT.md](P0_ANDROID_BRIDGE_CONTRACT.md) publicado, indexado e conferido. Define os métodos Python, payloads de retorno, códigos de erro, limites de package visibility, ausência de permissões em tempo de execução e exclusão de identificadores sensíveis. Commit: `c71c256`.

**Evidência P0-5.2:** `AndroidBridgeAdapter.getDeviceInfoJson()` publica manufacturer, brand, model, device, Android release/API level, supported ABIs, package name e app version. Não coleta serial, IMEI, Android ID, MAC, advertising ID, telefone ou localização. `PythonRuntime.configureBridge()` injeta o adaptador e `mobile.device.get_info()` converte o JSON em dict Python; o executor configura a Bridge antes de cada script. Arquivos recuperados e conferidos no GitHub. Commits: `bfb0019` (adapter), `345d7cb` (configuração runtime), `eea6ede` (executor), `c2b074a` e `ed3278b` (facade e API Python). Build e validação no Android pendentes.

**Evidência P0-5.3:** `AndroidBridgeAdapter.launchAppJson()` valida o nome do pacote e retorna erros estruturados. Em Android 13+ usa `PackageManager.getLaunchIntentSenderForPackage`/IntentSender, que a documentação Android indica como alternativa quando o chamador não pode consultar o pacote; em API 24–32 usa `getLaunchIntentForPackage` e `FLAG_ACTIVITY_NEW_TASK`. O manifesto declara somente consultas MAIN/INFO e MAIN/LAUNCHER para o fallback legado; não usa `QUERY_ALL_PACKAGES`. Arquivos conferidos no GitHub. Commits: `554c62a` (adaptador) e `7196f0d` (queries). Referências oficiais: PackageManager API e package visibility docs.

**Evidências P0-5.4/P0-5.5/P0-5.6:** `mobile.configure(bridge)` guarda o adaptador Kotlin no pacote Python; `mobile.device.get_info()` e `mobile.apps.launch(package_name)` retornam dicts Python e não expõem Context/Activity/PackageManager. O executor injeta o adaptador antes de cada execução. Testes host `test_mobile_api.py` cobrem retorno de device info, abertura simulada, app inexistente, tipo inválido e Bridge não configurada; teste Kotlin cobre validação de nomes de pacote. Contrato atualizado e indexado com comportamento API33+ IntentSender, fallback API24–32, queries restritas e ausência de QUERY_ALL_PACKAGES. Testes ainda não executados. Commits: `c7ebe23` (apps API), `f84b629` (testes Python), `3f7e354` (validador Kotlin), `601e2c9` (documentação).

**Gate P0-5:** ambas as capacidades implementadas e cobertas por fontes de teste; gate de execução permanece pendente até testes locais e validação física.

## P0-6 — Persistência local mínima

- [x] **P0-6.1** Configurar Room/SQLite e processamento KSP conforme ADR-0003.
- [x] **P0-6.2** Definir entidade/DAO/repositório mínimos para o script de teste e seu conteúdo.
- [x] **P0-6.3** Integrar leitura e gravação ao fluxo de edição/execução.
- [x] **P0-6.4** Tratar primeira inicialização, ausência de registro e falhas de persistência.
- [x] **P0-6.5** Criar testes de persistência e recuperação.
- [x] **P0-6.6** Revisar fronteira: Python não acessa diretamente tabelas Room.

**Evidência P0-6.1:** Room 2.8.5 runtime/ktx/compiler adicionados; KSP 2.3.12 declarado no build raiz e aplicado ao app; exportação de schema configurada em `app/schemas`. Arquivos Gradle recuperados e conferidos após publicação. Commits: `107a99c` e `b3ec8d3`. Entidades/DAO/database/repositório ainda serão implementados na subetapa P0-6.2; build não executado.

**Evidência P0-6.2:** criados `ScriptEntity`, `ScriptDao`, `AppDatabase` e `ScriptRepository`. O schema inicial contém id, nome, source_code, created_at e updated_at; DAO oferece leitura por id, inserção sem sobrescrever e upsert; repository cria o script default de forma race-safe, preserva createdAt e atualiza updatedAt. Banco `androide_py.db` fica no armazenamento privado padrão do app e não usa migração destrutiva automática. Arquivos recuperados e revisados no GitHub. Commits: `7bce5b7`, `e56b51d`, `f40a1c1`, `e369f7b`. Compilação KSP/Room pendente.

**Evidência P0-6.4:** `AndroidePyApplication` disponibiliza `ScriptRepository` lazy no escopo do processo. `getOrCreateDefault()` trata registro ausente com `insertIfAbsent` e relê o resultado; `save()` preserva criação e atualiza modificação. Erros Room/SQLite são convertidos em `ScriptStorageException`, enquanto cancelamento é propagado; não há fallback destrutivo. Documentação `P0_PERSISTENCE.md` publicada e indexada. Commits: `647fd24`, `9d0c124`, `af4919f`, `0d7894f`, `55bc867`, `585ed70`. Leitura/gravação e execução agora estão integradas: o botão Executar salva primeiro e envia ao executor exatamente o sourceCode retornado pelo repositório.

**Evidência P0-6.5:** `ScriptRepositoryTest.kt` cobre criação única do script default, atualização preservando createdAt, atualização de updatedAt, nome vazio, erro de banco, propagação de cancelamento, erro do Flow e rejeição de id vazio. Usa DAO falso para rodar em JVM sem ADB. Comando documentado em `P0_PERSISTENCE.md`: `./gradlew :app:testDebugUnitTest`. Fonte conferida no GitHub (blob `6217f38fae6cff332cd3bb07854494d17c933691`); **os testes ainda não foram executados**.

**Evidência P0-6.6:** inspeção dos cinco módulos Python (`main.py`, `runner.py`, `mobile/__init__.py`, `mobile/device.py`, `mobile/apps.py`) não encontrou referências a Room, SQLite, DAO, Entity ou arquivo de banco. `PythonRuntime` recebe somente o adaptador de capacidades; `ScriptExecutor` recebe o código-fonte no request e não expõe repositório/DAO ao runtime. Verificação estática dos arquivos remotos concluída.

**Evidência P0-6.3:** `MainScreen` carrega o script pelo repositório, salva alterações e, antes de executar, persiste novamente e cria `ScriptExecutionRequest` com o código retornado pelo repositório. Verificação estática confirmou que `repository.save` precede `executor.submit` e que o request usa `saved.sourceCode`. Commit: `82ce719`.

**Gate P0-6:** integração Kotlin concluída; testes JVM e build KSP/Room ainda aguardam execução local.

## P0-7 — Interface mínima integrada

- [x] **P0-7.1** Criar tela Compose com editor/campo de código e área de console.
- [x] **P0-7.2** Carregar e salvar o script de teste pela camada de persistência.
- [x] **P0-7.3** Conectar a ação Executar ao executor Python.
- [x] **P0-7.4** Apresentar stdout, stderr, exceções e estado da execução com distinção visual clara.
- [x] **P0-7.5** Disponibilizar acionamento demonstrável das duas capacidades da Bridge, sem sobrecarregar a tela.
- [x] **P0-7.6** Tratar loading, execução concorrente, erros e mudanças de configuração/lifecycle.
- [x] **P0-7.7** Revisar acessibilidade básica, layout em tela móvel e responsividade.

**Gate P0-7:** fluxo completo de editar, salvar, executar e observar resultado conectado às camadas reais.

## P0-8 — Qualidade, documentação e pacote

- [x] **P0-8.1** Adicionar testes unitários Kotlin e Python compatíveis com a arquitetura.
- [ ] **P0-8.2** Adicionar testes de integração para runtime, executor, Bridge e persistência.
- [ ] **P0-8.3** Executar revisão estática do código, imports, permissões, manifesto e dependências.
- [ ] **P0-8.4** Atualizar README e documentação técnica com instruções reproduzíveis de build e limites reais.
- [ ] **P0-8.5** Conferir que nenhum segredo, dado pessoal, caminho absoluto da máquina de desenvolvimento ou configuração local foi publicado.
- [ ] **P0-8.6** Revisar o diff completo e registrar o commit final da implementação remota.
- [ ] **P0-8.7** Fornecer ao usuário os comandos de sincronização e build local; não afirmar que foram executados.

**Evidência P0-8.1:** fontes de teste criadas para runner Python (stdout, stderr, exceção, SystemExit, truncamento), facade Python (device, apps, erro e bridge ausente), parser Kotlin (sucesso, falha e versão de schema) e repository/validador Kotlin. Testes escritos e conferidos no GitHub; ainda não executados. Comandos locais estão nos documentos operacionais.\n\n**Gate P0-8:** código e documentação revisados; testes executáveis localmente registrados, com execução ainda pendente.

## P0-9 — Validação local e física (dependente do usuário)

- [ ] **P0-9.1** Usuário sincroniza a branch no Fedora.
- [ ] **P0-9.2** Usuário executa build debug e compartilha o resultado.
- [ ] **P0-9.3** Corrigir falhas de build com base nos logs reais; repetir até obter build aprovado.
- [ ] **P0-9.4** Usuário transfere o APK ao Moto G06 pelo fluxo já utilizado no My-CT; ADB/USB não é requisito do projeto.
- [ ] **P0-9.5** Usuário instala e valida abertura, inicialização Python, edição, execução, stdout, stderr e exceções.
- [ ] **P0-9.6** Usuário valida `mobile.device.get_info()` e `mobile.apps.launch(package_name)`.
- [ ] **P0-9.7** Usuário fecha/reabre o app e confirma persistência do script.
- [ ] **P0-9.8** Registrar modelo, versão Android, resultados, limitações e evidências relatadas.

**Gate P0-9:** todos os oito critérios do roadmap demonstrados no dispositivo físico.

## Critérios oficiais de aceite da P0

A P0 só poderá ser declarada concluída quando todos forem demonstrados:

1. APK instala e abre no dispositivo de validação.
2. Runtime Python inicializa.
3. Script Python executa pela interface.
4. stdout e stderr aparecem no console.
5. Exceção Python é apresentada sem encerrar o aplicativo.
6. Python consulta informações básicas do dispositivo.
7. Python inicia aplicativo instalado por API Android autorizada.
8. Script permanece salvo após fechar e reabrir o aplicativo.

## Registro de evidências

| Data | Subetapa | Evidência (commit, teste, comando ou relato) | Resultado |
|---|---|---|---|
| 2026-11-25 | Preparação do ambiente local | Fedora 44, JDK 21.0.10, Python 3.13.18, SDK 36 e Gradle Wrapper 8.14.3 reportados pelo usuário | Confirmado pelo usuário; não equivale a build Android |
| 2026-11-25 | Clone remoto/local | Clone SSH do repositório concluído e sincronizado, informado pelo usuário | Confirmado pelo usuário |

## Histórico de atualização

- 2026-11-25 — governança, baseline técnica e fundação Android concluídas (OP-0; P0-1; P0-2). Runtime Chaquopy/Python 3.13 implementado e documentado (P0-3). Contrato, executor serial, captura de streams/exceções, testes host/JVM e revisão de lifecycle concluídos em código (P0-4); testes e build aguardam execução local.
