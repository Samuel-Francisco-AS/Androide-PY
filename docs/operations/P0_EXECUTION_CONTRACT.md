# P0 — Contrato de execução de scripts

**Versão do contrato:** 1

Este contrato separa a solicitação de execução, as atualizações de estado e o resultado final. A fronteira Kotlin/Python usa apenas strings e JSON; objetos Android não são entregues diretamente ao script.

## Solicitação Kotlin

`ScriptExecutionRequest` contém:

- `executionId`: UUID gerado no host.
- `scriptId`: identificador do script (inicialmente `scratch`).
- `sourceCode`: código Python a executar.
- `queuedAtEpochMs`: timestamp Unix em milissegundos no momento do enfileiramento.

## Executor e ciclo de vida

O executor é criado de forma lazy no objeto `AndroidePyApplication`, portanto existe uma única fila por processo e ela sobrevive à recriação da Activity. Ele usa uma thread de trabalho e uma fila limitada a 16 solicitações. A fila cheia produz falha estruturada; não cresce sem limite.

As atualizações são publicadas por `SharedFlow` com buffer limitado, sem callbacks que mantenham uma Activity destruída em memória. A tela observa o fluxo enquanto está ativa. O executor não é fechado no `onDestroy` da Activity e não interrompe uma thread Python em execução.

## Estados

- `QUEUED`: aceito na fila serial.
- `RUNNING`: execução iniciada.
- `SUCCEEDED`: terminou sem exceção Python.
- `FAILED`: terminou com exceção Python ou falha do host.
- `CANCELLED`: reservado para cancelamento cooperativo; o P0 não interromperá à força uma thread Python.

O executor terá uma única thread de trabalho, garantindo no máximo uma execução Python ativa. A UI receberá atualizações pelo thread principal, mas o runtime nunca será chamado nele.

## Resultado JSON Python → Kotlin

O módulo Python `runner.execute(source_code, execution_id, script_id)` retornará uma string JSON com este formato:

```json
{
  "schema_version": 1,
  "execution_id": "uuid",
  "script_id": "scratch",
  "status": "SUCCEEDED",
  "stdout": "texto impresso",
  "stderr": "mensagem de erro escrita em stderr",
  "error": null,
  "started_at_ms": 0,
  "finished_at_ms": 0,
  "duration_ms": 0,
  "stdout_truncated": false,
  "stderr_truncated": false
}
```

Quando ocorrer uma exceção Python, `status` será `FAILED` e `error` conterá `type`, `message` e `traceback`. stdout e stderr serão preservados separadamente mesmo em falha.

## Limites de segurança e recursos

- Saída stdout e stderr será limitada a 65.536 caracteres por stream para evitar crescimento ilimitado de memória.
- O executor serial reduz concorrência, mas não é uma sandbox.
- Código do usuário é tratado como confiável nesta etapa.
- Não haverá interrupção forçada de threads Python.
- Bibliotecas nativas que escrevam diretamente nos file descriptors podem não passar pelo redirecionamento Python.
- O cancelamento de uma execução em andamento não faz parte do primeiro contrato funcional. O estado `CANCELLED` fica reservado para uma futura estratégia cooperativa.

## Testes locais sem ADB

Na raiz do repositório, os testes puros do runner podem ser executados com o Python host 3.13:

```bash
PYTHONPATH=android/app/src/main/python python3.13 -m unittest discover -s android/app/src/test/python -v
```

Os testes JVM do contrato Kotlin podem ser executados sem aparelho conectado:

```bash
cd android
./gradlew :app:testDebugUnitTest
```

## Implementação

- Modelos e parser Kotlin: `android/app/src/main/java/com/samuel/androidepy/execution/ExecutionModels.kt`.
- Executor serial: etapa P0-4.2.
- Runner e captura de streams: etapas P0-4.3/P0-4.4.
