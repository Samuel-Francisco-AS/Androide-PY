# Instruções operacionais para agentes — Androide PY

Estas instruções são obrigatórias para qualquer agente humano ou automatizado que leia, implemente, revise ou documente este repositório. Aplicam-se a todas as fases, especialmente à P0.

## 1. Antes de qualquer trabalho

Não comece a editar código, arquivos de produção, configurações ou documentação sem primeiro:

1. Ler este arquivo (`AGENTS.md`) integralmente.
2. Ler `docs/operations/P0_EXECUTION_CHECKLIST.md` integralmente quando o trabalho pertencer à P0; em fases posteriores, consultar o checklist operacional vigente e criar/atualizar um quando necessário.
3. Ler `README.md` e `docs/README.md`.
4. Ler os documentos relevantes ao escopo:
   - `docs/PROJECT_VISION.md`
   - `docs/ARCHITECTURE.md`
   - `docs/ROADMAP.md`
   - ADRs aplicáveis em `docs/adr/`
5. Conferir o estado atual da branch, os arquivos existentes, alterações recentes e testes relacionados. Não presumir que uma conversa antiga descreve o estado atual do repositório.

Se um documento referenciado não existir ou estiver inacessível, registrar a lacuna e não inventar seu conteúdo.

## 2. Ciclo obrigatório por subetapa

O trabalho deve seguir, sem pular etapas, este ciclo:

**CONFERE → IMPLEMENTA → CONFERE → MARCA COMO CONCLUÍDA → CONFERE → IMPLEMENTA A PRÓXIMA SUBETAPA NÃO CONCLUÍDA**

### A. CONFERE — antes de implementar

- Reler o item e o contexto no checklist operacional.
- Reler os trechos relevantes da visão, arquitetura, roadmap e ADRs.
- Inspecionar o código e as convenções já existentes.
- Confirmar dependências, versões e APIs em documentação oficial quando a correção depender delas.
- Definir o resultado esperado e como será verificado.
- Identificar conflitos, riscos e dependências ainda não resolvidas.

### B. IMPLEMENTA

- Implementar somente a subetapa selecionada e suas dependências estritamente necessárias.
- Preferir alterações pequenas, coesas e revisáveis.
- Preservar decisões arquiteturais aceitas.
- Não introduzir dependências, permissões, serviços, ferramentas externas ou custos sem justificativa e autorização quando aplicável.
- Não copiar cegamente configurações de outros projetos.
- Não alegar que código foi executado, compilado ou testado quando apenas foi escrito ou revisado.

### C. CONFERE — depois de implementar

- Revisar o diff completo e os arquivos alterados.
- Conferir nomes, caminhos, imports, contratos, tratamento de erros, segurança e consistência documental.
- Executar os testes e verificações disponíveis no ambiente real da ferramenta. Se não houver ambiente de build Android, declarar essa limitação.
- Distinguir claramente: revisão estática, teste automatizado executado, build executado e validação física.
- Corrigir problemas encontrados antes de concluir a subetapa.

### D. MARCA COMO CONCLUÍDA

- Atualizar o checklist operacional somente depois da conferência.
- Marcar como concluído apenas o item efetivamente realizado e respaldado por evidência.
- Registrar a evidência concreta: commit, caminho, teste/comando e resultado.
- Se a implementação foi escrita, mas não executada, usar uma descrição explícita como “implementação publicada; build local pendente”, sem marcar validação de build como concluída.
- Se depender do usuário ou de dispositivo físico, manter o item pendente até receber o resultado.

### E. CONFERE — antes de avançar

- Reler a versão recém-publicada do checklist.
- Confirmar que o estado e a evidência ficaram corretos.
- Escolher a próxima subetapa não concluída cujas dependências estejam satisfeitas.
- Repetir o ciclo desde o início.

Não executar várias subetapas independentes em lote sem as conferências intermediárias. Não marcar uma fase inteira como concluída apenas porque seus arquivos foram criados.

## 3. Regras de verdade e evidência

Use estados distintos e não intercambiáveis:

- **Escrito/publicado:** conteúdo foi criado no repositório remoto.
- **Revisado:** conteúdo foi inspecionado; isso não prova execução.
- **Testado:** um teste específico foi realmente executado e seu resultado foi observado.
- **Compilado:** o comando de build foi realmente executado e terminou com sucesso.
- **Validado fisicamente:** o usuário confirmou o comportamento no dispositivo real.

Nunca converter um estado em outro por inferência. Uma resposta positiva da API GitHub confirma publicação, não compilação. Um diff revisado não confirma execução. Um APK gerado não confirma que o aplicativo funciona corretamente no Moto G06.

## 4. GitHub e fluxo de trabalho

- Trabalhar em branch dedicada para a entrega; não publicar implementação diretamente em `main`.
- Antes de escrever, confirmar branch-base e estado remoto.
- Fazer commits pequenos, com mensagens descritivas e escopo claro.
- Não sobrescrever alterações concorrentes sem buscar o estado mais recente.
- Conferir os arquivos remotos após publicação.
- Informar ao usuário branch, commits, arquivos alterados e comandos necessários para sincronizar.
- O usuário prefere sincronizar por SSH no Fedora e realizar builds/testes localmente.
- Não exigir ADB nem conexão USB com o telefone. O usuário transfere o APK ao Moto G06 pelo fluxo que já utiliza no My-CT.

## 5. Restrições arquiteturais do Androide PY

- Kotlin e Jetpack Compose hospedam a aplicação e fazem integração nativa Android.
- Python é a linguagem principal das automações e do motor.
- Chaquopy é a opção inicial de runtime, sujeita à validação real no P0.
- Room/SQLite é a persistência relacional; acesso Python a tabelas Room deve ocorrer por contratos/repositórios Kotlin, nunca por escrita direta.
- Android Bridge deve expor APIs Python estáveis e capacidades explícitas.
- Shizuku, ADB e root são opcionais e não podem ser dependências do núcleo.
- A primeira política de concorrência é conservadora: uma execução Python ativa por vez; não matar threads Python arbitrariamente.
- Scripts são código confiável do próprio usuário na primeira etapa; não declarar sandbox ou isolamento que não exista.
- Não prometer execução em segundo plano, alarmes exatos, controle universal de apps ou compatibilidade irrestrita com PyPI.
- Respeitar o escopo e os critérios de aceite definidos em `docs/ROADMAP.md`.

## 6. Documentação e mudanças arquiteturais

- Documentação deve refletir comportamento implementado, não intenção como se fosse funcionalidade pronta.
- Mudanças relevantes de arquitetura, dependências estruturais, segurança ou contratos devem atualizar os documentos afetados e, quando apropriado, criar/atualizar ADR.
- Não remover limitações documentadas sem evidência.
- Manter o checklist operacional atualizado durante o trabalho; ele é o registro de progresso, não uma lista decorativa.

## 7. Comunicação de progresso

Ao relatar uma entrega, separar claramente:

1. O que foi implementado e publicado.
2. O que foi revisado.
3. Quais testes foram efetivamente executados e seus resultados.
4. O que ainda depende de build local ou validação física do usuário.
5. Qual é a próxima subetapa não concluída.

Se houver bloqueio, explicar a causa e manter o item bloqueado ou pendente. Não fingir progresso para manter o fluxo.
