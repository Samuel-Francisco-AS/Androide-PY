# ADR-0007: Política de preservação de dados locais durante o desenvolvimento

- **Status:** Aceito
- **Data:** 2026-11-25
- **Escopo:** desenvolvimento e validação do Androide PY

## Contexto

Durante as fases iniciais do Androide PY, os scripts e registros armazenados no aplicativo são predominantemente dados de teste. Exigir compatibilidade retroativa e preservação integral desses dados em toda mudança de esquema pode aumentar desnecessariamente a complexidade, o custo de implementação e o tempo de validação.

Ao mesmo tempo, podem existir momentos em que o usuário deseje preservar dados específicos de uma instalação ou versão. Essa necessidade deve ser explícita, em vez de presumida em todas as fases.

## Decisão

A preservação de dados locais **não é um requisito padrão** durante o desenvolvimento do Androide PY.

A regra operacional é:

1. Por padrão, é permitido recriar, limpar ou substituir o banco de dados local e descartar scripts e registros de teste quando isso simplificar ou tornar mais confiável o desenvolvimento.
2. Migrações retrocompatíveis e preservação de dados de versões anteriores só serão requisitos quando o usuário solicitar expressamente a preservação dos dados e scripts da versão atual ou de uma versão identificada.
3. Quando uma operação puder apagar ou tornar inacessíveis dados locais, a documentação operacional e as instruções de teste devem informar esse efeito antes da execução.
4. Mesmo quando a preservação não for necessária, continuam obrigatórios os cuidados com integridade transacional, consistência do esquema, tratamento de erros e prevenção de corrupção acidental.
5. A ausência de requisito de preservação não autoriza apagar dados fora do escopo do ambiente de desenvolvimento ou executar ações destrutivas sem comunicar seu impacto.

## Consequências

### Benefícios

- Permite simplificar mudanças de esquema durante prototipagem e desenvolvimento.
- Reduz a necessidade de manter caminhos de migração que não agregam valor aos dados descartáveis.
- Facilita testes limpos e reprodutíveis.

### Custos e limitações

- Uma atualização pode exigir limpeza de dados, reinstalação ou recriação do banco.
- Dados que o usuário passe a considerar importantes precisam ser explicitamente protegidos antes de operações destrutivas.
- A decisão deve ser reavaliada quando o aplicativo passar a armazenar dados de uso real ou quando houver distribuição a outros usuários.

## Aplicação prática

Antes de cada alteração de persistência, identificar se há uma solicitação explícita de preservação:

- **Sem solicitação explícita:** escolher a estratégia tecnicamente mais simples e confiável; migração retrocompatível não é obrigatória.
- **Com solicitação explícita:** definir estratégia de migração ou backup, testar a preservação e documentar as versões de origem e destino.

Esta decisão complementa os ADRs sobre persistência e versionamento; não substitui os requisitos de qualidade do banco de dados.
