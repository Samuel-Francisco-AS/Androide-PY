# ADR-0004: Android Bridge baseada em capacidades e adaptadores

- **Status:** Aceito
- **Data:** 2026-11-24

## Contexto

As automações precisam acessar recursos heterogêneos do Android. Algumas operações estão disponíveis por APIs comuns; outras exigem permissões especiais, serviços nativos ou ferramentas opcionais como Shizuku. A disponibilidade varia por versão, fabricante, configuração e autorização do usuário.

## Decisão

Criar uma Android Bridge com API Python de alto nível e adaptadores nativos. Cada capacidade terá identificador, requisitos, estado de disponibilidade, limitações e implementação responsável.

Os scripts não devem conhecer detalhes de Kotlin, Java, JNI ou o mecanismo concreto utilizado para realizar uma operação.

Exemplos de capacidades:
- `apps.launch`
- `screen.accessibility`
- `notifications.observe`
- `files.read_scoped`
- `system.shizuku`
- `system.root`

A disponibilidade deve ser consultável em tempo de execução. Adaptadores opcionais não podem ser dependências do núcleo.

## Alternativas consideradas

- **Uma API monolítica com acesso direto a todas as classes Android:** acoplamento alto e permissões difíceis de explicar.
- **Depender exclusivamente de AccessibilityService:** não atende operações que possuem APIs Android próprias e não fornece acesso irrestrito.
- **Depender de Shizuku/ADB:** exclui dispositivos sem essa configuração e torna o núcleo dependente de um mecanismo externo.

## Consequências

### Positivas
- Contratos Python estáveis.
- Integrações substituíveis e testáveis.
- Transparência sobre permissões e limitações.
- Núcleo funcional mesmo sem mecanismos avançados.

### Custos e riscos
- É necessário manter contratos e testes por adaptador.
- Uma operação pode ter diferentes níveis de disponibilidade.
- Algumas capacidades não terão alternativa quando o Android bloquear o acesso.

## Validação

O P0 implementará capacidades básicas de consulta do dispositivo e abertura de aplicativos. P2, P3 e P6 validarão adaptadores de acessibilidade, notificações e integrações avançadas, respectivamente.
