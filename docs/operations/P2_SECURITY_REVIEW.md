# P2.0.8 — Revisão de privacidade e segurança

**Estado:** revisão documental concluída; decisões de segurança aprovadas para orientar a implementação.  
**Branch:** `feat/p2-accessibility-automation`  
**Data:** 2026-11-25  
**Escopo:** P2 — AccessibilityService, consulta de interface, interação e Mapas de Coordenadas.  
**Natureza:** análise e contratos; não implementa serviço, autorização, overlay, persistência ou API Python.

## 1. Resultado da revisão do estado real

A revisão foi feita sobre `feat/p2-accessibility-automation`, comparada a `main` (`205044caffc9df7acb94f43eb85cbeea0787d9f3`). A branch está 20 commits à frente e 0 atrás. O diff contém somente documentação P2; nenhum código de produção foi alterado na P2 até esta revisão.

Foram conferidos `AGENTS.md`, checklist P2, contratos P2.0.6/P2.0.7, visão, arquitetura, roadmap, ADRs 0003/0004/0006/0007, manifesto, Bridge, runtime, executor, runner, entidades/repositórios Room e testes existentes.

Constatações relevantes:

- Não existe AccessibilityService, configuração XML do serviço, overlay, gateway de acessibilidade ou API `mobile.screen`.
- O manifesto declara `android:allowBackup="false"`; não há declaração de serviço de acessibilidade nem permissões de overlay.
- O catálogo Python anuncia somente `device.get_info` e `apps.launch`.
- A aplicação já possui Room privado ao aplicativo, com entidades de scripts, versões e execuções. A política vigente usa `fallbackToDestructiveMigration()`; mudanças de schema podem descartar dados locais.
- O histórico P1 persiste parâmetros, stdout, stderr e exceções/tracebacks. O executor também registra exceções do host no Logcat. Portanto, não seria correto afirmar que o histórico atual armazena apenas metadados.
- O runtime executa scripts Python no processo/permissões do aplicativo. Não há sandbox; scripts podem utilizar bibliotecas e rede disponíveis ao processo.
- Não existe API Python genérica de arquivos nem persistência de Mapas de Coordenadas.

## 2. Ativos, ameaças e superfícies

### Ativos
- Conteúdo acessível de aplicativos de destino, inclusive dados pessoais e credenciais.
- Texto fornecido pelo usuário a campos de interface.
- Mapas de coordenadas e metadados de contexto.
- Código, parâmetros, saída, erros e histórico dos scripts.
- Estado de autorização/conexão do AccessibilityService e solicitações pendentes.

### Atores e cenários de risco
- Script com erro ou comportamento malicioso, executado com as permissões do aplicativo.
- Aplicativo de destino que expõe conteúdo sensível ou marca incorretamente seus elementos.
- Acesso local ao dispositivo, depuração ou leitura de dados após comprometimento do aparelho.
- Terceiros alcançados por código Python que utilize rede ou bibliotecas disponíveis.

### Superfícies
- Eventos e árvore de acessibilidade; ações de leitura, clique e preenchimento.
- Bridge Python/Kotlin, gateway, fila e callbacks.
- Overlay de seleção do Mapa de Coordenadas.
- Room, histórico, console, Logcat e arquivos exportados.
- Código Python, parâmetros e bibliotecas executadas pelo runtime.

## 3. Política aprovada para leitura e interação

### 3.1 Conteúdo comum — abordagem B
- A leitura de texto comum é permitida quando uma operação do script a solicitar explicitamente e o Android disponibilizar o conteúdo.
- A coleta deve ser pontual, limitada à operação solicitada e ao mínimo de elementos/campos necessários.
- Não haverá varredura contínua, captura indiscriminada de eventos, leitura automática de toda a tela ou persistência automática de snapshots.
- A API deve devolver estruturas simples e serializáveis, nunca objetos `AccessibilityNodeInfo` ou referências Android.

### 3.2 Campos sensíveis — bloqueio por padrão, autorização expressa como exceção
Campos de senha, autenticação, pagamento/cartão e outros dados que o sistema ou o aplicativo identifique como sensíveis ficam bloqueados por padrão. **A interação não é proibida em definitivo:** Sam poderá autorizá-la expressamente.

A política distingue duas permissões independentes:
- **Leitura sensível:** autoriza obter o valor/conteúdo do campo.
- **Preenchimento/alteração sensível:** autoriza inserir ou substituir o conteúdo do campo.

Requisitos para a futura implementação:
1. A autorização deve ser concedida pelo usuário na interface hospedeira do Androide PY, nunca por uma declaração do próprio script.
2. A concessão deve ser explícita, informada e limitada à execução atual, ao aplicativo de destino identificado e ao tipo de operação autorizado.
3. Autorizar leitura não autoriza escrita, e vice-versa. Troca do aplicativo em primeiro plano não transfere automaticamente a concessão.
4. Ao terminar/cancelar a execução, revogar a autorização; desconexão/interrupção do serviço invalida a concessão e operações pendentes.
5. Sem concessão válida, negar a operação com erro estável e sem expor o valor do campo.
6. Se houver indicador de sensibilidade ou classificação ambígua, aplicar bloqueio por padrão. Uma concessão expressa pode autorizar a operação, desde que o Android ainda permita acessá-la.
7. A autorização do usuário não contorna telas seguras, restrições do Android ou a decisão do aplicativo de destino de não expor um elemento.

Sinais de classificação podem incluir atributos de senha, tipo de entrada, sinalização de sensibilidade e metadados/semântica fornecidos pelo aplicativo. Esses sinais variam por versão e implementação; não existe garantia de detectar perfeitamente todo dado sensível. A política deve falhar de forma conservadora quando houver indício, mas a documentação não deve prometer detecção infalível.

A autorização é uma política de consentimento do produto, não uma sandbox: scripts continuam executando com as permissões do processo e podem tentar usar outros meios disponíveis. O gateway deve ser a única fronteira suportada para as capacidades de acessibilidade.

## 4. Conteúdo fora do escopo e minimização

- Captura de screenshots não faz parte do escopo P2 inicial.
- Conteúdo de notificações permanece fora da P2; pertence a uma etapa própria, com autorização e revisão separadas.
- Não registrar texto digitado, valores de campos, texto de tela, valores de nós ou conteúdo de notificações em Logcat, mensagens de erro ou logs automáticos do adaptador.
- Erros devem conter código estável, mensagem genérica, request ID e estado operacional; não devem incorporar payloads ou valores de interface.
- Consultas devem ter limites de quantidade/profundidade/tamanho definidos na implementação, sem reter nós após a conclusão.
- Snapshots e referências de nós devem existir somente durante a operação. Não persistir árvores, nós ou conteúdo extraído automaticamente.

## 5. Mapas de Coordenadas e persistência

- Persistir apenas nome do mapa, pontos/coordenadas e metadados técnicos necessários para recalibração e compatibilidade (dimensões, densidade, rotação e pacote em primeiro plano quando disponível).
- Não incluir texto de tela, valores de campos, screenshots ou credenciais nos mapas.
- Usar a infraestrutura Room existente, por repositórios Kotlin; não criar banco separado nem permitir escrita Python direta.
- Manter os dados no armazenamento privado do aplicativo. O manifesto atual usa `allowBackup=false`, mas isso, isoladamente, não deve ser tratado como prova de exclusão de toda modalidade de transferência entre dispositivos em versões Android recentes. A implementação deverá revisar e configurar explicitamente as regras de extração/transferência de dados (data extraction rules), validar o comportamento na API 35 e manter mapas fora de exportações de scripts. Não transmitir mapas automaticamente.
- Permitir exclusão individual do mapa e de seus pontos associados.
- A política de desenvolvimento ADR-0007 permite migração destrutiva; qualquer alteração de schema deve avisar que dados locais podem ser descartados.
- O mapa não é compartilhado automaticamente entre aplicativos. O pacote/contexto registrado serve para detectar incompatibilidade, não como prova de identidade ou autorização.

## 6. Overlay, autorização Android e lifecycle

- O usuário habilita o AccessibilityService manualmente nas configurações Android; o aplicativo explica finalidade e estado. Nunca habilitar o serviço por conta própria ou contornar Settings.
- O overlay `TYPE_ACCESSIBILITY_OVERLAY` existe somente durante a captura solicitada do mapa, identifica claramente o modo de seleção e oferece concluir/cancelar.
- Cancelamento, timeout, interrupção ou desconexão removem o overlay e descartam captura parcial.
- O overlay não imita telas de login, diálogos do sistema ou interfaces de outros aplicativos.
- Não adicionar `SYSTEM_ALERT_WINDOW` para substituir o overlay próprio do serviço.
- O serviço é gerenciado pelo Android; o gateway application-scoped não instancia o serviço nem retém Activity.
- Na desconexão, invalidar solicitações e concessões sensíveis pendentes; callbacks tardios são descartados.

## 7. Logs, console e histórico — ressalva importante do estado P1

O adaptador de acessibilidade não deve registrar conteúdo de tela ou texto digitado. Contudo, a revisão encontrou uma limitação preexistente: scripts podem imprimir qualquer valor em stdout/stderr, e o histórico P1 persiste essas saídas, parâmetros e detalhes de exceções/tracebacks. O runner também captura exceções Python, e o executor registra exceções do host no Logcat.

Assim:
- A P2 não deve adicionar conteúdo de interface aos logs automaticamente.
- Um script que deliberadamente imprima um valor obtido pela API poderá fazer esse valor aparecer no console e no histórico atual.
- Não há redaction confiável e universal de stdout/stderr sem alterar o contrato de execução; não declarar que o histórico atual é livre de dados sensíveis.
- A futura UI/implementação deverá deixar claro esse comportamento antes de conceder acesso sensível. A decisão foi formalizada e aceita no ADR-0009: em uma execução que recebeu autorização para acessar campos sensíveis, stdout/stderr/tracebacks ficam disponíveis apenas no console enquanto a execução estiver ativa e não são persistidos no histórico por padrão. Persistir essas saídas exige consentimento adicional, separado, explícito e limitado à execução. Essa política ainda não está implementada e exigirá evolução explícita do executor/histórico P1.

## 8. Rede e confiança dos scripts

O Androide PY não transmitirá automaticamente conteúdo de acessibilidade, mapas ou valores de campos para servidores próprios ou terceiros. Entretanto, scripts Python executam com as permissões do aplicativo e podem usar rede/bibliotecas disponíveis. Como não existe sandbox, não é tecnicamente correto prometer que um script autorizado não enviará dados por conta própria.

A interface de autorização deve explicar que conceder acesso sensível a uma automação confiável permite que o código dessa automação processe o dado. Scripts importados/externos continuam sujeitos à confirmação de execução já definida pela arquitetura; essa confirmação não equivale a sandbox.

## 9. Controles e critérios para implementação futura

| ID | Controle | Critério verificável |
|---|---|---|
| SEC-01 | Observação sob demanda | Sem coleta contínua; somente eventos/consultas necessários à operação |
| SEC-02 | Campos sensíveis bloqueados por padrão | Sem concessão, leitura e escrita sensíveis são negadas |
| SEC-03 | Exceção por autorização expressa | Concessões separadas para leitura e escrita, feitas pela UI e limitadas à execução/app |
| SEC-04 | Screenshots fora da P2 inicial | Nenhuma API/capability de captura de tela |
| SEC-05 | Mapas mínimos | Persistência apenas de coordenadas e metadados técnicos |
| SEC-06 | Logs sem conteúdo automático | Adaptador não registra texto de tela, valores digitados ou payloads |
| SEC-07 | Overlay temporário e cancelável | Overlay removido em concluir, cancelar, timeout e teardown |
| SEC-08 | Armazenamento privado | Room/repositório Kotlin; sem exportação automática ou compartilhamento |
| SEC-09 | Sem transmissão automática | Nenhum envio de dados de acessibilidade pelo produto; limitação de scripts sem sandbox documentada |
| SEC-10 | Bridge como fronteira | Scripts usam somente API pública; nenhum objeto Android/gateway exposto |

## 10. Pendências e limites da conclusão

P2.0.8 concluiu a revisão de segurança; P2.0.9 formalizou as decisões nos ADRs-0008 e 0009; P2.0.10 harmoniza a visão, arquitetura, roadmap, índices e contratos. As decisões aceitas orientam a implementação futura, mas não provam que qualquer controle esteja implementado.

A implementação futura deverá testar, no mínimo: bloqueio sensível sem concessão; leitura e escrita separadas; escopo da concessão por execução/aplicativo; revogação em cancelamento/desconexão; classificação conservadora; ausência de conteúdo em logs do adaptador; descarte de snapshots; overlay removido em todos os caminhos terminais; persistência/exclusão dos mapas; comportamento de backup/exportação.

**Nenhum serviço, overlay, controle de autorização, API ou teste P2 foi implementado ou executado nesta subetapa.**
