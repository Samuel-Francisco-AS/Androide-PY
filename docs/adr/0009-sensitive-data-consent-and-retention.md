# ADR-0009: Consentimento para dados sensíveis e retenção de saídas

- **Status:** Aceito para orientar a implementação da P2; ainda não validado em código ou dispositivo.
- **Data:** 2026-11-25
- **Escopo:** leitura/interação de campos sensíveis, histórico de execução e Mapas de Coordenadas.

## Contexto

A P2 habilitará scripts Python a consultar e interagir com interfaces de outros aplicativos. A interface pode expor dados pessoais, credenciais, informações de autenticação e pagamento. A P1 já persiste parâmetros, stdout, stderr e informações de erro/traceback no histórico de execução.

A revisão P2.0.8 adotou a abordagem B: leitura de conteúdo comum sob demanda, com proteção reforçada para campos sensíveis. Sam também aprovou que a autorização para acessar um dado sensível não conceda automaticamente autorização para persistir a saída da execução.

## Decisão

### 1. Conteúdo comum e coleta mínima

- Conteúdo textual comum pode ser consultado somente quando uma operação do script o solicitar explicitamente e o Android o disponibilizar.
- A coleta é pontual e limitada à operação; não haverá monitoramento contínuo, leitura indiscriminada de toda a tela ou persistência automática de árvores/snapshots.
- Nós e referências Android permanecem temporários e não atravessam a fronteira pública Python.

### 2. Campos sensíveis: bloqueio por padrão e consentimento expresso

Campos que o sistema/aplicativo indique como senha, autenticação, pagamento/cartão ou outros dados sensíveis são bloqueados por padrão. O bloqueio não é absoluto: o usuário pode autorizar explicitamente uma operação sensível.

A autorização deve cumprir todos os requisitos:
1. Ser apresentada e concedida pela interface hospedeira do Androide PY, nunca pelo próprio script ou por parâmetros declarados no código.
2. Identificar o aplicativo de destino, a execução atual e a finalidade/tipo da operação de forma compreensível.
3. Ser concedida separadamente para **leitura de conteúdo sensível** e **preenchimento/alteração de conteúdo sensível**. Uma autorização não implica a outra.
4. Ser válida somente durante a execução autorizada e para o pacote de destino confirmado. Mudança do aplicativo em primeiro plano, encerramento/cancelamento da execução, interrupção ou desconexão invalida a concessão.
5. Ser verificada no gateway no momento da operação. Sem concessão válida, negar a solicitação sem expor o valor.
6. Não permitir contornar proteções do Android ou do aplicativo de destino. A autorização do usuário não torna acessível um conteúdo que o sistema/alvo não disponibilize.

A classificação deve usar sinais disponíveis, como atributos de senha, tipo de entrada, sinalizações de sensibilidade e semântica do nó. Os sinais podem ser incompletos ou incorretos; diante de indício ou ambiguidade, bloquear por padrão. Não prometer detecção perfeita de todo dado sensível.

### 3. Persistência de stdout, stderr e tracebacks

A permissão para acessar ou alterar um campo sensível **não autoriza persistir a saída da execução**.

- Em uma execução que recebeu autorização para acessar campos sensíveis, stdout, stderr e tracebacks ficam disponíveis apenas no console enquanto a execução estiver ativa e **não são persistidos no histórico por padrão**.
- O histórico ainda pode registrar metadados operacionais não sensíveis, como identificador, script/versão, horários, duração, estado terminal e códigos genéricos de erro, sem payloads ou valores de interface.
- Salvar saídas dessa execução exige consentimento adicional, separado, explícito e limitado àquela execução. A interface deve avisar que a saída pode conter credenciais ou outros dados sensíveis.
- A concessão de persistência não altera a autorização de leitura/escrita sensível; cada decisão permanece independente.
- Sem autorização adicional, o descarte da saída em memória ocorre ao encerrar a execução e liberar seus recursos.
- O contrato de execução/histórico P1 precisará de evolução explícita para suportar essa política; não presumir que a implementação atual já a cumpra.

Esta política controla a persistência automática do produto. Não constitui sandbox: scripts executam com as permissões do aplicativo e podem imprimir, copiar ou transmitir dados por mecanismos disponíveis ao processo. A interface deve comunicar esse limite antes de autorizar scripts com acesso sensível.

### 4. Mapas de Coordenadas e dados locais

- Persistir somente nome, pontos/coordenadas e metadados técnicos mínimos para calibração: dimensões, densidade, rotação e pacote de destino quando identificável.
- Não armazenar texto de tela, valores de campos, screenshots ou credenciais nos mapas.
- Usar Room e repositórios Kotlin existentes, em armazenamento privado do aplicativo; scripts não recebem acesso direto ao banco.
- Não exportar mapas automaticamente junto com scripts, nem transmiti-los automaticamente.
- Permitir exclusão individual de mapas e respectivos pontos.
- A configuração atual `allowBackup=false` não deve ser tratada isoladamente como prova de exclusão de toda modalidade de transferência de dados nas versões Android recentes. A implementação deve revisar/configurar regras de extração/transferência e verificar o comportamento no Android 15/API 35 antes de declarar mapas excluídos de backup.
- Mudanças de schema continuam sujeitas à política de desenvolvimento do ADR-0007; qualquer risco de descarte de dados locais deve ser comunicado antes do teste.

### 5. Logs e mensagens de erro

- O adaptador de acessibilidade não registra texto de tela, valores de campos, texto digitado, payloads ou conteúdo de notificações.
- Erros expostos ao script usam códigos estáveis e mensagens genéricas; não incluem valores sensíveis ou trechos de tela.
- Logcat e histórico técnico não devem receber conteúdo extraído automaticamente.
- A saída de scripts permanece código produzido pelo usuário e está sujeita ao modelo de confiança existente; a ausência de sandbox deve permanecer explícita.

## Alternativas consideradas

- **Proibir permanentemente qualquer interação com campos sensíveis:** rejeitada porque impede automações legítimas deliberadamente autorizadas pelo usuário.
- **Permitir acesso sensível por padrão:** rejeitada por ampliar a exposição acidental de credenciais e dados pessoais.
- **Um único consentimento para leitura, escrita e persistência:** rejeitada porque são operações e riscos diferentes.
- **Persistir toda saída de execução como na P1 atual:** rejeitada para execuções que receberam acesso sensível, pois stdout/stderr/tracebacks podem conter dados acessados.
- **Criar um banco separado para mapas:** rejeitada; Room já é a infraestrutura local, com acesso Kotlin mediado por repositórios.

## Consequências

### Positivas
- O usuário mantém controle explícito sobre operações sensíveis sem bloqueio absoluto da capacidade.
- Acesso, alteração e retenção de dados têm consentimentos independentes.
- Diminui a chance de credenciais chegarem acidentalmente ao histórico.
- Mapas permanecem locais e limitados a dados de calibração.

### Custos e limitações
- A interface precisa explicar e coletar consentimentos com escopo por execução e aplicativo.
- O executor e o histórico precisam suportar saída efêmera e persistência opcional por execução.
- A detecção de sensibilidade depende dos sinais fornecidos pelo Android e pelos aplicativos.
- Scripts não são isolados; podem utilizar rede e outras APIs disponíveis ao processo.
- A política de backup/transferência precisa ser testada no dispositivo e versão Android suportados.

## Contratos e decisões relacionados

- `docs/operations/P2_SECURITY_REVIEW.md` — modelo de ameaças e controles.
- `docs/operations/P2_PYTHON_API.md` — operações públicas previstas.
- `docs/operations/P2_ASYNC_PROTOCOL.md` — solicitações, estados e erros.
- ADR-0003 — Room/SQLite.
- ADR-0006 — parâmetros e histórico de execução.
- ADR-0007 — preservação de dados locais durante o desenvolvimento.

## Validação necessária

- Bloqueio de leitura e escrita sensíveis sem concessão.
- Concessões independentes de leitura e escrita, limitadas a execução e pacote.
- Revogação ao trocar de app, encerrar/cancelar execução, interromper ou desconectar serviço.
- Classificação conservadora e falha segura quando houver indício/ambiguidade.
- Execução sensível sem persistência automática de stdout/stderr/tracebacks.
- Persistência de saída somente após consentimento adicional explícito e escopado.
- Verificar logs, console e mensagens de erro sem vazamento automático de conteúdo.
- Testar Room, exclusão dos mapas e regras Android de backup/transferência.

**Estado da evidência:** decisões documentais aceitas. Nenhum controle de consentimento, retenção, mapa ou backup foi implementado ou testado por estes ADRs.
