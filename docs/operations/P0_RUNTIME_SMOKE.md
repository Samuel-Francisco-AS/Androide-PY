# P0 — Smoke check do runtime Python

Este documento descreve a primeira verificação de integração Kotlin/Chaquopy/Python. Ele não substitui os critérios completos de aceite da P0.

## Baseline Android de build

O módulo usa Compose BOM 2026.06.01 (Compose 1.11.4), AGP 8.13.2 e compileSdk 36. A linha Compose 1.12.0+ exige compileSdk 37 e AGP 9.2+, por isso não deve ser selecionada enquanto a baseline API 36 for mantida.

## Componentes

- Configuração Gradle: `android/app/build.gradle.kts`
- Runtime host: `android/app/src/main/java/com/samuel/androidepy/runtime/PythonRuntime.kt`
- Executor: `android/app/src/main/java/com/samuel/androidepy/execution/ScriptExecutor.kt`
- Runner: `android/app/src/main/python/runner.py`
- Módulo auxiliar: `android/app/src/main/python/main.py`

Chaquopy usa `app/src/main/python` como diretório de código Python. O runtime embarcado está configurado para Python 3.13; no host Linux, o build usa `python3.13`.

## Fluxo real implementado

1. O usuário toca em Executar na tela Compose.
2. O editor salva o script pelo `ScriptRepository` em `Dispatchers.IO`.
3. O host cria um `ScriptExecutionRequest` e envia o código persistido ao executor serial.
4. O executor chama `PythonRuntime.initialize(applicationContext)` em uma thread de trabalho.
5. O host injeta o adaptador Kotlin no pacote Python `mobile`.
6. O módulo `runner.execute()` compila e executa o código, capturando stdout, stderr e exceções em um resultado JSON.
7. O console Compose recebe os eventos e apresenta saída e falhas.

`PythonRuntime.runSmokeTest()` continua disponível para diagnóstico direto; a tela principal usa o runner de scripts editáveis.

## Teste manual inicial

No editor, execute:

```python
import main
print(main.smoke_test())
```

Resultado esperado, com a versão micro real do runtime:

```text
Androide PY runtime OK — Python 3.13.x (CPython)
```

O script default também pode ser executado para confirmar a inicialização básica:

```python
print("Olá, Androide PY!")
```

## Restrições do runtime consideradas

- Chaquopy é aplicado a exatamente um módulo do app: `:app`.
- O `minSdk` mínimo é 24.
- O interpretador Python é nativo; a baseline declara somente ABI `arm64-v8a`. Confirmar a ABI do Moto G06 no primeiro teste.
- Não é necessário instalar NDK adicional apenas para usar as bibliotecas nativas pré-compiladas do Chaquopy.
- Python host e runtime embarcado compartilham major/minor: 3.13.
- Em Android, a maior parte de `multiprocessing` não funciona por limitações de IPC; não presumir suporte a processos Python.
- `sys.stdin` retorna EOF; não há entrada interativa padrão.
- Arquivos gravados pelo Python devem ficar sob `os.environ["HOME"]`, no armazenamento privado do app, e não no diretório do código embarcado.

## Limites conhecidos

- Publicação e revisão estática não provam que o runtime inicializa; o primeiro build e teste físico são obrigatórios.
- Python-level stdout/stderr são redirecionados pelo runner para o console. Bibliotecas nativas que escrevam diretamente em file descriptors podem continuar aparecendo no Logcat e não necessariamente no console.
- O runner limita cada stream a 65.536 caracteres e preserva um indicador de truncamento.
- Exceções Python, incluindo `SystemExit`, são convertidas em resultado FAILED; operações que encerrem o processo nativamente não podem ser contidas por esse mecanismo.
- O runtime não é sandbox. Scripts são considerados código confiável do usuário.
- ADB e conexão USB com o computador não são requisitos; o APK pode ser transferido manualmente pelo fluxo do My-CT.
