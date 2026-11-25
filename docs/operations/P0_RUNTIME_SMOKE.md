# P0 — Smoke check do runtime Python

Este documento descreve a primeira verificação de integração Kotlin/Chaquopy/Python. Ele não substitui os critérios completos de aceite da P0.

## Componentes

- Configuração Gradle: `android/app/build.gradle.kts`
- Runtime host: `android/app/src/main/java/com/samuel/androidepy/runtime/PythonRuntime.kt`
- Módulo embarcado: `android/app/src/main/python/main.py`

Chaquopy usa por padrão `app/src/main/python` como diretório de código Python. O runtime embarcado está configurado para Python 3.13; no host Linux, o build procura `python3.13`, configurado explicitamente no projeto.

## Contrato inicial

1. A camada Kotlin chama `PythonRuntime.initialize(context)` em uma thread de trabalho.
2. A inicialização usa o application context e é protegida contra chamadas concorrentes/repetidas.
3. A thread principal do Android é rejeitada explicitamente.
4. Após inicializar, `PythonRuntime.runSmokeTest()` chama `main.smoke_test()`.
5. A função Python retorna uma mensagem contendo a versão do interpretador.

Resultado esperado, com a versão micro real do runtime:

```text
Androide PY runtime OK — Python 3.13.x (CPython)
```

## Fluxo de validação planejado

A tela Compose da P0 chamará a inicialização e o smoke check em um worker, exibindo o retorno no console do aplicativo. O usuário poderá gerar o APK no Fedora, transferi-lo ao Moto G06 pelo fluxo manual já utilizado no My-CT e executar o teste no próprio aparelho.

**ADB e conexão USB com o computador não são requisitos desse fluxo.**

## Restrições do runtime consideradas

- Chaquopy deve ser aplicado a exatamente um módulo do app; nesta arquitetura, somente `:app`.
- O `minSdk` mínimo é 24.
- O interpretador Python é nativo: o app declara ABI(s) explicitamente. A baseline inicial usa apenas `arm64-v8a`; confirmar no Moto G06.
- Não é necessário instalar um NDK adicional apenas para usar as bibliotecas nativas pré-compiladas do Chaquopy.
- O Python host precisa compartilhar major/minor com o Python embarcado; o projeto usa Python 3.13 em ambos.
- Em Android, a maior parte de `multiprocessing` não funciona por limitações de IPC; não presumir suporte a processos Python.
- `sys.stdin` retorna EOF; não há entrada interativa padrão neste primeiro runtime.
- Arquivos gravados pelo Python devem ficar sob `os.environ["HOME"]`, no armazenamento privado do app, e não no diretório do código embarcado.

## Limites conhecidos

- A publicação do código e a revisão estática não provam que o runtime inicializa.
- O primeiro build precisa ser executado localmente com o Gradle Wrapper.
- A inicialização e a chamada Python precisam ser confirmadas no Moto G06.
- Chaquopy direciona stdout/stderr Python para Logcat por padrão. O console próprio do aplicativo será implementado na etapa P0-4 por redirecionamento explícito dos streams durante a execução do script.
- Este smoke check não valida ainda Android Bridge, persistência, captura de exceções do usuário ou execução de scripts editáveis.
