# ADR-0001: Aplicativo hospedeiro Android nativo

- **Status:** Aceito
- **Data:** 2026-11-24

## Contexto

O produto depende de integração profunda com APIs Android, ciclo de vida de serviços, permissões especiais, notificações, agendamentos e acessibilidade. A linguagem principal da lógica de automação deve ser Python, mas a camada de integração com o sistema precisa seguir os contratos nativos do Android.

## Decisão

Construir o aplicativo hospedeiro em Kotlin, utilizando Jetpack Compose para a interface. Python será incorporado como runtime e será a linguagem principal para scripts, APIs de automação e lógica do motor.

Kotlin não deve absorver desnecessariamente a lógica de automação; sua responsabilidade é hospedar a aplicação e integrar capacidades nativas.

## Alternativas consideradas

- **Kivy/python-for-android:** maior participação de Python na interface, porém exigiria avaliar o custo de integração e manutenção de serviços Android específicos.
- **Flutter ou React Native:** adicionariam outra camada de interface sem eliminar a necessidade de integração nativa.
- **Aplicação inteiramente Kotlin:** simplificaria a integração Android, mas contrariaria a proposta Python-first.

## Consequências

### Positivas
- Acesso direto às APIs e ao ciclo de vida Android.
- Interface nativa.
- Fronteira clara entre lógica Python e integração do sistema.
- Melhor controle sobre serviços e permissões.

### Custos e riscos
- O projeto terá duas linguagens principais.
- Contratos Kotlin/Python precisam ser projetados e testados.
- Parte das funcionalidades exige implementação nativa.

## Validação

O P0 deverá comprovar que a interface Kotlin consegue executar Python e invocar ao menos duas capacidades Android por uma API Python própria.
