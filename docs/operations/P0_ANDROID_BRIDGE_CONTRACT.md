# P0 — Contrato da Android Bridge

**Versão:** 1  
**Capacidades incluídas:** `mobile.device.get_info()`, `mobile.apps.launch(package_name)`

A Bridge é implementada em Kotlin e exposta ao Python por adaptadores. Os scripts usam somente funções Python de alto nível e estruturas primitivas/dicionários; não recebem Context, Activity, PackageManager ou objetos Android arbitrários.

## Inicialização

1. O host inicia Chaquopy em uma thread de trabalho.
2. O host instancia `AndroidBridgeAdapter` com application context.
3. O host passa esse adaptador ao módulo Python `mobile.configure(bridge)`.
4. Os módulos `mobile.device` e `mobile.apps` chamam métodos específicos do adaptador e convertem JSON em dicionários Python.

Se a Bridge não tiver sido configurada, a API Python levanta `RuntimeError` com mensagem clara. O runner converte essa exceção em resultado FAILED, sem encerrar o aplicativo.

## `mobile.device.get_info()`

Retorna um dicionário com informações não sensíveis:

- `manufacturer`
- `brand`
- `model`
- `device`
- `android_release`
- `api_level`
- `supported_abis`
- `app_package_name`
- `app_version_name`

Não coleta serial, IMEI, Android ID, endereço MAC, advertising ID, número telefônico, localização ou contas.

Não requer permissão Android em tempo de execução.

## `mobile.apps.launch(package_name)`

Recebe o nome completo do pacote Android e retorna um dicionário:

Sucesso:
```python
{"ok": True, "package_name": "com.example.app", "error": None}
```

Falha:
```python
{
    "ok": False,
    "package_name": "com.example.app",
    "error": {
        "code": "APP_NOT_FOUND_OR_NOT_VISIBLE",
        "message": "O aplicativo não foi encontrado ou não está visível para o Androide PY."
    }
}
```

Códigos previstos:

- `INVALID_PACKAGE_NAME`
- `APP_NOT_FOUND_OR_NOT_VISIBLE`
- `LAUNCH_ACTIVITY_NOT_FOUND`
- `LAUNCH_DENIED`
- `LAUNCH_FAILED`

O adaptador usa `PackageManager.getLaunchIntentForPackage` e `Context.startActivity` com `FLAG_ACTIVITY_NEW_TASK`. A consulta de pacotes é limitada por uma declaração `<queries>` para atividades MAIN/LAUNCHER. Não solicita `QUERY_ALL_PACKAGES`.

A API inicia apenas atividades lançáveis que o Android permite consultar. Não promete iniciar componentes privados, serviços, activities sem launcher ou aplicativos bloqueados por políticas do sistema.

Não requer permissão Android em tempo de execução, mas depende das regras de package visibility do Android 11+.

## Falhas e limites

- Pacote inexistente e pacote invisível podem ser indistinguíveis pelo PackageManager; ambos retornam `APP_NOT_FOUND_OR_NOT_VISIBLE`.
- Uma Activity pode desaparecer ou ser bloqueada entre a consulta e o início; erros são convertidos em códigos estáveis.
- A Bridge não tenta contornar restrições Android, permissões, perfis de trabalho ou controles do fabricante.
- Os adaptadores usam application context e não retêm Activity.
- A disponibilidade futura de cada capacidade poderá ser consultada por um registro de capacidades, mas esse registro não é necessário para estas duas funções iniciais.

## Testes

- Python: módulos testáveis com um adaptador falso, sem Android/ADB.
- Kotlin: validar nomes de pacote e respostas estruturadas; a integração PackageManager depende de validação no Android.
- Validação física: executar as duas APIs no Moto G06 e confirmar comportamento de app instalado e pacote inexistente.
