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
