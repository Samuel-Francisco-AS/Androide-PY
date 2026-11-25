# ADR-0002: Chaquopy como runtime Python inicial

- **Status:** Validado na P0 (Chaquopy 17.2.0 / Python 3.13, testes e validação física relatados)
- **Data:** 2026-11-24

## Contexto

O aplicativo precisa executar scripts Python localmente dentro de uma aplicação Android. A integração deve permitir que Kotlin inicialize o runtime, envie código/parâmetros e receba resultados e erros.

## Decisão

Adotar Chaquopy como primeira opção de runtime Python embarcado para o P0. A escolha é provisória até que a compilação, execução, integração nativa e compatibilidade das dependências sejam comprovadas no dispositivo de validação.

A arquitetura deverá encapsular o runtime atrás de uma interface interna para permitir substituição futura sem reescrever o motor de automação.

## Alternativas consideradas

- **Kivy/python-for-android:** alternativa viável, especialmente se a interface também fosse Python.
- **Runtime Python próprio ou outra integração CPython:** maior controle potencial, mas maior custo inicial de manutenção e integração.

## Consequências

### Positivas
- Integração direta com projeto Android/Gradle.
- Possibilidade de manter a interface e serviços nativos.
- Execução de scripts Python locais.

### Custos e riscos
- Nem todos os pacotes PyPI possuem binários compatíveis.
- Bibliotecas com extensões nativas exigem validação específica.
- Recursos como multiprocessing e comportamento de processos não devem ser presumidos equivalentes a um ambiente desktop.
- O runtime embarcado não fornece sandbox de segurança por si só.

## Validação

No P0, verificar:
1. Compatibilidade entre versões de Gradle, Android Gradle Plugin, SDK e Chaquopy.
2. Inicialização no dispositivo físico.
3. Execução e captura de stdout/stderr/exceções.
4. Chamada Python → Kotlin → API Android.
5. Comportamento de memória e responsividade.
6. Compatibilidade das dependências Python efetivamente necessárias.

Se o P0 revelar bloqueios estruturais, revisar este ADR antes de expandir o runtime.
