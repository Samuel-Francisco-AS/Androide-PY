# ADR-0006: Versionamento, parâmetros e histórico da biblioteca de scripts

- **Status:** Aceito para a P1; validação de migração e build pendente
- **Data:** 2026-11-25

## Contexto

A P0 persistia um único script e executava código Python sem um catálogo completo, snapshots de versão ou histórico relacional. A P1 precisa permitir que o usuário mantenha vários scripts, altere metadados, declare entradas e consulte resultados anteriores sem misturar persistência Kotlin e execução Python.

Também é necessário preservar os scripts já gravados no banco P0. O runtime Chaquopy não deve ser tratado como um ambiente pip desktop nem como sandbox.

## Decisão

1. Manter Room/SQLite como fonte de verdade e elevar o schema da versão 1 para a versão 2 por migração incremental explícita.
2. Expandir a entidade scripts com descrição, tags, definições JSON de parâmetros e manifesto declarativo de dependências.
3. Criar script_versions com chave composta (script_id, version_number). Cada alteração efetiva de código ou metadados cria um snapshot imutável. Restaurar uma versão cria um novo número, sem sobrescrever o histórico.
4. Criar script_executions com identificador, script, nome/version snapshot, parâmetros resolvidos, status, horários, duração, stdout, stderr, erro estruturado e flags de truncamento.
5. A exclusão de um script e suas versões é transacional; registros de execução permanecem para preservar evidências históricas.
6. O host valida definições e valores de parâmetros e entrega um objeto JSON ao runner. O código Python recebe esses valores em params.
7. Dependências ficam declaradas por script e passam por validação sintática local. Instalação de pacotes continua sendo responsabilidade controlada do build Chaquopy, após avaliação da versão Python, ABI e disponibilidade do pacote.
8. Python nunca recebe DAO, RoomDatabase ou acesso direto ao arquivo SQLite.

## Alternativas consideradas

- **Um arquivo JSON como banco completo:** simplificaria o protótipo, mas dificultaria consultas, transações e relações entre scripts, versões e execuções.
- **Criar um banco SQLite separado acessado pelo Python:** criaria duas fontes de verdade e duplicaria regras de migração.
- **Sobrescrever o script sem snapshots:** perderia histórico e impediria restauração.
- **Instalar qualquer pacote PyPI dinamicamente no dispositivo:** não é uma promessa segura nem compatível com a distribuição de todos os pacotes Android/Chaquopy.

## Consequências

### Positivas

- Scripts, versões e execuções possuem identidade e persistência locais.
- Alterações podem ser restauradas sem apagar snapshots anteriores.
- Execuções continuam consultáveis após exclusão do script.
- Parâmetros são validados antes de chegar ao runtime.
- A fronteira Kotlin/Python permanece explícita.

### Custos e riscos

- O schema exige migração 1 → 2 e testes de preservação de dados.
- O histórico pode crescer sem política de retenção automática.
- O manifesto de dependências não prova compatibilidade binária; cada pacote precisa de validação real.
- O editor P1 fornece realce lexical básico, não análise semântica ou depuração.
- Scripts continuam executando com as permissões do aplicativo; não há sandbox.

## Validação

- Testes JVM do repositório e do histórico de execução foram escritos, mas ainda precisam ser executados.
- Testes de resolução de parâmetros foram escritos, mas ainda precisam ser executados.
- A migração Room 1 → 2 foi implementada, mas precisa ser exercitada sobre banco P0 real.
- Build APK e validação física no Moto G06 permanecem pendentes.
