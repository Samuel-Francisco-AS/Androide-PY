# P0 — Persistência de scripts

## Arquitetura

- Banco Room/SQLite: `androide_py.db`, no armazenamento privado padrão do aplicativo.
- Entidade: `ScriptEntity`.
- DAO: `ScriptDao`.
- Banco: `AppDatabase`, singleton de processo.
- Repositório: `ScriptRepository`, disponibilizado de forma lazy por `AndroidePyApplication.scriptRepository`.

O schema é exportado para `android/app/schemas` por KSP. Os arquivos JSON gerados pelo primeiro build devem ser versionados; não devem ser editados manualmente.

## Primeiro uso e ausência de registro

`getOrCreateDefault()` consulta o script `default`. Se não existir, tenta inseri-lo com conflito IGNORE e relê o registro. Isso evita sobrescrever um script caso duas chamadas de primeira inicialização ocorram simultaneamente.

O script inicial é `Meu primeiro script`, com código `print("Olá, Androide PY!")`.

## Gravação

`save(scriptId, name, sourceCode)`:

- exige um id não vazio;
- remove espaços externos do nome;
- mantém o nome anterior se o novo nome estiver vazio;
- preserva `createdAtEpochMs` de um registro existente;
- atualiza `updatedAtEpochMs`;
- grava por meio do DAO.

## Falhas

- Erros de Room/SQLite são propagados como `ScriptStorageException`.
- Cancelamento de coroutine é propagado sem ser convertido em erro de armazenamento.
- Falhas não são silenciosamente ignoradas nem substituídas por dados vazios.
- O primeiro schema usa versão 1 e não configura migração destrutiva automática. Mudanças futuras exigem migração explícita e teste.

A camada UI deve executar chamadas suspend fora da thread principal e apresentar `ScriptStorageException` ao usuário sem encerrar a Activity.

## Fronteira Kotlin/Python

Python nunca recebe RoomDatabase, DAO, Entity, SQLiteConnection ou acesso direto ao arquivo do banco. O host Kotlin lê o conteúdo do script pelo repositório e entrega apenas o texto ao executor Python.
