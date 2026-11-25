# P2 — Relatório operacional de implementação

**Projeto:** Androide PY  
**Branch:** `feat/p2-accessibility-automation`  
**Base:** `main`  
**Marco:** P2 — Automação de interfaces Android  
**Data do registro:** 2026-11-25  
**Estado:** planejamento operacional inicial publicado. Nenhuma implementação P2, teste, build ou validação física foi executada nesta abertura.

## 1. Objetivo

Evoluir a Android Bridge do Androide PY para permitir que scripts Python interajam de forma controlada com interfaces de outros aplicativos Android, utilizando um serviço nativo de acessibilidade autorizado pelo usuário.

A P2 deve oferecer dois meios complementares de interação:

- **Geométrico:** operar em posições da tela por coordenadas, inclusive por meio de mapas visuais calibrados e reutilizáveis.
- **Semântico:** consultar e localizar elementos que a árvore de acessibilidade do aplicativo de destino efetivamente disponibiliza.

A lógica de automação continua Python-first. Kotlin hospeda a integração nativa e o serviço Android; os scripts recebem estruturas simples e resultados estáveis, nunca objetos Android arbitrários.

## 2. Baseline e contexto arquitetural

A P2 parte da branch `main` após a integração da P1.

A arquitetura existente estabelece:
- Kotlin e Jetpack Compose como host Android.
- Chaquopy/Python como runtime de automação.
- Android Bridge baseada em capacidades específicas e contratos Python estáveis.
- Acesso a APIs Android por adaptadores Kotlin, sem expor `Context`, `Activity`, `PackageManager` ou objetos Android arbitrários aos scripts.
- Shizuku/ADB/root como alternativas opcionais, não dependências do núcleo.
- Execução Python serial como política inicial; não interromper threads Python arbitrariamente.
- Scripts tratados como código confiável do usuário, sem alegação de sandbox universal.

A ADR-0004 já estabelece a Bridge baseada em capacidades e reconhece AccessibilityService como mecanismo apropriado para eventos e ações de interface autorizadas, sem prometer acesso irrestrito. A P2 concretiza essa direção e deve respeitar as limitações do Android.

A inspeção detalhada do código atual e a confirmação de versões/API Android são tarefas de P2.0; este relatório não presume detalhes ainda não verificados.

## 3. Decisões de escopo aceitas em 25/11/2026

### 3.1 Ordem de prioridades

| Prioridade | Escopo aceito | Objetivo |
|---|---|---|
| A — Interação | Cliques/toques, digitação, rolagem e Mapa de Coordenadas | Tornar possível executar ações essenciais em interfaces |
| B — Consulta e coleta | Consultar informações disponíveis, coletar dados, retornar ao Python, console e possível arquivo | Permitir automações orientadas por dados da interface |
| C — Leitura de elementos | Leitura estruturada e busca na árvore de acessibilidade | Oferecer identificação semântica mais elaborada |

A ordem é intencional: as ações fundamentais vêm primeiro; a consulta/coleta vem em seguida; a leitura completa da árvore e busca avançada são complementares.

### 3.2 Mapa de Coordenadas — parte da Prioridade A

O Mapa de Coordenadas foi aceito como ferramenta de calibração visual reutilizável, e não apenas como seletor pontual.

Fluxo pretendido:
1. Script abre o aplicativo de destino ou aguarda o usuário chegar à tela desejada.
2. Um timer configurável aguarda antes de ativar a seleção (30 segundos foi citado como exemplo).
3. Uma camada translúcida sobrepõe a interface de destino.
4. O usuário toca em uma posição e recebe indicação visual do ponto.
5. O ponto é nomeado e salvo; o usuário pode registrar vários pontos no mesmo mapa.
6. A camada pode ser concluída ou cancelada e deixa de interceptar toques.
7. O script recebe os pontos e pode reutilizá-los em execuções futuras.

A implementação deverá investigar `TYPE_ACCESSIBILITY_OVERLAY` como mecanismo de overlay associado ao AccessibilityService. A camada precisará interceptar o toque durante a seleção; portanto, não é transparente aos eventos de toque nesse intervalo.

Modelo de coordenadas:
- armazenar x/y em pixels e normalizados;
- guardar largura/altura de referência e contexto de orientação/insets/densidade quando necessário;
- verificar compatibilidade antes de reutilizar;
- reconhecer que coordenadas normalizadas não corrigem mudanças de layout ou conteúdo.

O mapa poderá conter pontos nomeados (por exemplo, entrada, pesquisa, confirmar e rolagem) e ser salvo como perfil local reutilizável.

### 3.3 Consulta e saída de dados

A Prioridade B deve devolver dados em tipos Python simples e serializáveis para processamento em variáveis e exibição no console existente. A gravação em arquivo é desejada quando tecnicamente viável, mas depende de uma API de arquivos existente ou de uma extensão aprovada com destino/permissões/limites explícitos. Não se deve prometer exportação antes de verificar e implementar esse contrato.

### 3.4 Segurança e limites

- O usuário deve habilitar explicitamente o serviço nas configurações do Android.
- A aplicação deve explicar finalidade e capacidades e informar quando o serviço estiver indisponível.
- Não solicitar root, ADB ou Shizuku para o funcionamento central.
- Não prometer controle universal de aplicativos, telas seguras, campos protegidos ou conteúdo não exposto pela acessibilidade.
- Minimizar observação e coleta; não persistir conteúdo de tela automaticamente.
- Não registrar texto digitado ou conteúdo sensível em logs por padrão.
- Tratar desconexão, revogação, timeout e operação não suportada com erros claros.

## 4. Decomposição aprovada

A decomposição de execução está detalhada no [Checklist Operacional P2](P2_EXECUTION_CHECKLIST.md).

1. **P2.0 — Preparação, baseline e contratos:** leitura da documentação e código real, confirmação das versões/API, protocolo assíncrono, API Python, privacidade e ADRs.
2. **P2.1 — Fundação nativa e autorização:** AccessibilityService, manifesto/configuração, explicação e estado de autorização, lifecycle, erros e testes básicos.
3. **P2.2 — Prioridade A: interação e Mapa de Coordenadas:** toques/cliques, digitação, rolagem, gestos básicos e mapa visual com timer, overlay, múltiplos pontos, persistência e reutilização.
4. **P2.3 — Prioridade B: consulta e coleta:** snapshot/consulta sob demanda, estruturas Python, filtros, console, opção de arquivo e proteção de dados sensíveis.
5. **P2.4 — Prioridade C: leitura e identificação:** árvore limitada, atributos, busca semântica, referências temporárias e ações sobre elementos.
6. **P2.5 — Integração, testes e validação:** catálogo de capacidades, testes host/JVM, build, validação física no Moto G06, documentação e gate de integração.

A sequência preserva a prioridade funcional sem eliminar dependências técnicas: a fundação do serviço e o protocolo precisam existir antes das ações Python.

## 5. Critérios gerais de aceite

- Cada operação deve ter contrato Python documentado, tipos/retornos previsíveis, timeout e erros estáveis.
- O estado de autorização do AccessibilityService deve ser consultável e a revogação deve falhar de forma controlada.
- Cliques/toques, digitação e rolagem devem ser demonstrados individualmente no Moto G06.
- O Mapa de Coordenadas deve permitir selecionar e nomear múltiplos pontos, persistir o perfil e reutilizá-lo após reabrir o aplicativo.
- Coordenadas em pixels e normalizadas devem ser documentadas; mudanças de orientação/tela devem ser detectadas ou tratadas.
- Consulta/coleta deve retornar dados acessíveis ao Python e exibir resultado no console; arquivo de saída só será considerado aceito se sua implementação for incluída e validada.
- Leitura semântica deve respeitar limites de profundidade/tamanho e indicar quando o app não expõe os elementos.
- Testes host/JVM, build e validação física devem ser relatados separadamente.
- Nenhuma capacidade pode aparecer como implementada no catálogo mobile antes de existir e ter contrato/testes correspondentes.

## 6. Testes e validações

Nenhum teste P2 foi executado na abertura deste relatório.

| Categoria | Estado inicial |
|---|---|
| Revisão documental e arquitetural | Parcial; documentos centrais e ADR-0004 consultados para planejar a P2 |
| Inspeção do código P2 | Pendente |
| Testes Python host | Não executados para P2 |
| Testes JVM/Kotlin | Não executados para P2 |
| Build APK | Não executado para P2 |
| Validação de autorização no Moto G06 | Pendente |
| Validação Prioridade A | Pendente |
| Validação Prioridade B | Pendente |
| Validação Prioridade C | Pendente |

Os resultados históricos da P0/P1 não são evidência de funcionamento da P2.

## 7. Fora do escopo

- NotificationListenerService e automação de notificações.
- Agendamento e execução recorrente.
- Workflows/fluxos visuais avançados.
- Dependência obrigatória de Shizuku, ADB ou root.
- OCR/captura de tela como requisito inicial do Mapa de Coordenadas.
- Contorno de permissões, políticas Android ou proteções de aplicativos.
- Controle universal de jogos, apps bancários, telas seguras ou componentes não acessíveis.

## 8. Riscos e questões técnicas a resolver

1. Viabilidade e comportamento de `TYPE_ACCESSIBILITY_OVERLAY` nas versões Android suportadas e no Moto G06.
2. Protocolo assíncrono entre execução Python serial e serviço Android, evitando bloqueio da UI e chamadas indefinidas.
3. Limitações de `dispatchGesture`, ações de texto e ações de nós conforme API level e aplicativo de destino.
4. Conversão correta entre coordenadas físicas, densidade, orientação, barras do sistema, insets e área útil.
5. Persistência e versionamento de mapas de coordenadas sem criar acoplamento desnecessário.
6. Tratamento de dados sensíveis e limites de coleta.
7. Variação da árvore de acessibilidade entre apps, WebViews, componentes customizados e conteúdo desenhado.
8. Estratégia de testes que diferencie “gesto despachado” de “ação confirmada na interface”.

Esses itens não bloqueiam a criação da branch e dos documentos operacionais, mas devem ser resolvidos antes das respectivas implementações.

## 9. Integração

- Branch de trabalho: `feat/p2-accessibility-automation`.
- Base: `main`.
- Estado: branch criada e documentação operacional inicial publicada e conferida no GitHub.
- PR: ainda não criada.
- Merge: não realizado nem autorizado nesta etapa.
- Implementação: não iniciada.

## 10. Histórico de atualização

| Data | Alteração |
|---|---|
| 2026-11-25 | Abertura da P2; prioridades A/B/C, Mapa de Coordenadas na Prioridade A, decomposição e limites registrados conforme decisões de Sam. Checklist commit `8c23d5b79a9e6d0de86a4ce4287ed6c7996af302`; relatório commit `d8c2e409cf11077a8dd7da596a3380d339603eb8`; índice docs commit `3017ad8fa0983feb2d0950b366242eae0349786c`; checklist atualizado com evidências em `4722fce1ccf0d9647c821234e01f6c7668dd8ae0`. |
