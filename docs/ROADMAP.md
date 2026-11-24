# Roadmap de Desenvolvimento

**Projeto:** Androide PY  
**Status:** planejamento inicial  
**Estratégia:** entregas incrementais, testes automatizados e validação física.

## Diretrizes

- Cada marco deve produzir um resultado demonstrável.
- Não avançar com base apenas em compilação: validar o comportamento em dispositivo físico.
- Registrar decisões técnicas em ADRs.
- Manter o escopo pequeno o suficiente para diagnosticar falhas.
- Tratar capacidades Android como adaptadores incrementais.
- Revisar arquitetura e documentação quando evidências invalidarem uma premissa.

## P0 — Runtime Python e Android Bridge mínima

**Objetivo:** provar que um APK Android consegue executar Python e invocar capacidades Android por uma API própria.

### Escopo
- Projeto Android nativo com Kotlin e Jetpack Compose.
- Integração inicial do Chaquopy.
- Tela simples com editor/campo de código, Executar e console.
- Execução fora da thread principal.
- Captura de stdout, stderr e exceções.
- API Python inicial `mobile.device.get_info()`.
- API Python inicial `mobile.apps.launch(package_name)`.
- Persistência local do script de teste.

### Critérios de aceite
1. APK instala e abre no dispositivo de validação.
2. Runtime Python inicializa.
3. Um script Python é executado a partir da interface.
4. stdout e stderr aparecem no console.
5. Exceção Python é apresentada sem encerrar o aplicativo.
6. Python consulta informações básicas do dispositivo.
7. Python inicia um aplicativo instalado por uma API Android autorizada.
8. Script permanece salvo após fechar e reabrir a aplicação.

### Fora do P0
- Editor profissional completo.
- Execução em segundo plano.
- Acessibilidade.
- Shizuku/ADB/root.
- Múltiplos workers.
- Workflow visual.
- Sandbox para scripts não confiáveis.

**Gate:** todos os critérios de aceite demonstrados em dispositivo físico e testes de integração essenciais aprovados.

## P1 — Biblioteca e gerenciamento de scripts

**Objetivo:** transformar a prova de conceito em um ambiente utilizável de criação e execução.

Entregas:
- Biblioteca de scripts.
- Criar, importar, renomear, duplicar e excluir.
- Editor com realce de sintaxe e numeração de linhas.
- Metadados, tags e descrição.
- Parâmetros de entrada.
- Histórico de versões inicial.
- Console por execução.
- Registro de execuções e erros.
- Gerenciamento de dependências compatíveis.

**Gate:** ciclo completo de criar/importar, editar, salvar, executar e consultar histórico validado.

## P2 — Integração de acessibilidade

**Objetivo:** permitir automações determinísticas de interface por meio de um serviço nativo autorizado.

Entregas:
- AccessibilityService.
- Configuração e explicação de autorização.
- Consulta controlada da árvore de acessibilidade.
- Localização de elementos por atributos disponíveis.
- Clique, digitação, rolagem e gestos.
- API Python `mobile.screen`.
- Verificação de disponibilidade da capacidade.

**Gate:** testes em aplicativos de demonstração e aplicativos reais selecionados, documentando elementos não acessíveis e limitações.

## P3 — Notificações e eventos

**Objetivo:** permitir automações acionadas por eventos selecionados.

Entregas:
- NotificationListenerService.
- Registro e revogação da autorização.
- Filtros por pacote e condições.
- Normalização de eventos.
- API Python `mobile.notifications`.
- Roteamento de eventos para automações inscritas.

**Gate:** evento autorizado recebido, filtrado e processado sem depender da interface aberta.

## P4 — Agendamento e execução em segundo plano

**Objetivo:** executar trabalhos locais programados com mecanismos Android apropriados.

Entregas:
- Integração com WorkManager.
- Integração com AlarmManager quando aplicável.
- Foreground Service para tarefas prolongadas elegíveis.
- Recorrências e execução única.
- Política de retries.
- Recuperação de estado após reinicialização quando suportada.
- Interface de estado e diagnóstico de agendamentos.

**Gate:** cenários de agendamento validados com documentação de atrasos, permissões e restrições por versão do Android/fabricante.

## P5 — Workflow Engine

**Objetivo:** compor automações em fluxos reutilizáveis.

Entregas:
- Modelos de workflow e etapas.
- Dependências sequenciais.
- Condições de sucesso/falha.
- Passagem de dados entre etapas.
- Repetição configurável.
- Cancelamento e tratamento de falhas.
- Logs por etapa.
- Visualização inicial do fluxo.

**Gate:** fluxos sequenciais e condicionais testados, inclusive em cenários de falha e retomada.

## P6 — Integrações avançadas opcionais

**Objetivo:** ampliar capacidades em dispositivos e ambientes que ofereçam mecanismos adicionais.

Entregas possíveis:
- Adaptador Shizuku.
- Operações ADB autorizadas.
- Detecção de disponibilidade e estado de conexão.
- Adaptador root, somente se houver justificativa e ambiente de teste.
- Catálogo de capacidades e alternativas.

**Gate:** cada adaptador deve ter documentação de pré-requisitos, permissões, riscos e testes próprios. O núcleo deve continuar funcional sem eles.

## P7 — Segurança, robustez e refinamento

**Objetivo:** preparar a plataforma para uso mais confiável e demonstração técnica.

Entregas:
- Modelo de confiança para scripts importados.
- Controles de recursos e execução.
- Estratégia de isolamento avaliada.
- Proteção de credenciais.
- Backup e restauração.
- Observabilidade e diagnóstico.
- Otimização de bateria e memória.
- Testes de regressão e compatibilidade.
- Documentação de instalação e demonstração.

**Gate:** riscos conhecidos documentados, testes críticos aprovados e limitações transparentes.

## Dependências entre marcos

```text
P0 → P1 → P2 → P3 → P4 → P5
                  │           │
                  └──── P6 ───┘
                         │
                         P7
```

A sequência é orientativa. P6 pode ser desenvolvido em paralelo após a estabilização das interfaces da Android Bridge. P7 é transversal e deve receber melhorias desde os primeiros marcos, mesmo que seu gate final ocorra mais tarde.

## Política de conclusão

Um marco só será considerado concluído quando:
- o escopo estiver implementado;
- os critérios de aceite forem verificados;
- os testes relevantes estiverem registrados;
- a validação física necessária estiver concluída;
- a documentação refletir o comportamento real;
- limitações e débitos conhecidos estiverem registrados.
