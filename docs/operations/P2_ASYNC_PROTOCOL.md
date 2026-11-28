# P2.0.6 — Protocolo assíncrono Python/Kotlin/AccessibilityService

**Estado:** contrato v1 implementado parcialmente na P2.2-A. A fundação do serviço, gateway e estado foram validados na P2.1; a P2.2-A adicionou coordenador/fila, despacho de gestos, timeout/cancelamento lógico, Bridge e fachada Python para operações geométricas e digitação comum. Revisão estática, testes JVM/Python host e build debug foram concluídos; validação física parcial (4 PASS), com `type_text()` adiado. A P2.2-B1 foi concluída; o contrato B2 está definido/aceito, mas sua implementação ainda não começou.
**Versão do protocolo:** 1  
**Branch:** `feat/p2-accessibility-automation`  
**Data:** 2026-11-25

## 1. Objetivo e fronteiras

Definir como uma chamada iniciada por um script Python percorre a Bridge Kotlin, alcança o AccessibilityService e retorna um resultado sem bloquear a thread principal do Android.

O contrato mantém a fronteira pública baseada em JSON e tipos simples. Scripts não recebem `Context`, `Activity`, `AccessibilityService`, `AccessibilityNodeInfo`, `GestureDescription`, `CompletableFuture` ou qualquer outro objeto Android.

## 2. Modelo de execução aprovado

A API pública Python poderá ser síncrona no primeiro corte: o script chama uma função, aguarda um resultado ou recebe uma exceção Python estável e continua. A implementação nativa será assíncrona internamente.

Fluxo:

1. O script chama a fachada Python `mobile` no worker serial existente.
2. A fachada valida argumentos, cria um `request_id` UUID e serializa a solicitação JSON.
3. O adaptador Kotlin recebe a string JSON, valida versão/campos e registra a solicitação no gateway application-scoped.
4. O gateway confirma que o serviço está habilitado e efetivamente conectado, registra o prazo e encaminha o comando para o thread do AccessibilityService por um `Handler` associado ao `Looper` principal.
5. O serviço processa a operação no seu contexto/thread apropriado. Operações com callback (por exemplo, gestos) concluem a solicitação somente quando o callback de término chega.
6. O gateway completa o resultado pendente. O adaptador aguarda esse resultado com timeout finito no worker Python e devolve uma string JSON.
7. A fachada Python converte o envelope em dicionário/valores Python simples e entrega o resultado ao script.

**Limite de threads:** a espera bloqueante é permitida somente no worker `AndroidePY-Python`. É proibido aguardar `Future.get()`, `join()`, `runBlocking` ou qualquer latch na main thread. O serviço e os callbacks Android nunca aguardam o worker Python.

Não criar outro executor Python, outro runtime Chaquopy ou uma thread Python por operação. A política continua sendo uma execução Python ativa por vez. Coroutines podem ser usadas internamente se uma etapa futura justificar, mas não são necessárias para a fronteira inicial: um gateway assíncrono baseado em `CompletableFuture` e um adaptador bloqueante fora da UI são suficientes.

## 3. Compatibilidade da espera nativa

A baseline do projeto é minSdk 24. `java.util.concurrent.CompletableFuture` e `get(timeout, unit)` estão disponíveis desde API 24, portanto atendem exatamente ao mínimo configurado. Não utilizar métodos adicionados apenas em APIs posteriores, como `completeOnTimeout` (API 31), para implementar o timeout.

O Chaquopy 17.2 documenta que o GIL é liberado quando código Python chama um método Java. Assim, a chamada Java pode aguardar o futuro sem manter o GIL ocupado. Essa espera continua proibida na main thread e deve ser limitada por timeout.

## 4. Envelope de solicitação

A fronteira Python/Kotlin utiliza uma única string JSON por chamada:

```json
{
  "schema_version": 1,
  "request_id": "uuid",
  "operation": "screen.operation",
  "payload": {},
  "timeout_ms": 10000
}
```

Regras:

- `schema_version` é obrigatório e identifica a versão do envelope, não a versão da biblioteca Python.
- `request_id` é um UUID único por chamada, criado pela fachada Python. O gateway usa-o para correlacionar callback, timeout, cancelamento e resultado.
- `operation` é um identificador estável e namespaced. O catálogo de operações concretas será fechado na P2.0.7.
- `payload` é um objeto JSON validado pela operação. Não transportar objetos Java/Android.
- `timeout_ms` é finito, positivo e explícito no envelope. A fachada pública poderá fornecer defaults por operação; a P2.0.7 definirá esses defaults e os limites públicos. O gateway calcula o prazo com relógio monotônico (`SystemClock.elapsedRealtime()`), nunca com relógio civil.
- Campos ausentes, tipos incorretos, versão desconhecida e payload inválido produzem erro estruturado; não devem gerar espera indefinida.

## 5. Envelope de resposta

Toda solicitação termina com exatamente um envelope terminal:

**Sucesso**

```json
{
  "schema_version": 1,
  "request_id": "uuid",
  "ok": true,
  "result": {},
  "error": null
}
```

**Falha**

```json
{
  "schema_version": 1,
  "request_id": "uuid",
  "ok": false,
  "result": null,
  "error": {
    "code": "SERVICE_DISCONNECTED",
    "message": "O serviço de acessibilidade foi desconectado.",
    "retryable": true
  }
}
```

O campo `result` é um objeto JSON específico da operação. `error.code` é máquina-legível e estável; `message` é explicativo e não deve ser usado como identificador lógico. `retryable` informa se uma nova tentativa pode fazer sentido, não garante sucesso.

Códigos comuns do protocolo:

| Código | Significado |
|---|---|
| `INVALID_REQUEST` | Envelope malformado, versão ou campos inválidos |
| `INVALID_PAYLOAD` | Payload inválido para a operação |
| `UNSUPPORTED_OPERATION` | Operação não reconhecida/implementada |
| `BRIDGE_NOT_CONFIGURED` | Adaptador Kotlin não configurado no runtime |
| `SERVICE_NOT_ENABLED` | Usuário não habilitou o serviço nas configurações |
| `SERVICE_DISCONNECTED` | Autorização pode existir, mas não há instância conectada |
| `SERVICE_INTERRUPTED` | Serviço recebeu interrupção e não pode concluir a solicitação |
| `QUEUE_FULL` | Limite de solicitações pendentes atingido |
| `TIMEOUT` | Prazo monotônico expirou antes de uma conclusão observável |
| `CANCELLED` | Solicitação cancelada logicamente |
| `ACTION_UNSUPPORTED` | O alvo não oferece a ação solicitada |
| `ACTION_REJECTED` | O Android/alvo rejeitou a operação |
| `INTERNAL_ERROR` | Falha nativa inesperada, sem expor detalhes internos ao script |

Operações específicas poderão definir códigos adicionais documentados sem alterar a estrutura do envelope.

## 6. Timeout, cancelamento e callbacks tardios

- O timeout inclui tempo de fila e execução; o prazo é monotônico e não pode ser renovado implicitamente.
- Ao expirar, o gateway marca a solicitação como terminal, remove-a da fila se ainda não tiver sido despachada e devolve `TIMEOUT`.
- Se a operação já tiver sido despachada ao Android, o cancelamento é lógico: o resultado/callback tardio é descartado pelo `request_id`. Não se promete desfazer uma ação já iniciada na interface.
- A conclusão usa transição atômica/uma única vez. Callback após timeout, cancelamento ou desconexão não pode completar novamente a solicitação.
- Para gestos já despachados, o slot físico de gesto permanece ocupado até callback de término ou encerramento do serviço, mesmo que o chamador já tenha recebido timeout. Isso evita despachar um segundo gesto e cancelar inadvertidamente o primeiro.
- Não interromper à força a thread Python. O timeout de uma operação nativa não equivale a cancelar a execução Python inteira.
- A API pública de cancelamento explícito de scripts/ações, se necessária, será definida em etapa própria; este contrato define a semântica interna necessária para timeout e lifecycle.

## 7. Estado de autorização e lifecycle

O gateway distingue três estados:

- `NOT_ENABLED`: o usuário ainda não habilitou o AccessibilityService.
- `DISCONNECTED`: a autorização está habilitada, mas não há instância operacional conectada.
- `CONNECTED`: o Android vinculou o serviço e `onServiceConnected()` publicou uma instância disponível.

Antes de despachar, verificar estado habilitado e conectado. Não inferir conexão apenas pela configuração de Settings. Em `onInterrupt()`, invalidar a disponibilidade operacional e falhar solicitações que não podem continuar com `SERVICE_INTERRUPTED`; a implementação inicial da P2.1-B mantém o estado `disconnected` até o próximo evento de janela permitido, sem ler seu payload. Em desconexão/unbind, limpar a referência e falhar pendências com `SERVICE_DISCONNECTED`. Uma reconexão não ressuscita requisições antigas.

O serviço é criado e destruído exclusivamente pelo Android. O gateway application-scoped não instancia o serviço, não mantém Activity e recebe/remova a referência apenas pelos callbacks do lifecycle.

## 8. Fila, concorrência e main thread

- O executor Python serial limita o fluxo normal a uma chamada ativa por vez; chamadas sequenciais de um script aguardam sequencialmente.
- O gateway mantém uma fila nativa limitada (limite inicial: 16 solicitações pendentes, alinhado à capacidade operacional já usada pelo executor) e rejeita excesso com `QUEUE_FULL`. Timeout conta enquanto a solicitação aguarda na fila.
- O serviço processa comandos no thread principal por meio do seu handler. O handler nunca espera pelo Python.
- Gestos são estritamente serializados: apenas um `dispatchGesture()` fica em andamento por vez, pois um novo despacho pode cancelar um gesto anterior. A fila só libera o próximo gesto após callback terminal ou teardown do serviço.
- Ações síncronas de nós e gestos assíncronos devem convergir para o mesmo coordenador de operações, para preservar ordenação e lifecycle.
- Nenhuma operação pesada, parsing volumoso ou espera bloqueante deve ser executada no thread principal.

## 9. Resultado despachado não é resultado confirmado

O protocolo diferencia:
- **aceitação/encaminhamento:** o gateway aceitou e enviou a operação;
- **conclusão da API Android:** por exemplo, o callback de gesto informou que o gesto terminou ou foi cancelado;
- **efeito na aplicação de destino:** mudança de tela, envio de formulário ou alteração de estado observável.

Um retorno positivo de `dispatchGesture()` significa apenas que o sistema aceitou o despacho. O callback informa o término/cancelamento do gesto, mas não prova que o app de destino realizou a ação pretendida. O resultado deve refletir exatamente o nível de confirmação disponível, sem prometer sucesso semântico.

## 10. Limites e decisões deixadas para P2.0.7+

Este contrato não define ainda nomes/funções públicas finais de `mobile.screen`, parâmetros por operação, defaults de timeout por função, tipos de retorno específicos, política de exceções Python nem catálogo de capacidades. Esses pontos pertencem à P2.0.7.

P2.1-A/B/C concluídas no escopo acordado, com validação física de autorização/reboot. P2.2-A possui implementação publicada de fila, despacho, serialização, timeout/cancelamento lógico e fachada Python; testes JVM/Python host e build debug foram concluídos. A validação física foi parcial, com `type_text()` adiado; consulte o relatório de fechamento. A sessão de captura B2 permanece apenas especificada, sem implementação.

## 11. Referências oficiais

- Chaquopy 17.2 — Python API, conversão de tipos Java/Python e liberação do GIL em chamadas Java: https://www.chaquo.com/chaquopy/doc/current/python.html
- Chaquopy 17.2 — FAQ, interrupção de chamadas Python e execução no processo hospedeiro: https://www.chaquo.com/chaquopy/doc/current/faq.html
- Android — `CompletableFuture`, disponível desde API 24; `get(timeout, unit)`: https://developer.android.com/reference/java/util/concurrent/CompletableFuture
- Android — `AccessibilityService`: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Android — `AccessibilityServiceInfo`: https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo


## 12. Sessões temporárias de captura — contrato B2

A sessão de captura é um ciclo de vida prolongado e independente das operações pontuais. Ela **não** reserva posição na fila limitada do `AccessibilityOperationCoordinator`, mantém chamada Python síncrona aguardando por toda a duração ou bloqueia a main thread. Os controladores de sessão e overlay pertencem à instância conectada do `AndroidePyAccessibilityService`; o gateway application-scoped acompanha disponibilidade, sem reter Activity nem controlar a janela.

O início é curto: valida serviço, duração e contexto, cria o overlay e ativa o timer após anexar a janela. Timer, cancelamento, expiração e teardown são conduzidos pelo lifecycle nativo, com callbacks na main thread e relógio monotônico. Contrato aprovado: 15–300 segundos, padrão 120; contexto inicial somente por `event.packageName`, congelado antes do overlay; mudança posterior de aplicativo não encerra a sessão nem altera esse contexto. Encerramento normal por cancelamento ou expiração; interrupção/desconexão e falhas encerram por segurança. Detalhes em [P2_2B2_CAPTURE_SESSION_CONTRACT.md](P2_2B2_CAPTURE_SESSION_CONTRACT.md).

Nenhuma API Python pública `screen.*`, payload de sessão ou anúncio de capacidade é introduzido por este registro. A seleção/conclusão do mapa continua fora de B2. Este adendo é contratual, não comprova implementação ou testes.
