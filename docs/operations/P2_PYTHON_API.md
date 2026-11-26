# P2.0.7 — Contrato público da API Python `mobile.screen`

**Estado:** contrato público v1 definido; módulo Python `mobile.screen` ainda não implementado. A P2.1-B preparou somente um método JSON interno na Bridge para consulta de estado; ele não é chamado pela fachada Python nem anunciado no catálogo.  
**Branch:** `feat/p2-accessibility-automation`  
**Data:** 2026-11-25  
**Dependência:** `P2_ASYNC_PROTOCOL.md` (protocolo interno v1).

## 1. Objetivo e fronteira pública

A API `mobile.screen` oferece ao script Python operações síncronas e previsíveis para consultar o estado do serviço de acessibilidade, interagir geometricamente com a tela e criar/reutilizar Mapas de Coordenadas.

A implementação Kotlin permanece assíncrona internamente conforme o protocolo P2.0.6. A espera ocorre somente no worker serial Python existente. A API não expõe objetos Android, nós de acessibilidade, handlers, futures ou detalhes do lifecycle.

Esta especificação define contratos futuros. **Nenhuma função descrita aqui está implementada ou disponível no aplicativo neste momento.**

## 2. Política de retorno e erros

- Sucesso: dicionários, listas e valores Python simples, serializáveis.
- Falhas operacionais: exceção pública `mobile.screen.ScreenError`, derivada de `RuntimeError`.
- Argumentos inválidos: `TypeError` para tipo incorreto e `ValueError` para valor fora do domínio/limites.
- Bridge ausente ou não configurada: `ScreenError(code="BRIDGE_NOT_CONFIGURED")`.
- `ScreenError` expõe os atributos `code` (identificador estável), `retryable` (booleano) e `request_id` (UUID ou `None` quando não houve solicitação).
- O texto de `str(error)` é explicativo; scripts devem tomar decisões pelo atributo `code`, nunca analisar a mensagem.
- A API legada `mobile.apps.launch()` mantém seu contrato atual de retorno estruturado com `ok`; P2 não o altera.

Exemplo de tratamento:

```python
from mobile.screen import ScreenError, tap

try:
    result = tap(500, 800)
except ScreenError as error:
    print(error.code, error.retryable)
```

## 3. Estado e disponibilidade

### `get_status() -> dict`

Consulta o estado atual sem abrir Configurações, solicitar autorização ou tentar reconectar o serviço.

Retorno:

```python
{
    "enabled": True,
    "connected": True,
    "state": "connected"
}
```

Estados estáveis:
- `not_enabled`: o usuário não habilitou o serviço nas configurações Android.
- `disconnected`: a autorização está habilitada, mas o Android ainda não vinculou uma instância operacional.
- `connected`: o serviço está conectado e o gateway pode encaminhar operações.

`enabled` representa autorização/configuração do sistema; `connected` representa conexão efetiva publicada pelo lifecycle. Um não deve ser inferido do outro. A consulta não garante que uma operação específica seja aceita pelo aplicativo de destino.

## 4. Interação geométrica

Todas as operações de interação são síncronas para o script, recebem timeout finito em milissegundos e retornam um dicionário com pelo menos:

```python
{
    "request_id": "uuid",
    "operation": "screen.tap",
    "completed": True
}
```

`completed` significa que a operação Android alcançou o estado terminal observável pelo adaptador (por exemplo, callback de gesto concluído ou ação de nó aceita). **Não significa que o aplicativo de destino realizou a intenção do usuário**, como abrir uma tela, enviar um formulário ou alterar um dado.

Timeout padrão de interação: `10_000` ms. Limite público: de 1 a 120.000 ms. O prazo inclui fila e execução, conforme P2.0.6.

### `tap(x, y, *, coordinate_space="pixels", timeout_ms=10_000) -> dict`

Executa um toque simples em uma posição da tela.

- `coordinate_space`: `"pixels"` ou `"normalized"`.
- Pixels: coordenadas de tela em pixels lógicos Android, com origem no canto superior esquerdo do display.
- Normalizadas: valores reais no intervalo inclusivo [0, 1], relativos à largura/altura total do display; 1.0 é convertido para o último pixel válido.
- Valores não finitos, negativos ou fora dos limites são rejeitados localmente.
- O alvo é geométrico; não há identificação semântica do elemento.

### `type_text(text, *, timeout_ms=10_000) -> dict`

Insere texto no campo editável atualmente focado.

- Exige campo editável focado e suporte do nó à ação Android `ACTION_SET_TEXT`.
- A operação substitui o conteúdo atual do campo; não promete anexar texto ao final.
- Não tenta descobrir o campo por coordenadas e não promete fallback universal por teclas.
- Campos sem foco, somente leitura, protegidos ou que não aceitem a ação podem produzir `ACTION_UNSUPPORTED` ou `ACTION_REJECTED`.
- Texto digitado não deve ser incluído em logs por padrão.

### `swipe(x1, y1, x2, y2, *, coordinate_space="pixels", duration_ms=350, timeout_ms=10_000) -> dict`

Executa um gesto de deslize entre dois pontos.

- Coordenadas seguem a mesma convenção de `tap`.
- `duration_ms` é inteiro positivo entre 1 e 10.000 ms.
- Gestos são serializados pelo coordenador nativo; novo despacho não pode cancelar inadvertidamente um gesto ainda ativo.
- O callback terminal informa conclusão/cancelamento do gesto, não o efeito semântico no app.

### `scroll(direction="down", *, distance=0.7, duration_ms=350, timeout_ms=10_000) -> dict`

Atalho geométrico de rolagem que executa um gesto vertical ou horizontal a partir do centro do display.

- `direction`: `"up"`, `"down"`, `"left"` ou `"right"`.
- `distance`: fração entre 0 e 1 (exclusive 0, inclusive 1) do eixo correspondente do display.
- `duration_ms`: mesmo limite de `swipe`.
- O centro do display é a âncora inicial; esta primeira versão não recebe seletor de contêiner nem garante rolagem de uma região específica.
- A implementação usa o mesmo mecanismo serializado de gestos. A confirmação de movimento real do conteúdo não é prometida.

## 5. Mapa de Coordenadas

O mapa é um perfil local persistente, com pontos nomeados e metadados de calibração. O overlay de seleção intercepta intencionalmente os toques enquanto estiver ativo. O script recebe apenas dados serializáveis.

Cada mapa contém:
- `schema_version`, `name` e data de criação;
- contexto de display: largura/altura em pixels, densidade e rotação;
- pacote do aplicativo em primeiro plano no momento da captura, quando identificável;
- lista de pontos nomeados, cada um com `x_px`, `y_px`, `x_normalized` e `y_normalized`.

Coordenadas normalizadas são calculadas em relação ao display completo. Elas permitem reprojeção para outra resolução, mas não corrigem mudanças de layout, conteúdo, barras do sistema ou escala do aplicativo.

### `capture_coordinates(name, *, delay_ms=30_000, timeout_ms=120_000) -> dict`

Inicia a captura de um mapa novo.

- `name` é obrigatório, não vazio após trim e limitado a 80 caracteres.
- `delay_ms` define a espera antes da ativação do overlay; zero inicia imediatamente. Limite: 0 a 120.000 ms.
- `timeout_ms` limita a sessão inteira de captura, incluindo seleção e conclusão; limite: 1.000 a 300.000 ms.
- O usuário pode selecionar vários pontos, atribuir nomes, corrigir/refazer um ponto, concluir ou cancelar.
- O nome de mapa já existente produz `ScreenError(code="MAP_ALREADY_EXISTS")`; a captura não sobrescreve mapas silenciosamente.
- Cancelamento pelo usuário produz `ScreenError(code="CANCELLED")`; nenhum mapa parcial é persistido.
- A persistência deve ocorrer por repositório Kotlin/local do aplicativo, nunca por escrita direta do Python no banco.

### `list_coordinate_maps() -> list[dict]`

Lista mapas persistidos, retornando resumos com nome, data de criação e quantidade de pontos. Não inclui conteúdo de tela.

### `get_coordinate_map(name) -> dict`

Retorna o mapa completo pelo nome. Nome inexistente produz `ScreenError(code="MAP_NOT_FOUND")`.

### `delete_coordinate_map(name) -> dict`

Exclui o mapa pelo nome e retorna `{"name": name, "deleted": True}`. Nome inexistente produz `ScreenError(code="MAP_NOT_FOUND")`.

### `tap_point(map_name, point_name, *, timeout_ms=10_000) -> dict`

Executa toque usando as coordenadas normalizadas de um ponto salvo.

- Reprojeta o ponto para as dimensões atuais do display.
- Quando o contexto capturado estiver disponível, exige correspondência do pacote em primeiro plano e da orientação; divergência produz `ScreenError(code="MAP_CONTEXT_MISMATCH")`.
- Mudanças de layout dentro do mesmo aplicativo ainda podem invalidar o ponto; o mapa não é reconhecimento semântico.
- Mapa ou ponto inexistente produz `ScreenError(code="MAP_NOT_FOUND")` ou `ScreenError(code="MAP_POINT_NOT_FOUND")`.

## 6. Códigos de erro públicos

A fachada preserva os códigos do protocolo P2.0.6 e acrescenta os erros de domínio abaixo:

| Código | Significado |
|---|---|
| `BRIDGE_NOT_CONFIGURED` | Bridge não configurada no runtime |
| `SERVICE_NOT_ENABLED` | Serviço não habilitado pelo usuário |
| `SERVICE_DISCONNECTED` | Serviço autorizado, mas sem conexão operacional |
| `SERVICE_INTERRUPTED` | Serviço interrompido |
| `QUEUE_FULL` | Limite de solicitações pendentes atingido |
| `TIMEOUT` | Prazo da operação expirou |
| `CANCELLED` | Operação/captura cancelada |
| `ACTION_UNSUPPORTED` | Alvo não oferece a ação solicitada |
| `ACTION_REJECTED` | Android/alvo rejeitou a operação |
| `MAP_ALREADY_EXISTS` | Já existe mapa com o nome informado |
| `MAP_NOT_FOUND` | Mapa não encontrado |
| `MAP_POINT_NOT_FOUND` | Ponto não encontrado no mapa |
| `MAP_CONTEXT_MISMATCH` | Pacote/orientação não corresponde ao contexto capturado |
| `INTERNAL_ERROR` | Falha nativa inesperada |

Erros de validação de argumentos ocorrem antes da solicitação e usam `TypeError`/`ValueError`, sem `request_id`.

## 7. Integração com a biblioteca `mobile`

- Módulo público: `mobile.screen`; exceção: `mobile.screen.ScreenError`.
- Identificadores planejados no catálogo de capacidades: `screen.get_status`, `screen.tap`, `screen.type_text`, `screen.swipe`, `screen.scroll`, `screen.capture_coordinates`, `screen.list_coordinate_maps`, `screen.get_coordinate_map`, `screen.delete_coordinate_map`, `screen.tap_point`.
- Essas capacidades só poderão ser anunciadas como implementadas no catálogo após cada operação existir e passar pelos testes correspondentes.
- A adição é compatível com `API_VERSION = 1`; não exige quebra do contrato existente. A versão da biblioteca só será alterada quando o módulo for efetivamente entregue.
- A API semântica (por exemplo, localizar/clicar elemento por texto ou atributo) permanece fora deste contrato e será definida na etapa correspondente à Prioridade C.

## 8. Limites de escopo e privacidade

Esta API inicial não captura screenshots, não faz OCR, não lê conteúdo de tela em lote e não monitora continuamente outros aplicativos. O Mapa de Coordenadas persiste somente posições e metadados mínimos de calibração; não persiste texto ou imagem da tela.

A revisão de privacidade P2.0.8 e os ADRs-0008/0009 foram concluídos e aceitos como direção para a implementação. A API pública aqui definida permanece estável; os controles de consentimento e retenção deverão ser implementados na UI hospedeira e na camada nativa, sem permitir que o script conceda a própria autorização.

## 9. Verificação e estado

- Revisão estática do contrato contra `mobile/__init__.py`, `device.py`, `apps.py`, `capabilities.py`, `runner.py`, testes Python, `AndroidBridgeAdapter`, `ScriptExecutor` e protocolo P2.0.6.
- Na revisão documental original, nenhum código Python/Kotlin havia sido alterado; a implementação posterior da P2.1-B adicionou somente um método JSON interno Kotlin à Bridge, sem criar módulo Python ou capacidades de tela.
- A validação desta API pública continua pendente; nenhum `mobile.screen` é anunciado no catálogo.
- Preparação documental P2.0.8–P2.0.10 concluída. P2.1-A foi compilada localmente; P2.1-B está em implementação e ainda aguarda build/testes; P2.1-C fará a validação integrada e física.
- O contrato descreve funções futuras; `mobile.screen` e suas capacidades não estão implementadas nem anunciadas pelo catálogo atual.
