# ADR-0003: Persistência local com Room e SQLite

- **Status:** Aceito
- **Data:** 2026-11-24

## Contexto

A plataforma precisa armazenar metadados de scripts, versões, automações, workflows, agendamentos, execuções, logs e referências a artefatos. O produto é local-first e não exige um servidor de banco de dados.

## Decisão

Utilizar Room sobre SQLite como camada de persistência relacional. O acesso ao banco será mediado por repositórios e serviços Kotlin. O runtime Python não escreverá diretamente nas tabelas do Room.

Arquivos de scripts, temporários, backups e artefatos serão armazenados no sistema de arquivos privado do aplicativo, com metadados e referências no banco.

## Alternativas consideradas

- **SQLite acessado diretamente por Python:** reduziria parte da camada Kotlin, mas criaria dois caminhos de acesso e maior risco de divergência de schema/transações.
- **Banco remoto:** desnecessário para o requisito local-first e adicionaria dependência de infraestrutura.
- **Armazenamento apenas em arquivos:** insuficiente para consultas relacionais, histórico e integridade entre entidades.

## Consequências

### Positivas
- Persistência local sem servidor.
- Migrações e DAOs gerenciados pelo ecossistema Android.
- Uma única fonte de verdade para metadados.
- Separação entre dados estruturados e arquivos binários.

### Custos e riscos
- Kotlin precisa expor operações de persistência ao Python por contratos.
- Migrações de schema exigem testes.
- Arquivos e registros do banco precisam de política de consistência e limpeza.

## Validação

Implementar persistência mínima no P0 e validar gravação/leitura após reinício do aplicativo. Evoluir entidades e migrações conforme P1–P5.


## Evolução P1 — scripts, versões e execuções

A P1 mantém Room/SQLite como fonte de verdade e eleva o schema para a versão 2:

- scripts recebem descrição, tags, definições de parâmetros e manifesto declarativo de dependências;
- script_versions armazena snapshots imutáveis numerados;
- script_executions armazena estado, parâmetros resolvidos, timestamps, duração, stdout, stderr e erro;
- excluir um script remove seus metadados e versões em transação, preservando os registros de execução;
- o runner recebe parâmetros pelo dicionário Python params, sem acesso direto ao banco.

A migração manual 1 → 2 foi implementada, sem fallback destrutivo. A branch ainda não contém os JSONs de schema exportado Room v1/v2: o schema v1 deve ser recuperado/gerado da baseline P0, e o v2 gerado pelo build P1. Ambos devem ser versionados. A validação em banco P0 real continua pendente.
