# P2.2-B3 — Relatório de validação física inicial e análise da falha

**Data:** 2026-11-28  
**Projeto:** Androide PY  
**Branch:** `feat/p2-accessibility-automation`  
**Commit do código/APK instalado:** `163b890cd2185538e82776647fc66359c6897257`  
**Dispositivo:** Moto G06 — Android 15 / API 35  
**Estado:** validação física bloqueada após o teste 2/15.

## 1. Resumo executivo

A primeira execução integrada no Moto G06 confirmou a inicialização do aplicativo e a conexão do AccessibilityService. O início da captura B3 também foi confirmado: o script Python retornou sem erro, o overlay apareceu e os toques foram representados como coordenadas no painel.

Entretanto, o fluxo não atingiu um estado terminal utilizável. O contador permaneceu visualmente em 2 minutos, o botão Cancelar não removeu o overlay e a sessão permaneceu na tela mesmo após aproximadamente dez minutos. O aparelho precisou de reinicialização forçada. Após reiniciar, Sam confirmou que o overlay desapareceu, o Moto G06 voltou ao funcionamento normal e o Androide PY continuou indicando **Conectado**.

**Resultado da bateria até aqui: 1 PASS, 1 FAIL, 13 não executados.** A validação permanece suspensa. O teste 2 não será apagado nem reclassificado; uma futura repetição após correção será registrada como reteste do mesmo caso, mantendo o teto de 15 testes.

## 2. Evidências da validação física

### Teste 1/15 — Inicialização e conexão do serviço

**Resultado: PASS**

Relato de Sam:
- O aplicativo foi fechado pela tela de recentes e aberto novamente.
- A interface carregou normalmente.
- O estado da Acessibilidade permaneceu **Conectado**.

### Teste 2/15 — Início da captura e apresentação do overlay

**Resultado: FAIL**

Script executado no editor Python do Moto G06:

```python
from mobile.screen import capture_coordinates

handle = capture_coordinates(
    "B3_VALIDACAO_OVERLAY_01",
    delay_ms=0,
    timeout_ms=120000
)

print("ID da captura:", handle.capture_id)
print("Estado:", handle.status())
```

**Comportamentos confirmados como funcionais:**
- O script iniciou sem erro.
- O overlay transparente e o painel inferior foram apresentados.
- Toques na tela foram mapeados e as coordenadas apareceram no painel.

**Comportamentos observados como defeituosos ou inconclusivos:**
- O timer permaneceu visualmente em 2 minutos.
- Os controles do painel responderam visualmente ao toque, mas Sam não conseguiu concluir as ações esperadas.
- Cancelar não removeu o overlay.
- A sessão permaneceu ativa por aproximadamente dez minutos, sem expiração observável.
- Foi necessária reinicialização forçada do aparelho.

Não foram fornecidos logs Android, stack traces ou capturas de diagnóstico do momento da falha. A evidência deste teste é o relato direto de Sam; não se afirma que a integração tenha observado o evento no dispositivo.

### Recuperação após a falha

Sam confirmou:
- O overlay desapareceu após a reinicialização.
- O Moto G06 voltou a funcionar normalmente.
- O estado do Androide PY voltou a apresentar **Conectado**.

Isto confirma recuperação operacional após reboot, mas não demonstra que a sessão tenha encerrado corretamente antes da reinicialização.

## 3. Artefato instalado

O APK foi gerado no Fedora a partir do commit indicado acima e transferido por Sam ao Moto G06 via My-CT.

- Caminho local do artefato: `android/app/build/outputs/apk/debug/app-debug.apk`
- SHA-256: `ec7b0b155a5c09372ac2471c48108a5de0e9ccd46155184c9170072b997275d4`
- Tamanho reportado: 32 MB.

Os dois commits que faltavam no checkout Fedora antes do build eram documentais; por isso o hash do APK permaneceu igual ao artefato anterior. A instalação/atualização no Moto G06 foi confirmada por Sam.

A autorização explícita previamente concedida por Sam para a possível migração Room schema 2 → 3 foi utilizada nesta instalação. Este relatório não presume nem afirma perda de dados; nenhuma evidência de perda foi relatada.

## 4. Revisão estática do código remoto

Arquivos principais examinados na branch:
- `android/app/src/main/java/com/samuel/androidepy/accessibility/CaptureOverlayController.kt`
- `android/app/src/main/java/com/samuel/androidepy/accessibility/CoordinateCaptureController.kt`
- `android/app/src/main/java/com/samuel/androidepy/accessibility/AndroidePyAccessibilityService.kt`
- `android/app/src/main/java/com/samuel/androidepy/accessibility/CoordinateCaptureRegistry.kt`
- `android/app/src/main/java/com/samuel/androidepy/bridge/AndroidBridgeAdapter.kt`
- `android/app/src/main/python/mobile/screen.py`
- `android/app/src/test/java/com/samuel/androidepy/accessibility/CoordinateCaptureControllerTest.kt`
- `docs/operations/P2_PYTHON_API.md`
- `docs/operations/P2_2B2_CAPTURE_SESSION_CONTRACT.md`

### 4.1 Interceptação dos toques: comportamento previsto pelo contrato

O overlay de seleção é uma janela `TYPE_ACCESSIBILITY_OVERLAY` de tela inteira (`MATCH_PARENT`). A raiz recebe um `OnTouchListener` que retorna `true`, registra a posição em `ACTION_UP` fora do painel e consome os demais eventos.

**Importante:** a interceptação dos toques fora do painel não é, por si só, um defeito de implementação. O contrato P2 e o checklist operacional dizem expressamente que, durante a seleção, o overlay intercepta os toques para transformá-los em pontos do mapa; ele não deve ser transparente aos eventos. Portanto, não é esperado que o usuário consiga operar normalmente o aplicativo que está por baixo durante a seleção. O requisito é que os controles do próprio painel funcionem e que a sessão possa ser encerrada com segurança.

O overlay atual não usa `FLAG_NOT_TOUCHABLE`, o que é coerente com a necessidade de receber os toques de seleção. Não se deve simplesmente adicionar essa flag ou tornar a janela pass-through: isso impediria a captura de coordenadas pelo mesmo mecanismo.

### 4.2 Controles do painel: callbacks presentes, mas condições de ação

A revisão encontrou listeners explicitamente registrados:
- **Adicionar ponto:** só age se já houver uma coordenada pendente e um nome não vazio, com até 80 caracteres; a aceitação também depende de `registry.addPoint()`.
- **Desfazer último:** só chama a remoção quando o contador local de pontos é maior que zero.
- **Concluir mapa:** só chama `onFinish()` quando existe pelo menos um ponto.
- **Cancelar:** sempre chama o callback de cancelamento.

O encadeamento de Cancelar está presente no código: o botão chama `onCancel`; o controlador associa esse callback à sessão e tenta executar `finish(CANCELLED)`, remover o overlay e notificar o callback terminal. Assim, a revisão estática não encontrou um callback de Cancelar ausente ou deliberadamente vazio.

As condições de Adicionar, Desfazer e Concluir podem explicar ações aparentemente inertes quando seus pré-requisitos não foram satisfeitos. Elas não explicam, sozinhas, a falha de Cancelar. O relato não permite determinar se todos os pré-requisitos de cada botão foram satisfeitos durante o teste.

### 4.3 Timer e timeout: ambos dependem da main Looper

Há dois mecanismos de prazo:
1. `CoordinateCaptureController` calcula um deadline monotônico com `SystemClock.elapsedRealtime()` e agenda atualizações/expiração pelo `HandlerCoordinateCaptureScheduler`, ligado à main Looper.
2. `AndroidePyAccessibilityService` agenda um timeout global pelo mesmo `mainHandler`.

Em condições normais, os callbacks devem atualizar o texto do timer e encerrar a sessão no prazo. Como os dois mecanismos dependem da thread principal, uma obstrução prolongada da main Looper poderia explicar conjuntamente timer congelado, controles sem resposta e timeout que não dispara.

**Isso é uma hipótese compatível com os sintomas, não uma causa raiz confirmada.** A inspeção dos caminhos apresentados não identificou, até aqui, um loop infinito ou uma espera explícita dentro do callback de atualização do timer ou dos listeners do overlay. É necessário continuar a revisão do ciclo de vida, do despacho de callbacks e de qualquer trabalho síncrono que possa alcançar a main Looper; não há evidência suficiente para atribuir a falha a um ponto específico.

### 4.4 Divergência concreta entre timeout público e duração interna

A API Python aceita `timeout_ms` entre 1.000 e 300.000 ms e a documentação define esse parâmetro como prazo total da captura. Separadamente, a sessão visual B2 possui duração de 15 a 300 segundos, com padrão de 120 segundos.

Na integração do serviço, porém, a chamada de `captureController.start()` usa sempre `CoordinateCaptureController.DEFAULT_DURATION_SECONDS` (120 segundos), sem derivar essa duração do prazo público. O timeout global continua usando o valor recebido.

Consequência: para uma chamada com `timeout_ms` superior a 120.000 ms, o controlador interno ainda expirará aos 120 segundos, antes do prazo total solicitado. Isso é uma divergência real entre o contrato documentado e a integração. **Não explica o caso executado**, que usou exatamente 120.000 ms, mas precisa ser corrigida ou o contrato precisa ser explicitamente ajustado antes de aprovar B3.

### 4.5 Cobertura automatizada não exercita o overlay real

O teste `CoordinateCaptureControllerTest` usa `FakeOverlay`. Esse fake implementa `show()`, mas não substitui `showSelection()`; portanto, a implementação padrão da interface apenas delega para `show()`.

Consequentemente, os testes de controller não exercitam:
- criação da janela Android real;
- dispatch de toque da raiz e do painel;
- listeners dos botões reais;
- atualização real do TextView;
- remoção do overlay pelo WindowManager no dispositivo.

A suíte valida a máquina de estados e o scheduler falso, mas não cobre a interação View/WindowManager que falhou fisicamente. Não foi localizado, entre os arquivos examinados, teste de instrumentação específico para `CaptureOverlayController`.

### 4.6 Remoção e falha de cleanup

O controlador tenta remover o overlay e, se a remoção lançar exceção, mantém o estado `FAILED` para permitir nova tentativa. O botão Cancelar também pode tentar novamente a remoção quando o estado já está `FAILED`.

Esse mecanismo é uma proteção útil, mas a UI não apresenta ao usuário uma indicação explícita de que a remoção falhou ou de que a sessão está em estado `FAILED`. Se a main Looper estiver bloqueada ou o callback do botão não for entregue, a tentativa de recuperação também não poderá ocorrer.

## 5. Achados e classificação

| ID | Achado | Classificação | Evidência |
|---|---|---|---|
| B3-F01 | A janela de tela inteira intercepta toques fora do painel | Comportamento previsto; não é defeito isolado | Contrato B2, checklist P2 e `CaptureOverlayController` |
| B3-F02 | Timer e timeout dependem da main Looper | Risco técnico / hipótese compatível com a falha | `CoordinateCaptureController` e `AndroidePyAccessibilityService` |
| B3-F03 | Callbacks dos botões existem; três ações possuem pré-condições | Revisão estática; causa da falha de Cancelar não determinada | `CaptureOverlayController` e `CoordinateCaptureController` |
| B3-F04 | `timeout_ms` aceita até 300 s, mas a duração interna é sempre 120 s | Divergência concreta de contrato/integração | `screen.py`, `AndroidBridgeAdapter` e `AndroidePyAccessibilityService` |
| B3-F05 | Testes JVM usam overlay falso e não exercitam Views/WindowManager | Lacuna de cobertura automatizada | `CoordinateCaptureControllerTest` |
| B3-F06 | Falha de remoção mantém overlay em `FAILED`, sem feedback visual explícito | Limitação de recuperação/diagnóstico | `CoordinateCaptureController` e `CaptureOverlayController` |

Nenhum dos achados acima comprova isoladamente a causa raiz do congelamento observado. O diagnóstico permanece parcialmente aberto até que o fluxo de callbacks e a execução na main Looper sejam examinados com maior profundidade e cobertos por testes dirigidos.

## 6. Próxima etapa recomendada

Antes de alterar o código ou pedir novo teste físico:

1. Inspecionar integralmente o caminho da Bridge e do AccessibilityService, incluindo agendamento, cancelamento, timeout, callback terminal e transições do registry.
2. Verificar se existe trabalho síncrono ou bloqueio indireto na main Looper durante a abertura/uso do overlay.
3. Revisar o comportamento real de dispatch de toque do `FrameLayout`, painel e botões, mantendo a interceptação externa que é exigida pelo contrato.
4. Resolver a divergência entre duração interna de 120 segundos e `timeout_ms` de até 300 segundos.
5. Acrescentar cobertura automatizada dirigida para callbacks e condições dos controles; quando apropriado, usar teste Android instrumentado para a janela/View real, sem ampliar desnecessariamente a suíte.
6. Somente depois da revisão e de correções aprovadas, gerar novo APK e planejar um reteste seguro do teste 2. Não repetir a captura com o APK atual.

Não executar os testes físicos 3–15 enquanto a falha crítica do teste 2 não estiver corrigida e o reteste controlado não for aprovado.

## 7. Estado final desta sessão

- Teste 1/15: **PASS**.
- Teste 2/15: **FAIL**.
- Testes 3–15: **não executados / bloqueados**.
- Recuperação do Moto G06 após reboot: **confirmada por Sam**.
- AccessibilityService após reboot: **Conectado**, conforme relato de Sam.
- Revisão estática: **parcialmente concluída; causa raiz ainda não confirmada**.
- Alterações de código: **nenhuma**.
- Validação física B3: **bloqueada**.
- Merge em `main`: **não realizado**.

A retomada deve começar pela continuação da revisão estática, e não por uma nova tentativa de captura no dispositivo.
