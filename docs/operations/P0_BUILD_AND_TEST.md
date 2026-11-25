# P0 — Sincronização, build e testes no Fedora

Este fluxo não usa ADB e não exige conectar o Moto G06 ao computador. O APK será transferido pelo fluxo já utilizado no My-CT.

## 1. Sincronizar a branch de implementação

O código P0 está na branch remota `feat/p0-runtime-bridge`, não em `main`.

Como o clone local foi preparado antes da criação dessa branch, o usuário copiou manualmente os quatro arquivos do Gradle Wrapper. Eles agora também são rastreados pela branch remota. Primeiro confira o estado local:

```bash
cd ~/Projetos/Androide-PY
git status --short
```

Se os únicos arquivos não rastreados forem exatamente as quatro cópias do Wrapper feitas na preparação, remova essas cópias para permitir a troca de branch:

```bash
rm -f android/gradlew
rm -f android/gradlew.bat
rm -f android/gradle/wrapper/gradle-wrapper.jar
rm -f android/gradle/wrapper/gradle-wrapper.properties
```

Não use `git clean -fd`. Se `git status` mostrar qualquer outro arquivo local que você queira preservar, pare e revise antes de remover qualquer coisa.

Depois:

```bash
git fetch origin
git switch --track -c feat/p0-runtime-bridge origin/feat/p0-runtime-bridge
chmod +x android/gradlew
git status
```

## 2. Baseline corrigida após a primeira falha

A configuração usa Compose BOM 2026.06.01 (Compose 1.11.4), compatível com a baseline compileSdk 36/AGP 8.13.2. O BOM 2026.09.00 foi removido porque resolvia Compose 1.12.1, que exige compileSdk 37 e AGP 9.1+.

O Gradle Wrapper agora é 8.14.8 e mantém a distribuição completa verificada por SHA-256 oficial.

## 3. Testes Python host

Na raiz do repositório:

```bash
PYTHONPATH=android/app/src/main/python python3.13 -m unittest discover -s android/app/src/test/python -v
```

## 4. Testes JVM Kotlin

```bash
cd android
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2
```

## 5. Gerar APK debug

Ainda em `android/`:

```bash
./gradlew :app:assembleDebug --no-daemon --max-workers=2
```

Artefato esperado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

A partir da raiz do repositório, o caminho completo será:

`android/app/build/outputs/apk/debug/app-debug.apk`

## 6. Instalar e validar sem ADB

Transfira `app-debug.apk` ao Moto G06 usando o mesmo fluxo manual que já utiliza no My-CT. Instale pelo Android e execute os cenários de [P0_INTEGRATION_TEST_PLAN.md](P0_INTEGRATION_TEST_PLAN.md).

## Recursos e diagnóstico

- JDK 21 e Python host 3.13 precisam estar no PATH/ambiente.
- O projeto usa Gradle Wrapper 8.14.8; não instale Gradle global. O arquivo `gradlew` é marcado como executável localmente com `chmod +x` após o checkout.
- O primeiro build pode baixar plugins, dependências e o runtime Python; precisa de internet.
- Builds estão limitados a dois workers e sem paralelismo de projetos para reduzir pressão de RAM.
- Não execute builds simultâneos.
- Se falhar, envie o comando exato, o primeiro bloco `What went wrong` e o restante do stacktrace relevante.
- Não declare build ou testes aprovados apenas porque os arquivos estão no GitHub.
