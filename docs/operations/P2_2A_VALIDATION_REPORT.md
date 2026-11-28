# P2.2-A — Relatório de validação e fechamento com ressalvas

**Projeto:** Androide PY  
**Branch:** `feat/p2-accessibility-automation`  
**Data:** 2026-11-27  
**Dispositivo físico:** Moto G06 / Android 15 (API 35)  
**Estado:** **FECHADA ADMINISTRATIVAMENTE COM RESSALVAS**. Implementação, revisão estática, testes automatizados e build aprovados. Validação física parcial: quatro operações aprovadas; `type_text()` adiado por limitação do procedimento de teste. Nenhuma capacidade `screen.*` deve ser anunciada em `mobile.capabilities` neste fechamento.

## 1. Escopo e decisão de fechamento

A P2.2-A entregou o núcleo geométrico e a fachada inicial de interação `mobile.screen`: consulta de estado, toque, deslize, rolagem e inserção de texto sob as políticas conservadoras documentadas.

Sam autorizou o encerramento documental da subetapa após a execução dos testes automatizados, build e bateria física limitada a cinco testes. A bateria foi encerrada com quatro testes aprovados e um teste adiado. O fechamento não equivale à aprovação integral de todas as operações públicas: `type_text()` permanece sem validação física.

A decisão de adiar `type_text()` foi tomada porque, ao sair das Configurações Android para executar o script no Androide PY, o campo de busca deixa de permanecer focado. Não foi possível preparar o campo sem introduzir outra operação de acessibilidade no mesmo script de teste, o que violaria o procedimento acordado. Sam preferiu aguardar a implementação do Mapa de Coordenadas para realizar essa validação de forma controlada.

**Classificação do fechamento:** entrega técnica da P2.2-A concluída no escopo acordado, com validação física parcial e pendência explícita. Não classificar `type_text()` como PASS ou FAIL; seu estado é **ADIADO / NÃO TESTADO FISICAMENTE**.

## 2. Testes automatizados e build

Sam executou no Fedora, no diretório `android/`:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --max-workers=2
```

**Resultado reportado:** `BUILD SUCCESSFUL`. Os testes JVM da tarefa `:app:testDebugUnitTest` e a montagem debug `:app:assembleDebug` concluíram com sucesso.

Sam também executou:

```bash
PYTHONPATH=android/app/src/main/python python3.13 -m unittest discover -s android/app/src/test/python -v
```

**Resultado reportado:** 22 testes Python aprovados.

A revisão estática da implementação foi concluída anteriormente e permanece aprovada. Esses resultados são evidências reportadas por Sam a partir do ambiente Fedora; não foram executados novamente durante o registro documental remoto.

## 3. Bateria física — limite máximo de cinco testes

A bateria foi limitada a cinco testes totais. `get_status()` contou como o primeiro teste. Não foram adicionados testes extras.

| Nº | Operação | Resultado | Observação |
|---:|---|---|---|
| 1 | `get_status()` | **PASS** | Retornou `enabled=True`, `connected=True`, `state="connected"`. |
| 2 | `tap()` | **PASS funcional** | O toque foi executado nas Configurações Android. O alvo atingido foi “Tela inicial e de Bloqueio”, diferente do alvo inicialmente pretendido; comprova despacho/execução do toque, mas não precisão de coordenadas. |
| 3 | `swipe()` | **PASS funcional, com ressalva** | O gesto de arraste de baixo para cima ocorreu. Pela estrutura da tela, o efeito ficou semelhante à rolagem; não se considera comprovada uma distinção visual inequívoca entre swipe e scroll. |
| 4 | `scroll()` | **PASS** | A rolagem ocorreu na tela das Configurações Android. |
| 5 | `type_text()` | **ADIADO** | Campo focado não pôde ser mantido ao alternar para o Androide PY para executar o script. A validação será retomada após implementação do mapeamento de coordenadas, com preparação controlada do campo. |

### Interpretação dos resultados

- Os PASS funcionais demonstram que as operações alcançaram comportamento observável no dispositivo durante os cenários executados.
- O resultado de `tap()` não valida a exatidão das coordenadas fornecidas; a calibração/mapeamento ainda será tratada no escopo próprio.
- O resultado de `swipe()` confirma a execução do gesto, mas a observação não isolou de forma inequívoca seu efeito de um movimento de rolagem.
- O PASS de `scroll()` registra que a rolagem foi observada na tela de Configurações.
- O contrato de retorno `completed=True` não deve ser interpretado como garantia do efeito semântico pretendido no aplicativo de destino.
- `type_text()` não foi reprovado nem aprovado fisicamente. A ausência de teste decorre da preparação/foco do campo, não de erro observado na implementação.

## 4. Ressalvas e pendências preservadas

1. **`type_text()`:** validação física pendente. Retomar após a implementação do mapeamento de coordenadas, em teste isolado e sem combinar múltiplas operações `screen.*` no mesmo script.
2. **Precisão geométrica:** as coordenadas usadas no teste de `tap()` foram estimadas a partir da captura de tela; não houve calibração da resolução/densidade via ADB. Não declarar precisão validada.
3. **Generalização:** a evidência física é específica do Moto G06 / Android 15 (API 35) e dos aplicativos/telas utilizados. Não implica compatibilidade universal.
4. **Mapas de Coordenadas:** captura, persistência e reutilização não fazem parte do resultado desta validação. Permanecem para as subetapas posteriores aprovadas no planejamento, ainda não autorizadas para implementação nesta rodada.
5. **Catálogo:** manter `mobile.capabilities` sem qualquer `screen.*`. Não anunciar `screen.get_status`, `screen.tap`, `screen.swipe`, `screen.scroll` ou `screen.type_text` como capacidades liberadas até que os critérios do gate correspondente sejam integralmente atendidos, incluindo a pendência de `type_text()`.
6. **Escopo de segurança:** não relaxar a política conservadora de campos sensíveis/ambíguos para facilitar o teste. Não introduzir fallback de digitação que contorne `ACTION_SET_TEXT` ou a política aprovada.

## 5. Decisões

- Fechar documentalmente a P2.2-A como **entrega técnica concluída com ressalvas**, registrando de forma explícita que a validação física não foi integral.
- Preservar `type_text()` como pendência de validação, a ser retomada após o mapeamento de coordenadas.
- Não realizar alterações em `mobile.capabilities` para anunciar operações `screen.*`.
- Não iniciar P2.2-B nem P2.2-C, não alterar schema Room, não fazer merge e não modificar `main`.
- A retomada de `type_text()` deve ser planejada como teste físico isolado, respeitando o limite original da bateria encerrada; qualquer nova bateria ou ampliação do limite exigirá decisão explícita de Sam.

## 6. Evidências e autoria

- Testes JVM + build debug: execução local reportada por Sam no Fedora; comando e resultado registrados na seção 2.
- Testes Python host: 22/22 PASS, execução local reportada por Sam.
- Validação física: resultados dos cinco itens registrados conforme relatos de Sam no Moto G06 / Android 15 (API 35).
- Revisão estática: concluída na P2.2-A, sem alteração incidental dos avisos de depreciação `recycle()` em `AndroidePyAccessibilityService.kt`.
- Implementação-base previamente registrada no commit `3ea04e4695fd8a11abfb530d8e9e05a63b8c7e9e`.

**Nenhuma validação foi inventada ou inferida a partir do build. Este documento distingue testes automatizados, compilação e observação física.**
