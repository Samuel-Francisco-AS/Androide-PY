# P0 — Relatório da implementação remota

**Branch:** `feat/p0-runtime-bridge`  
**Base:** `main`  
**Snapshot comparado:** 46 arquivos alterados; `feat/p0-runtime-bridge` está à frente de `main`, sem commits atrás, conforme comparação GitHub conferida em 2026-11-25. O número de commits à frente não é fixado neste relatório porque as atualizações documentais incrementais também avançam a branch. O histórico é predominantemente composto por commits pequenos de implementação e documentação.  
**Última atualização do checklist:** `a219069e14adba02027de2200ebb96bddf9cbda8`.

## Implementado no repositório

### Host Android
- Kotlin, Jetpack Compose, Activity launcher e edge-to-edge compatível com targetSdk 36.
- Gradle Wrapper 8.14.8, AGP 8.13.2, Kotlin/Compose plugin 2.4.21; Compose BOM 2026.06.01 (Compose 1.11.4), compatível com compileSdk 36.
- Chaquopy 17.2.0 com CPython 3.13 e ABI inicial arm64-v8a.
- Application-scoped Python executor serial, fila limitada a 16 e atualizações via SharedFlow.

### Execução Python
- Runner de scripts editáveis.
- Captura separada de stdout/stderr, limite de 65.536 caracteres por stream e sinalização de truncamento.
- Exceções e SystemExit convertidos em resultados estruturados.
- Estados QUEUED, RUNNING, SUCCEEDED e FAILED; CANCELLED reservado, sem interrupção forçada.

### Android Bridge
- `mobile.device.get_info()`: informações públicas e não sensíveis do dispositivo/app.
- `mobile.apps.launch(package_name)`: abertura de app lançável, com validação e erros estruturados.
- API 33+ usa IntentSender; API 24–32 usa fallback com queries limitadas.
- Sem QUERY_ALL_PACKAGES e sem permissões Android em tempo de execução.

### Persistência e interface
- Room 2.8.5 + KSP 2.3.12.
- Script default e repositório Kotlin com tratamento de falhas.
- Python não acessa Room/SQLite diretamente.
- Editor Compose, carregar/salvar, executar, console stdout/stderr/exceção e exemplos das APIs Bridge.

### Qualidade e documentação
- Testes host Python e testes JVM Kotlin criados.
- Checklist operacional e rotina AGENTS.md.
- Contratos de execução, Bridge e persistência.
- Plano de integração e instruções de build/teste sem ADB.

## Correção publicada após a primeira tentativa de build

O primeiro comando `:app:testDebugUnitTest` executado pelo usuário falhou em `:app:checkDebugAarMetadata` porque o BOM 2026.09.00 resolveu Compose 1.12.1, que exige compileSdk 37 e AGP 9.1+. O BOM foi fixado em 2026.06.01, que mapeia Compose 1.11.4; o requisito compileSdk 37 aparece a partir da linha 1.12.0. O Gradle Wrapper foi atualizado para 8.14.8 com checksum oficial para remover o aviso de depreciação reportado em 8.14.3. AGP 8.13.2 e SDK 36 foram mantidos.

**Reteste JVM reportado pelo usuário em 2026-11-25:** `./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2` terminou com `BUILD SUCCESSFUL` em 2m29s (31 tarefas executadas). O usuário também informou que a build APK foi concluída, que instalou o APK no Moto G06 e que o aplicativo abriu. O log detalhado de `assembleDebug` e o APK não foram anexados à conversa; portanto, o resultado da build APK está registrado como relato explícito, não como log inspecionado por nós.

## Validação concluída e evidências (2026-11-25)

### Testes automatizados host

O usuário executou na raiz do repositório, com Python 3.13:

```bash
PYTHONPATH=android/app/src/main/python python3.13 -m unittest discover -s android/app/src/test/python -v
```

Resultado reportado:

```text
Ran 10 tests in 0.003s

OK
```

Os dez testes aprovados cobrem:
- API mobile: retorno de informações do dispositivo como dict, abertura bem-sucedida simulada, erro estruturado para app ausente, rejeição de pacote que não é string e falha clara quando a Bridge não foi configurada.
- Runner: limite de saída, exceção Python estruturada, captura separada de stderr, captura de stdout e tratamento de SystemExit.

### Testes JVM/Kotlin

O usuário executou `./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2` e reportou `BUILD SUCCESSFUL`, 31 tarefas executadas, em 2m29s. Isso cobre os testes JVM/Kotlin, incluindo modelos de execução, validação do nome de pacote e repositório de scripts.

### Build e validação física no Moto G06

O usuário relatou que:
- a build APK foi concluída;
- o APK foi instalado e o app abriu no Moto G06;
- scripts Python executados pela interface exibiram stdout;
- stdout e stderr foram apresentados em canais separados;
- `ValueError: falha de teste` foi exibida com traceback, sem encerrar o app;
- `mobile.device.get_info()` retornou dados do aparelho Motorola moto g06, Android 15/API 35 e ABI arm64-v8a;
- `mobile.apps.launch("com.android.settings")` abriu as Configurações do Android;
- pacote inexistente retornou erro estruturado `APP_NOT_FOUND_OR_NOT_LAUNCHABLE`, sem fechar o app;
- um script editado e executado permaneceu no editor após encerrar completamente e reabrir o app.

Esses resultados físicos foram confirmados explicitamente pelo usuário, mas screenshots, logcat e log completo do comando `assembleDebug` não foram anexados. Não devemos descrevê-los como logs diretamente inspecionados.

### Limitações e verificações adicionais

- Não foram executados testes instrumentados Android que exijam ADB, de acordo com a preferência do usuário.
- Não há evidência relatada de teste manual de rotação durante execução ou de saída acima de 65.536 caracteres no aparelho; o limite de saída foi coberto pelo teste Python host.
- Não foi feita validação com leitor de tela.
- A validação de APK/instalação é baseada no relato do usuário; os testes host e JVM têm comandos e saídas textuais reportados.
- O código continua sendo executado com o mesmo UID do aplicativo; não existe sandbox para scripts não confiáveis.
- O escopo P0 não inclui garantia de execução em segundo plano, alarmes exatos, compatibilidade irrestrita com PyPI, múltiplos workers ou controle universal de aplicativos.

## Observação sobre histórico Git

A branch contém muitos commits incrementais porque cada arquivo e atualização operacional foi publicado separadamente. A comparação GitHub conferida em 2026-11-25 registra 162 commits à frente, 0 atrás e 46 arquivos alterados. **Não recomendo merge commit direto para main.** Os testes host e JVM passaram e os oito critérios funcionais foram demonstrados conforme os relatos físicos registrados acima. Antes do merge, faça uma revisão final do diff e do estado da branch; devido aos 162 commits incrementais, prefira squash merge para manter o histórico principal limpo. A branch ainda não foi mesclada.

## Próximo passo

Validação automatizada e funcional da P0 registrada. Próximo passo: revisão final da documentação/diff/branch e decisão explícita do usuário sobre squash merge para `main`. Não mesclar sem consentimento final.
