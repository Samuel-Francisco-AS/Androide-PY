# ADR-0008: AccessibilityService, gateway e fronteira da Android Bridge

- **Status:** Aceito; P2.1-A implementada e compilada em build debug local; implementação inicial da P2.1-B publicada, ainda aguardando build/testes e validação física.
- **Data:** 2026-11-25
- **Escopo:** P2 — automação de interfaces Android.

## Contexto

A P2 precisa permitir que scripts Python consultem e interajam com interfaces de outros aplicativos sem expor classes Android ao runtime. O Android administra o ciclo de vida do AccessibilityService, exige autorização manual do usuário e impõe limitações por aplicativo, componente, versão e estado da interface.

Os contratos P2.0.6 e P2.0.7 definem, respectivamente, o protocolo assíncrono interno e a API Python síncrona. A revisão P2.0.8 definiu limites de privacidade e consentimento. É necessário consolidar as fronteiras arquiteturais antes de implementar o serviço.

## Decisão

1. **Serviço nativo gerenciado pelo Android.** Declarar um AccessibilityService com configuração explícita e somente as capabilities necessárias às operações aprovadas. O usuário habilita/desabilita o serviço manualmente nas configurações Android. O aplicativo não contorna nem automatiza essa autorização.
2. **Gateway application-scoped.** Um gateway nativo de ciclo de vida da aplicação recebe a instância do serviço pelos callbacks Android, publica estados distintos de autorização e conexão e remove referências no teardown. O gateway não instancia o serviço, não retém Activity e não mantém nós de acessibilidade após a operação.
3. **Fronteira Python-first.** Scripts usam a fachada pública `mobile.screen`. A Bridge encaminha solicitações validadas para o gateway. Não expor objetos `AccessibilityNodeInfo`, classes Android, handlers, futures, referências do serviço ou acesso direto ao Room.
4. **Concorrência e protocolo.** Manter a execução Python serial existente. A fachada pode ser síncrona para o script; o lado Kotlin permanece assíncrono conforme `P2_ASYNC_PROTOCOL.md`. O worker Python pode aguardar o resultado finito; a main thread Android nunca aguarda o Python. O gateway limita solicitações pendentes e serializa gestos que possam cancelar uns aos outros.
5. **Disponibilidade como capacidade.** Estados de autorização e conexão são distintos. O catálogo só anuncia uma operação como implementada quando sua implementação e testes correspondentes existirem. Suporte do Android não implica suporte universal por aplicativos de destino.
6. **Overlay restrito.** O overlay `TYPE_ACCESSIBILITY_OVERLAY` é usado apenas durante captura explícita de Mapa de Coordenadas. Não adicionar `SYSTEM_ALERT_WINDOW` para esse propósito. A captura deve ser visível e cancelável e remover o overlay em conclusão, cancelamento, timeout, interrupção ou desconexão.
7. **Minimização.** Consultas e ações são pontuais e limitadas à solicitação. Não observar continuamente a tela, não capturar screenshots na P2 inicial, não registrar conteúdo da interface automaticamente e não persistir snapshots de nós/árvores.
8. **Persistência.** Dados de mapas são gravados por repositórios Kotlin na infraestrutura Room existente, nunca por acesso Python direto ao banco.

## Alternativas consideradas

- **Python acessando diretamente classes Android ou nós:** rejeitada por acoplar scripts a detalhes internos, dificultar validação e ampliar a superfície de acesso.
- **Serviço ou gateway instanciado pela Activity:** rejeitada porque o lifecycle pertence ao Android e a Activity pode ser recriada ou destruída.
- **Uma thread Python por solicitação ou espera na main thread:** rejeitada por contrariar o executor serial existente e criar risco de bloqueio/concorrência.
- **Captura contínua de eventos e snapshots:** rejeitada por exceder o requisito de automação sob demanda e aumentar exposição de dados.
- **Permissão genérica de overlay de sistema:** rejeitada; o overlay específico do AccessibilityService atende o caso de captura sem solicitar `SYSTEM_ALERT_WINDOW`.

## Consequências

### Positivas
- Preserva a fronteira Python/Kotlin e os contratos públicos.
- Respeita o lifecycle e a autorização Android.
- Mantém operações observáveis, limitadas e serializadas.
- Permite testar o gateway e a fachada separadamente.
- Reduz coleta incidental e retenção de conteúdo de interface.

### Custos e limitações
- A integração exige tratamento rigoroso de desconexão, timeout, callback tardio e cancelamento lógico.
- Ações Android não garantem o efeito semântico esperado no aplicativo de destino.
- Alguns aplicativos não expõem conteúdo ou recusam ações; nenhuma garantia de controle universal é feita.
- O serviço e o overlay ainda precisam de implementação e validação no Moto G06.

## Contratos relacionados

- `docs/operations/P2_ASYNC_PROTOCOL.md` — envelope, timeout, fila, lifecycle e resultados.
- `docs/operations/P2_PYTHON_API.md` — superfície pública Python.
- `docs/operations/P2_SECURITY_REVIEW.md` — política de privacidade, consentimento e minimização.
- ADR-0004 — Bridge baseada em capacidades e adaptadores.

## Validação necessária

- Testes JVM do gateway, estados e fila; testes Python da fachada e erros.
- Testes de lifecycle para conexão, interrupção, desconexão e callback tardio.
- Verificação de que nenhuma classe Android/nó é exposta ao script.
- Testes de overlay concluído/cancelado/timeout/desconectado e ausência de permissões não aprovadas.
- Build APK e validação física no Moto G06, conforme checklist P2.

**Estado da evidência:** decisão documental aceita. A implementação inicial do AccessibilityService/gateway (P2.1-A) foi compilada em build debug local. A P2.1-B adicionou UI de autorização, consulta de estado e erros nativos, ainda aguardando build/testes. Overlay, operações de automação e testes integrados não foram implementados por este ADR; validação física permanece pendente.
