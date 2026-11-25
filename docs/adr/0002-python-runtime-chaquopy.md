# ADR-0002: Chaquopy como runtime Python inicial

- **Status:** Validado na P0 (Chaquopy 17.2.0 / Python 3.13, testes e validação física relatados)
- **Data:** 2026-11-24

## Contexto

O aplicativo precisa executar scripts Python localmente dentro de uma aplicação Android. A integração deve permitir que Kotlin inicialize o runtime, envie código/parâmetros e receba resultados e erros.

## Decisão

Adotar Chaquopy como runtime Python embarcado inicial. A combinação Chaquopy 17.2.0/Python 3.13 foi validada no P0 para inicialização, execução de scripts, captura de saída/erros e chamadas à Android Bridge no Moto G06, conforme testes automatizados e relatos físicos registrados.

A decisão continua sujeita a revisão caso surjam limitações estruturais. A compatibilidade de pacotes adicionais deve ser validada individualmente por versão Python, ABI e disponibilidade para Chaquopy.

A arquitetura encapsula o runtime atrás de uma interface interna para permitir substituição futura sem reescrever o motor de automação.

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

No P0 foram registrados:
1. Build e testes JVM aprovados pelo usuário.
2. Inicialização e execução no Moto G06 reportadas.
3. Captura separada de stdout/stderr e apresentação de exceções.
4. Chamadas Python → Kotlin → API Android para informações do dispositivo e abertura de aplicativo.
5. Compatibilidade de pacotes adicionais permanece por validar individualmente.
