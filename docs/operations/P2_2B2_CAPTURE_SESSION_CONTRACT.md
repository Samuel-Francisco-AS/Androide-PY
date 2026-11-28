# P2.2-B2 — Contrato da sessão de captura e overlay

**Projeto:** Androide PY  
**Branch:** `feat/p2-accessibility-automation`  
**Estado:** contrato definido e aceito por Sam em 2026-11-28; implementação ainda não iniciada.  
**Escopo:** sessão temporária de captura e overlay do Mapa de Coordenadas.  
**Dependências:** P2.2-B1 (persistência Room) concluída. P2.2-B3 (seleção/nomeação dos pontos e conclusão do mapa) permanece fora deste contrato.

## 1. Objetivo e limites

Definir o ciclo de vida de uma sessão explícita de captura, com overlay `TYPE_ACCESSIBILITY_OVERLAY`, indicador visual, contagem regressiva e botão de cancelamento.

B2 prepara e mantém a sessão; **não seleciona, nomeia ou persiste pontos**. Não implementa conclusão do mapa, API pública Python `screen.*`, anúncio de capacidades, screenshots, OCR, leitura de texto/nós ou coleta contínua de conteúdo da tela. A sessão não ocupa a fila de operações pontuais nem mantém o worker Python bloqueado durante sua duração.

## 2. Decisões aceitas

| Tema | Decisão |
|---|---|
| Contexto do destino | Ler somente `event.packageName` do evento já configurado (`TYPE_WINDOW_STATE_CHANGED`). Não acessar payload, texto, nós, `rootInActiveWindow` ou conteúdo da janela. |
| Momento | Registrar fotografia imutável do último pacote externo válido conhecido no início, antes de adicionar o overlay. |
| Pacote do próprio app | Ignorar eventos cujo pacote seja o próprio Androide PY. |
| Mudança de aplicativo em primeiro plano | **Não encerra nem reinicia a sessão.** O pacote-alvo permanece como fotografia inicial; não acompanha a navegação. |
| Overlay | Indicador de captura, timer regressivo e botão explícito de cancelar. Sem seleção, nomes ou marcadores em B2. |
| Duração | Configurável: mínimo **15 s**, padrão **120 s**, máximo **300 s**, inclusive. |
| Encerramento normal | Cancelamento explícito pelo usuário ou expiração do timer. |
| Encerramento de segurança | Interrupção/desconexão do AccessibilityService ou falha operacional também encerra a sessão e descarta estado parcial. |
| Persistência | B2 não persiste captura parcial nem conteúdo de tela. |

A sessão só pode iniciar quando o serviço estiver operacional e houver pacote externo válido conhecido. Se o contexto estiver indisponível, vazio ou corresponder ao próprio Androide PY, rejeitar o início sem criar overlay. Uma mudança posterior de pacote em primeiro plano não cancela, não reinicia o timer nem altera o pacote registrado: a sessão representa o intervalo temporal escolhido pelo usuário, não uma trava vinculada ao foco contínuo de um aplicativo.

## 3. Responsabilidades e propriedade

- `AndroidePyAccessibilityService`: ponto de entrada Android e proprietário do ciclo de vida dos controladores.
- `CoordinateCaptureController`: estado, duração, relógio monotônico, timer, exclusividade e encerramento.
- `CaptureOverlayController`: criação, atualização e remoção da janela `TYPE_ACCESSIBILITY_OVERLAY` usando o contexto/WindowManager da instância conectada.
- `AccessibilityGateway`: permanece application-scoped e comunica disponibilidade/conexão; não retém Activity nem se torna proprietário da janela ou de uma instância antiga do serviço.

Os controladores pertencem à instância atual do serviço. Nenhum singleton/gateway deve manter referência forte a serviço desconectado. A remoção é idempotente, pois `onUnbind()` e `onDestroy()` podem ambos acionar teardown.

## 4. Máquina de estados

- `IDLE`: nenhuma sessão.
- `STARTING`: validação do contexto e tentativa de criação do overlay.
- `ACTIVE`: overlay anexado e timer em andamento.
- `STOPPING`: encerramento e remoção do overlay.
- `CANCELLED`: encerrada por ação explícita do usuário.
- `EXPIRED`: duração configurada atingida.
- `FAILED`: interrupção, desconexão ou falha operacional.

Transições:

```text
IDLE -> STARTING
STARTING -> ACTIVE | FAILED
ACTIVE -> STOPPING
STOPPING -> CANCELLED | EXPIRED | FAILED
CANCELLED | EXPIRED | FAILED -> IDLE (após limpeza/liberação)
```

Não há estado `COMPLETED` em B2; conclusão do mapa pertence à B3. Apenas uma sessão pode existir por instância de serviço. Tentativas enquanto houver sessão em `STARTING`, `ACTIVE` ou `STOPPING` são rejeitadas sem criar janela duplicada.

## 5. Início da sessão

1. Confirmar serviço operacional e ausência de sessão em andamento.
2. Validar duração inteira entre 15 e 300 segundos; usar 120 segundos quando omitida.
3. Obter o pacote externo válido já observado por `event.packageName`, ignorando o pacote do próprio app. Não consultar outras fontes de conteúdo da janela.
4. Fixar o pacote como contexto inicial imutável.
5. Entrar em `STARTING` e solicitar a criação do overlay.
6. Somente após a janela ser anexada com sucesso, entrar em `ACTIVE` e iniciar a contagem.

Falha de criação encerra em `FAILED`, sem timer ativo nem sessão ocupando o controlador. O início não bloqueia a main thread nem reserva a fila serial de operações durante o período.

## 6. Timer e permanência

- Usar `SystemClock.elapsedRealtime()` para prazo/tempo restante; não depender do relógio civil.
- Atualizar o indicador na main thread, sem `sleep` ou espera bloqueante.
- O timer começa após anexar o overlay; o prazo não é estendido por atualizações visuais.
- Mudanças de aplicativo em primeiro plano não alteram o pacote-alvo, não reiniciam o timer e não encerram a sessão.
- Ao atingir o prazo, encerrar com resultado `EXPIRED`.

## 7. Cancelamento, falhas e teardown

- O botão cancelar inicia encerramento `CANCELLED`.
- `onInterrupt()`, `onUnbind()` e `onDestroy()` invalidam a sessão, cancelam callbacks/tarefas pendentes e solicitam remoção do overlay.
- Falha de criação: `FAILED`, sem timer.
- Falha de remoção: registrar `OVERLAY_REMOVAL_FAILED`; não declarar limpeza bem-sucedida sem confirmação. Repetir limpeza de forma segura quando possível, sem duplicar janelas.
- Causas internas mínimas: `SERVICE_INTERRUPTED`, `SERVICE_DISCONNECTED`, `OVERLAY_CREATION_FAILED`, `OVERLAY_REMOVAL_FAILED`, `INTERNAL_ERROR`.
- `TARGET_CHANGED` **não é causa de encerramento em B2**.
- Em corrida entre cancelamento, expiração e teardown, a primeira transição terminal prevalece. Callbacks tardios não reativam sessão, recriam overlay ou alteram resultado terminal.
- Finalização libera timer, callbacks e referências à janela/serviço e descarta qualquer dado parcial não persistido.

## 8. Privacidade e minimização

- A única informação do evento usada para contexto é `event.packageName`; o filtro de eventos permanece o já configurado.
- Não ler texto, descrição, source, nós, árvore, extras, screenshots ou conteúdo da janela.
- Não registrar conteúdo de tela nem payload de evento. O pacote inicial é somente metadado técnico, não prova de identidade/autorização nem instrumento de monitoramento contínuo.
- Não persistir sessão temporária, timer, dados parciais ou histórico de navegação.
- Overlay visível e cancelável; usar somente `TYPE_ACCESSIBILITY_OVERLAY`; não adicionar `SYSTEM_ALERT_WINDOW`.

## 9. Critérios de aceite B2

Todos são critérios planejados; nenhum está marcado como implementado/testado neste registro.

| ID | Critério verificável |
|---|---|
| B2-01 | Inicia com serviço operacional, contexto externo válido e duração válida. |
| B2-02 | Rejeita início sem serviço conectado/operacional. |
| B2-03 | Rejeita duração abaixo de 15 s ou acima de 300 s; usa 120 s por padrão. |
| B2-04 | Rejeita início sem pacote externo válido ou quando o único pacote conhecido é o próprio app. |
| B2-05 | Registra o pacote-alvo antes do overlay e mantém a fotografia imutável. |
| B2-06 | Cria overlay `TYPE_ACCESSIBILITY_OVERLAY` com indicador, timer e botão cancelar. |
| B2-07 | Timer começa após anexar o overlay, usa tempo monotônico e respeita o prazo. |
| B2-08 | Mantém sessão ativa quando outro app assume o primeiro plano; não altera pacote nem timer. |
| B2-09 | Cancelamento encerra sessão e remove overlay. |
| B2-10 | Expiração encerra sessão e remove overlay. |
| B2-11 | Interrupção/desconexão encerra sessão e solicita limpeza. |
| B2-12 | Falhas de criação/remoção são reportadas sem falso sucesso. |
| B2-13 | Chamadas repetidas/concorrentes não criam sessões/overlays duplicados. |
| B2-14 | Teardown duplicado/callbacks tardios são idempotentes e não ressuscitam sessão. |
| B2-15 | Não persiste dados parciais, conteúdo de tela, screenshots ou payloads em logs. |
| B2-16 | Não adiciona `SYSTEM_ALERT_WINDOW`, API pública `screen.*` ou seleção de pontos em B2. |

## 10. Plano de verificação

**Testes JVM/host:** limites/defaults; transições; exclusividade; timer com relógio falso/injetável; cancelamento; expiração; mudança de pacote sem encerramento; interrupção/desconexão; corrida cancelamento/expiração; callbacks tardios; falhas simuladas de criação/remoção; idempotência.

**Revisão estática:** propriedade dos controladores no serviço; ausência de referências antigas, leitura de payload/nós, `SYSTEM_ALERT_WINDOW`, API/capability `screen.*` e bloqueio da main thread/fila Python.

**Build:** executar e registrar separadamente após implementação. Build não comprova funcionamento físico do overlay.

**Validação física:** adiada por decisão de Sam para a bateria integrada final da B3. Antes de qualquer instalação de APK, comunicar o risco Room schema 2 → 3 e obter decisão explícita de Sam.

## 11. Estado e limites

Contrato definido e aceito em 2026-11-28. B2 ainda não implementada, revisada, testada ou compilada. B3 permanece pendente. A aprovação deste contrato não autoriza implementação: é necessária autorização específica para iniciar B2.
