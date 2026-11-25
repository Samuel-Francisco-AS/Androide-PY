# P2 — Checklist operacional de implementação

**Projeto:** Androide PY  
**Branch de trabalho:** `feat/p2-accessibility-automation`  
**Base:** `main`  
**Marco:** P2 — Automação de interfaces Android  
**Data de abertura:** 2026-11-25  
**Responsável pela implementação remota:** assistente, via integração GitHub  
**Responsável por builds e validação física:** Sam, no Fedora e no Moto G06  
**Estado geral:** ⬜ Planejamento operacional publicado. Nenhuma funcionalidade P2 foi implementada, compilada ou validada nesta abertura. P0 e P1 permanecem integradas em `main`; esta branch parte da `main` pós-P1.

> Este checklist é a fonte operacional de verdade da P2. Publicar documentação ou código não equivale a executar testes, compilar o APK ou validar o comportamento no Moto G06. Marcar cada item somente após conferir evidências concretas.

## 1. Decisões de escopo aceitas

As decisões abaixo foram acordadas por Sam em 25/11/2026 e orientam a P2:

1. A P2 implementará automação de interfaces Android por integração nativa com `AccessibilityService`, conectada à Android Bridge e à biblioteca Python.
2. A ordem de prioridade funcional é:
   - **Prioridade A — Interação:** cliques/toques, digitação e rolagem; também inclui o Mapa de Coordenadas como ferramenta de calibração visual reutilizável.
   - **Prioridade B — Consulta e coleta:** consulta controlada da interface, obtenção de informações disponíveis e coleta de dados para variáveis Python, console e, quando houver API de arquivos autorizada, arquivos de saída.
   - **Prioridade C — Leitura e identificação:** leitura mais completa da árvore de acessibilidade e localização de elementos por atributos.
3. O **Mapa de Coordenadas** é parte da Prioridade A, não um extra futuro. Deve permitir selecionar posições visualmente sobre o aplicativo de destino e reutilizar os pontos em scripts/perfis.
4. O fluxo imaginado para o mapa inclui espera configurável (exemplo inicial: 30 segundos), abertura de uma camada translúcida sobre o app de destino, seleção por toque, registro da posição e retorno dos dados ao script. O tempo de 30 segundos é um exemplo, não um valor fixo.
5. O mapa deve permitir registrar múltiplos pontos nomeados e reutilizáveis, funcionando como ferramenta de calibração visual.
6. Coordenadas devem suportar pixels e representação normalizada (frações entre 0 e 1), registrando dimensões de referência e contexto suficiente para detectar incompatibilidades. Coordenadas normalizadas ajudam entre resoluções, mas não garantem compatibilidade entre layouts diferentes.
7. A camada de seleção poderá usar uma janela `TYPE_ACCESSIBILITY_OVERLAY` fornecida pelo AccessibilityService, sujeita à confirmação de viabilidade por versão Android e testes. Durante a seleção, a camada intercepta o toque; não deve ser descrita como transparente aos eventos.
8. Não depender de root, ADB ou Shizuku para o núcleo P2. Esses mecanismos continuam opcionais e fora deste marco.
9. O usuário concede explicitamente a autorização de Acessibilidade nas configurações Android. O aplicativo deve explicar a finalidade, expor o estado de autorização e não tentar contornar controles do sistema.
10. A automação semântica (por elementos acessíveis) e a automação geométrica (por coordenadas) são métodos complementares. Nenhum deles promete controle universal de todo app ou tela.
11. A lógica de automação e a API pública de alto nível permanecem Python-first; Kotlin implementa o serviço e os adaptadores Android. Scripts não recebem objetos Android arbitrários.
12. Dados coletados devem ser devolvidos como estruturas simples serializáveis ao Python. Saída no console faz parte do escopo B; gravação em arquivo deve reutilizar ou estender uma API de arquivos com limites e autorização explícitos, sem presumir que ela já exista.
13. O escopo será implementado incrementalmente, priorizando uma operação ponta a ponta antes de ampliar a superfície de API.

## 2. Regras operacionais

Para cada subetapa, seguir obrigatoriamente:

1. **CONFERE:** reler este checklist, AGENTS.md, arquitetura, roadmap, ADRs aplicáveis, contrato da Bridge e código relacionado.
2. **DEFINE:** registrar contrato, comportamento esperado, limitações Android, permissões, tratamento de erros e estratégia de teste antes de codificar.
3. **IMPLEMENTA:** alterar somente a subetapa selecionada e dependências estritamente necessárias.
4. **CONFERE:** revisar diff, chamadas entre Kotlin/Python, ciclo de vida, concorrência, segurança, testes e documentação.
5. **TESTA:** executar apenas testes realmente disponíveis; distinguir revisão estática, testes host/JVM, build e validação física.
6. **MARCA:** atualizar este checklist com commit, caminhos, comando e resultado observável.
7. **CONFERE:** reler o checklist publicado antes de selecionar a próxima subetapa.

Estados:
- ⬜ Pendente
- 🟡 Em andamento
- ⏸ Bloqueado
- ⚠️ Inconclusivo
- ☑️ Implementado e revisado
- 🧪 Testado automaticamente
- 📱 Validado fisicamente

Não marcar uma capacidade como validada fisicamente com base apenas em código, testes unitários ou instalação do APK.

## 3. P2.0 — Preparação, baseline e contratos

- [x] **P2.0.1** Criar branch dedicada a partir de `main`: `feat/p2-accessibility-automation`.
- [x] **P2.0.2** Criar checklist operacional e relatório da P2.
- [ ] **P2.0.3** Reler integralmente AGENTS.md, README.md, docs/README.md, PROJECT_VISION.md, ARCHITECTURE.md, ROADMAP.md, ADRs aplicáveis e contratos P0/P1.
- [ ] **P2.0.4** Inspecionar a estrutura real da branch, AndroidManifest, AndroidBridgeAdapter, PythonRuntime, ScriptExecutor, módulos `mobile`, registro de capacidades, testes e APIs de arquivo existentes.
- [ ] **P2.0.5** Registrar baseline de SDK mínimo/alvo, versões Android do Moto G06, ciclo de vida do serviço e compatibilidade das APIs oficiais utilizadas.
- [ ] **P2.0.6** Definir o protocolo assíncrono de chamadas Python → Kotlin → AccessibilityService → resultado, incluindo correlação de requisições, timeout, cancelamento lógico, erros e estado de autorização.
- [ ] **P2.0.7** Definir a API pública Python e seus contratos antes da implementação; nomes apresentados em exemplos/conversas são propostas, não contratos finais.
- [ ] **P2.0.8** Revisar implicações de privacidade e segurança: dados visíveis, campos sensíveis, senhas, conteúdo de notificações, captura de tela, overlays e registro de dados.
- [ ] **P2.0.9** Criar/atualizar ADR quando a decisão alterar fronteiras arquiteturais, permissões, política de dados ou protocolo da Bridge.
- [ ] **P2.0.10** Atualizar documentação de arquitetura/roadmap/índices se o contrato aprovado exigir.

**Gate P2.0:** contratos e limites técnicos definidos com base no código real e documentação Android oficial; nenhuma API pública presumida como implementada.

## 4. P2.1 — Fundação nativa e autorização

**Objetivo:** criar a base Android do serviço sem ainda declarar a automação funcional.

- [ ] **P2.1.1** Implementar classe `AccessibilityService` dedicada e configuração XML de serviço.
- [ ] **P2.1.2** Declarar o serviço no manifesto com as propriedades mínimas necessárias; justificar cada capability/evento solicitado.
- [ ] **P2.1.3** Criar tela explicativa de finalidade, riscos e instruções para habilitar/desabilitar o serviço nas configurações Android.
- [ ] **P2.1.4** Expor estado de disponibilidade/autorização para Kotlin e Python sem solicitar ou ativar autorização automaticamente.
- [ ] **P2.1.5** Implementar ciclo de vida robusto: conexão, desconexão, serviço indisponível, reinicialização do processo e ausência de Activity retida.
- [ ] **P2.1.6** Estabelecer política mínima de eventos e janelas de observação; evitar coleta contínua desnecessária.
- [ ] **P2.1.7** Implementar erros estáveis para serviço não habilitado, serviço desconectado, operação não suportada e timeout.
- [ ] **P2.1.8** Criar testes JVM/host para estados, protocolo e tratamento de falhas.
- [ ] **P2.1.9** Validar no Moto G06 habilitação, revogação, reconexão e comportamento após reiniciar o app.

**Gate P2.1:** serviço declarado corretamente, autorização explícita e estado observável; ciclo de vida testado. Não implica que as operações A estejam prontas.

## 5. P2.2 — Prioridade A: interação e Mapa de Coordenadas

**Objetivo:** permitir interação básica confiável com interfaces Android e registrar posições reutilizáveis.

### A1 — Toques e cliques

- [ ] **P2.2.1** Definir API de toque por coordenadas em pixels e normalizadas, com validação de limites e contexto de tela.
- [ ] **P2.2.2** Implementar toque simples usando APIs de gesto do AccessibilityService (`dispatchGesture`) quando suportadas.
- [ ] **P2.2.3** Retornar resultado estruturado: aceito/enviado, falha, motivo e metadados relevantes; não prometer que o app de destino concluiu a ação apenas porque o gesto foi despachado.
- [ ] **P2.2.4** Testar bordas, coordenadas inválidas, mudança de orientação, serviço desabilitado e falha de despacho.

### A2 — Digitação

- [ ] **P2.2.5** Definir API para inserir texto em elemento focado e/ou alvo identificado, usando ações de acessibilidade apropriadas.
- [ ] **P2.2.6** Definir política para campos não editáveis, foco ausente, teclado virtual, seleção/substituição de conteúdo e texto Unicode.
- [ ] **P2.2.7** Não registrar texto digitado em logs por padrão; tratar campos de senha e dados sensíveis com restrições explícitas.
- [ ] **P2.2.8** Testar texto comum, acentos, Unicode, campo sem foco, campo somente leitura e campo sensível.
- [ ] **P2.2.9** Documentar limitações: nem todo app/campo aceita ação de texto via acessibilidade; não prometer digitação universal.

### A3 — Rolagem e gestos básicos

- [ ] **P2.2.10** Definir rolagem vertical/horizontal e parâmetros de direção, distância/duração ou gesto equivalente.
- [ ] **P2.2.11** Implementar rolagem/gesto por ações de nó quando disponíveis e fallback por gesto coordenado quando apropriado.
- [ ] **P2.2.12** Distinguir operação solicitada de confirmação de que o conteúdo realmente se moveu.
- [ ] **P2.2.13** Testar contêiner rolável, tela sem rolagem, limites superior/inferior, orientação e gesto interrompido.
- [ ] **P2.2.14** Avaliar toque longo e deslize como gestos adicionais do mesmo núcleo, sem bloquear entregas fundamentais.

### A4 — Mapa de Coordenadas (Prioridade A)

- [ ] **P2.2.15** Definir modelo de dados do mapa: identificador, nome, pontos nomeados, x/y em pixels, x/y normalizados, largura/altura de referência, orientação, densidade/insets quando necessários e observações.
- [ ] **P2.2.16** Implementar espera configurável antes de iniciar a seleção; 30 segundos é exemplo inicial, com opção de iniciar manualmente/cancelar.
- [ ] **P2.2.17** Implementar overlay translúcido de seleção, preferencialmente `TYPE_ACCESSIBILITY_OVERLAY` após validação técnica; mostrar grade/mira e feedback do ponto tocado.
- [ ] **P2.2.18** Capturar um ponto por interação e permitir nomear/salvar múltiplos pontos no mesmo mapa.
- [ ] **P2.2.19** Permitir concluir, cancelar, refazer ponto e encerrar automaticamente a camada; garantir que a camada não bloqueie a navegação após o término.
- [ ] **P2.2.20** Devolver os pontos ao Python como dados serializáveis e permitir referenciá-los pelo nome em execuções futuras.
- [ ] **P2.2.21** Definir persistência de mapas/perfis: local, versionada e sem acoplamento ao histórico de execução de scripts; avaliar repositório existente antes de criar armazenamento novo.
- [ ] **P2.2.22** Implementar operação para executar toque a partir de um ponto salvo, com checagem de compatibilidade de tela e opção explícita de coordenada normalizada.
- [ ] **P2.2.23** Testar seleção com timer, seleção imediata, cancelamento, múltiplos pontos, reinício do app, rotação e diferenças de resolução.
- [ ] **P2.2.24** Registrar limites: coordenadas não reconhecem semanticamente o botão; mudanças de layout, escala, barras do sistema, teclado, densidade ou conteúdo dinâmico podem invalidar o mapa.

**Gate P2.2:** cliques/toques, digitação e rolagem validados individualmente; mapa visual seleciona e persiste pontos reutilizáveis no Moto G06. Capacidades não testadas permanecem pendentes.

## 6. P2.3 — Prioridade B: consulta e coleta de dados

**Objetivo:** permitir que scripts consultem informações que o Android disponibiliza por acessibilidade e utilizem os dados coletados.

- [ ] **P2.3.1** Definir snapshot limitado da janela/tela ativa e metadados permitidos.
- [ ] **P2.3.2** Implementar consulta pontual sob demanda, evitando monitoramento contínuo como padrão.
- [ ] **P2.3.3** Retornar estrutura Python serializável com campos explicitamente definidos e limites de profundidade/tamanho.
- [ ] **P2.3.4** Permitir filtrar/selecionar informações úteis sem exigir ainda o mecanismo completo da Prioridade C.
- [ ] **P2.3.5** Permitir que scripts processem os valores em variáveis e os exibam no console de execução existente.
- [ ] **P2.3.6** Avaliar exportação para arquivo usando API de arquivos existente ou proposta específica, com escolha do destino/permissão do usuário, limites e tratamento de falhas.
- [ ] **P2.3.7** Evitar coletar/armazenar senhas, conteúdo de campos sensíveis ou dados pessoais desnecessários; definir redaction e consentimento.
- [ ] **P2.3.8** Tratar telas vazias, janela indisponível, conteúdo não exposto, texto truncado, app bloqueado e timeout.
- [ ] **P2.3.9** Testar coleta no console e, se a API de arquivo for aprovada/implementada, verificar arquivo real de saída no Moto G06.

**Gate P2.3:** dados acessíveis retornam de forma previsível ao Python; console comprovado e exportação em arquivo comprovada apenas se incluída no escopo implementado.

## 7. P2.4 — Prioridade C: leitura e identificação de elementos

**Objetivo:** fornecer leitura estruturada e localização semântica mais completa.

- [ ] **P2.4.1** Definir modelo de elemento com atributos disponíveis: texto, contentDescription, className, resourceId, bounds, clickable, editable, focusable, selected, enabled e demais atributos justificados.
- [ ] **P2.4.2** Percorrer a árvore com limite de profundidade, quantidade de nós e tamanho total.
- [ ] **P2.4.3** Implementar busca por texto, descrição, identificador, classe e combinação de critérios.
- [ ] **P2.4.4** Definir identificadores/referências de elementos com validade curta e proteção contra nós obsoletos após mudança de tela.
- [ ] **P2.4.5** Integrar referências semânticas com ações de clique, digitação e rolagem quando a ação de nó for suportada.
- [ ] **P2.4.6** Retornar explicitamente quando o elemento não estiver exposto ou não puder ser acionado.
- [ ] **P2.4.7** Testar elementos duplicados, nós sem texto, árvores grandes, mudança de tela e apps com componentes customizados.
- [ ] **P2.4.8** Documentar que Canvas, jogos, conteúdo desenhado e algumas WebViews podem não expor elementos úteis.

**Gate P2.4:** leitura e busca funcionam em cenários suportados e reportam limitações sem prometer reconhecimento universal.

## 8. P2.5 — Integração, testes e validação final

- [ ] **P2.5.1** Atualizar catálogo de capacidades mobile para listar somente APIs efetivamente implementadas.
- [ ] **P2.5.2** Criar testes host Python com adaptadores falsos para contratos, serialização e erros.
- [ ] **P2.5.3** Criar testes JVM/Kotlin para serviço, ações, protocolo e lifecycle.
- [ ] **P2.5.4** Executar testes Python host e JVM; registrar comandos e resultados observados.
- [ ] **P2.5.5** Executar build debug e registrar APK/commit exato.
- [ ] **P2.5.6** Transferir/instalar no Moto G06 e validar primeiro autorização e ciclo de vida.
- [ ] **P2.5.7** Validar Prioridade A: clique, digitação, rolagem e mapa de coordenadas (pontos nomeados, persistência e reutilização).
- [ ] **P2.5.8** Validar Prioridade B: consulta, retorno Python, console e arquivo de saída se implementado.
- [ ] **P2.5.9** Validar Prioridade C: leitura e busca de elementos nos apps de teste.
- [ ] **P2.5.10** Registrar limitações por app/versão Android e não generalizar resultados de um único app.
- [ ] **P2.5.11** Revisar segurança, permissões, privacidade, consumo, estabilidade e comportamento quando o serviço é revogado.
- [ ] **P2.5.12** Atualizar relatório, checklist, documentação principal e PR com evidências; só então propor gate de integração.

**Gate P2.5:** todos os itens obrigatórios do escopo acordado possuem evidência apropriada ou são explicitamente adiados por decisão de Sam. Nenhuma capacidade pendente pode ser apresentada como aprovada.

## 9. Fora do escopo da P2

- Serviço de notificações (`NotificationListenerService`) — fase posterior.
- Agendamento, execução recorrente e eventos — fase posterior.
- Workflows/fluxos visuais e orquestração avançada — fase posterior.
- Integração obrigatória com Shizuku, ADB ou root — não faz parte do núcleo.
- OCR/captura de tela como requisito do mapa — não assumido; qualquer necessidade deve ser avaliada separadamente.
- Controle universal de jogos, apps bancários, campos protegidos, telas seguras ou componentes que bloqueiem acessibilidade.
- Execução em background irrestrita ou contorno de políticas Android.
- Coleta indiscriminada de conteúdo de tela.

## 10. Riscos e pontos de atenção

1. **Autorização especial:** AccessibilityService é sensível; finalidade e capacidades solicitadas devem ser transparentes e mínimas.
2. **Ciclo de vida:** o serviço pode ser desconectado/revogado a qualquer momento; chamadas Python precisam falhar de forma controlada.
3. **Assincronia:** chamadas à Bridge não podem bloquear a thread principal nem aguardar indefinidamente.
4. **Gestos:** `dispatchGesture` e ações de nós possuem limitações de versão, janela, concorrência e app de destino; validar documentação/API antes de fixar contrato.
5. **Overlay:** confirmar compatibilidade de `TYPE_ACCESSIBILITY_OVERLAY`, coordenadas/insets e captura de toque nas versões Android suportadas.
6. **Digitação:** ações de texto variam conforme componente e aplicativo; campos sensíveis precisam de proteção.
7. **Coleta:** conteúdo da interface pode conter dados pessoais; minimizar, limitar e não persistir automaticamente.
8. **Mapas:** coordenadas podem ficar obsoletas com alterações de layout, escala, rotação, teclado e barras do sistema.
9. **Compatibilidade:** não inferir suporte universal a partir de sucesso em um aplicativo de teste.
10. **API pública:** manter métodos e retornos pequenos, documentados e testáveis; não expor classes Android ao Python.

## 11. Registro de evidências e histórico

| Data | Subetapa | Evidência/resultado | Commit |
|---|---|---|---|
| 2026-11-25 | P2.0.1–P2.0.2 | Branch criada a partir de `main`. Checklist: commit inicial `8c23d5b79a9e6d0de86a4ce4287ed6c7996af302`; relatório: `d8c2e409cf11077a8dd7da596a3380d339603eb8`; índice `docs/README.md`: `3017ad8fa0983feb2d0950b366242eae0349786c`. Arquivos recuperados do GitHub para conferência posterior. | Ver commits |

Atualizações devem acrescentar linhas com caminhos, comandos, resultados e autoria da evidência. Relatos físicos do usuário devem ser identificados como tais.

## 12. Critério de encerramento da P2

A P2 só poderá ser proposta para encerramento depois de:
- Prioridade A implementada e validada no dispositivo físico, incluindo o Mapa de Coordenadas reutilizável;
- Prioridade B implementada no escopo aprovado e coleta/saída comprovada;
- Prioridade C implementada ou explicitamente reduzida/adiada por decisão de Sam;
- testes host/JVM e build registrados sem alegações não verificadas;
- permissões, privacidade, limitações e contratos documentados;
- revisão final e autorização explícita para integração em `main`.

A aprovação de um bloco não aprova automaticamente os demais.
