# ADR-0003: Persistência local com Room e SQLite

- **Status:** Validado na P0; Room/schema P1 implementados; fallback destrutivo específico não validado isoladamente
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
- Mudanças incompatíveis de schema podem descartar todos os dados locais do banco durante o desenvolvimento.
- Arquivos e registros do banco precisam de política de consistência e limpeza.

## Validação

Implementar persistência mínima no P0 e validar gravação/leitura após reinício do aplicativo. Evoluir entidades e migrações conforme P1–P5.


## Evolução P1 — scripts, versões e execuções

A P1 mantém Room/SQLite como fonte de verdade e utiliza o schema versão 2:

- scripts recebem descrição, tags, definições de parâmetros e manifesto declarativo de dependências;
- `script_versions` armazena snapshots imutáveis numerados;
- `script_executions` armazena estado, parâmetros resolvidos, timestamps, duração, stdout, stderr e erro;
- excluir um script remove seus metadados e versões em transação, preservando os registros de execução;
- o runner recebe parâmetros pelo dicionário Python `params`, sem acesso direto ao banco.

Conforme o ADR-0007, a preservação de dados locais não é requisito padrão durante o desenvolvimento. O `Room.databaseBuilder` utiliza `fallbackToDestructiveMigration()`: quando a versão instalada não possui caminho de migração compatível, Room recria o banco usando o schema atual. Isso pode apagar scripts, versões e execuções locais. Migrações explícitas só serão adicionadas quando houver solicitação expressa de preservação.

O schema Room não é exportado/versionado nesta etapa (`exportSchema = false`), pois não há migrações históricas a validar. A integridade do schema atual continua sendo verificada pelos testes de persistência e pelo build.


## Evolução P2 — Mapas de Coordenadas

Os Mapas de Coordenadas previstos na P2 reutilizarão a infraestrutura Room existente, com persistência mediada por repositórios Kotlin. Os registros conterão apenas nome, pontos/coordenadas e metadados técnicos mínimos de calibração; não armazenarão texto de tela, valores de campos, screenshots ou credenciais. Os mapas permanecerão no armazenamento privado e não serão exportados junto com scripts nem transmitidos automaticamente. As regras Android de backup/transferência deverão ser revisadas e validadas no Android 15/API 35 antes de afirmar que esses dados estão excluídos de toda modalidade de backup. Ver ADR-0009.
