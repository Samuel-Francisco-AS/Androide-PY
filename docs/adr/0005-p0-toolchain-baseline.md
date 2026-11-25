# ADR-0005: Baseline de ferramentas e dependências da P0

- **Status:** Aceito e validado na P0 (build e validação física confirmadas por relato do usuário; testes JVM e Python host aprovados)
- **Data:** 2026-11-25

## Contexto

A P0 precisa de uma matriz reproduzível para Android, Kotlin, Jetpack Compose, Chaquopy, Room e processamento de anotações. O ambiente de desenvolvimento confirmado pelo usuário é Fedora 44 x86_64, JDK 21.0.10, Gradle Wrapper 8.14.8, Android SDK Platform 36 e Build Tools 36.0.0, com Python host 3.13.18. O aplicativo será validado inicialmente em um Moto G06.

A escolha deve respeitar o SDK já instalado, evitar instalar Gradle globalmente e não exigir NDK adicional apenas para o runtime Chaquopy.

## Decisão

Adotar a seguinte baseline para a implementação P0:

| Componente | Versão/configuração | Justificativa |
|---|---|---|
| JDK de build | 21 | Disponível no ambiente; acima do mínimo do AGP |
| Gradle Wrapper | 8.14.8 | Patch de manutenção mais recente da linha 8.14; compatível com AGP 8.13.2 e remove o aviso de depreciação observado em 8.14.3 |
| Android Gradle Plugin | 8.13.2 | Linha AGP 8.13, Gradle mínimo 8.13; suporta API até 36.1 |
| Kotlin Gradle Plugin | 2.4.21 | Versão estável; compatível com Gradle 7.6.3–9.7.0 e AGP 8.5.2–9.3.1 |
| Compose Compiler Gradle Plugin | 2.4.21 | Deve acompanhar a versão do Kotlin |
| Jetpack Compose BOM | 2026.06.01 | Mapeia Compose 1.11.4; permanece antes da linha 1.12, que exige compileSdk 37 e AGP 9.2+ |
| Activity Compose | 1.13.0 | Versão estável documentada |
| Chaquopy | 17.2.0 | Compatível com AGP 7.3–9.5 e Python 3.10–3.15 |
| Python embarcado | 3.13 | Versão escolhida para o runtime Android |
| Python host | 3.13.18 | Major/minor coincide com o runtime exigido pelo Chaquopy |
| Room | 2.8.5 | Versão estável documentada |
| KSP | 2.3.12 | Compatível com KGP 2.3.20–2.4.x, AGP 8.13+ e Gradle 8.13+ |
| Kotlin Coroutines | 1.11.0 | Versão estável para execução assíncrona |
| compileSdk | 36 | Platform 36 já instalada |
| targetSdk | 36 | Alvo inicial alinhado ao SDK disponível |
| minSdk | 24 | Mínimo exigido pelo Chaquopy 17.2 |
| ABI inicial | arm64-v8a | ABI para dispositivos Android atuais; confirmar no Moto G06 durante validação física |
| JVM target | 17 | Compatibilidade de bytecode Android; o Gradle roda no JDK 21 |

## Repositórios

- Google Maven: plugins Android e dependências AndroidX.
- Maven Central: Chaquopy e dependências publicadas no Maven Central.
- Gradle Plugin Portal: resolução de plugins Gradle quando necessário.

## Consequências

### Positivas

- Usa o ambiente já instalado pelo usuário.
- Evita instalar Gradle globalmente.
- Usa versões estáveis e uma matriz de compatibilidade documentada.
- Mantém o runtime Python 3.13 alinhado ao Python host 3.13.
- Não exige NDK adicional apenas para empacotar as bibliotecas nativas pré-compiladas do Chaquopy.
- Limita o APK inicial à ABI física pretendida, evitando peso de ABIs que não serão usadas no primeiro teste.

### Custos e riscos

- O build ainda precisa ser executado no Fedora para validar a resolução real dos plugins e dependências.
- O ABI do Moto G06 deve ser confirmado na primeira validação física; se não for arm64-v8a, a configuração deverá ser revista.
- O Compose BOM 2026.09.00 não será usado enquanto o ambiente permanecer em compileSdk 36, pois resolve a linha Compose 1.12.1, que exige compileSdk 37 e AGP 9.x. A linha 1.13 alpha pode exigir compileSdk 37.1.
- A versão do Chaquopy não garante compatibilidade com qualquer pacote PyPI; dependências adicionais serão avaliadas individualmente.

## Correção após a primeira tentativa de build

A primeira execução local de `:app:testDebugUnitTest` falhou em `:app:checkDebugAarMetadata`: o BOM 2026.09.00 resolveu bibliotecas Compose 1.12.1, que exigem compileSdk 37 e AGP 9.1+. A baseline 8.13.2/API 36 não satisfaz esses requisitos. A correção é fixar o BOM 2026.06.01, que mapeia Compose 1.11.4 e antecede a mudança de requisito introduzida na linha 1.12.0. O targetSdk e minSdk permanecem inalterados.

O log também reportou que Gradle 8.14.3 será considerado deprecated por versões futuras do Kotlin Gradle Plugin e recomendou no mínimo 8.14.4. O Wrapper foi atualizado para Gradle 8.14.8, patch de manutenção da mesma linha, com SHA-256 oficial.

Esta correção é baseada no erro real observado e nas notas oficiais de Compose; precisa ser confirmada por nova execução local. Ela não equivale a build aprovado.

## Validação registrada (2026-11-25)

- O usuário informou que `:app:assembleDebug` foi concluído, o APK foi instalado e o aplicativo abriu no Moto G06. O log detalhado do comando não foi anexado; esta evidência é registrada como relato explícito.
- O usuário reportou `BUILD SUCCESSFUL` para `:app:testDebugUnitTest` (31 tarefas executadas).
- Os testes Python host foram executados com Python 3.13: 10 testes aprovados (`Ran 10 tests ... OK`).
- No Moto G06 (Android 15/API 35, ABI arm64-v8a), o usuário demonstrou runtime, stdout/stderr, exceções sem crash, `mobile.device.get_info()`, abertura de app, erro estruturado para pacote inexistente e persistência após reabertura.
- A primeira falha de compatibilidade Compose foi corrigida fixando BOM 2026.06.01 e Gradle Wrapper 8.14.8; o reteste JVM passou.
- Limitações: não há log completo de `assembleDebug`, screenshots ou logcat anexados. Rotação, leitor de tela e truncamento físico acima do limite permanecem verificações complementares.

## Fontes oficiais consultadas

- Kotlin release process: https://kotlinlang.org/docs/releases.html
- Kotlin Gradle compatibility: https://kotlinlang.org/docs/gradle-configure-project.html
- KSP quickstart: https://kotlinlang.org/docs/ksp-quickstart.html
- Android Gradle Plugin 8.13 release notes: https://developer.android.com/build/releases/agp-8-13-0-release-notes
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- Compose Material release notes (requisito compileSdk 37 a partir de 1.12.0): https://developer.android.com/jetpack/androidx/releases/compose-material
- Gradle release checksums: https://gradle.org/release-checksums/
- Compose November 2026 release: https://developer.android.com/blog/posts/jetpack-compose-november-2026-release
- Activity release notes: https://developer.android.com/jetpack/androidx/releases/activity
- Room release notes: https://developer.android.com/jetpack/androidx/releases/room
- Chaquopy version summary: https://chaquo.com/chaquopy/doc/current/versions.html
- Chaquopy Gradle plugin: https://chaquo.com/chaquopy/doc/current/android.html
- Coroutines releases: https://github.com/Kotlin/kotlinx.coroutines/releases
