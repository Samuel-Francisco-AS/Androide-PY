# P0 — Plano de testes de integração

Este plano separa testes que podem rodar no Fedora sem aparelho dos testes que exigem o Android real. A preferência operacional é transferir o APK pelo fluxo já usado no My-CT; nenhum passo abaixo exige ADB ou USB.

## 1. Testes automatizados sem dispositivo

Na raiz do repositório:

```bash
PYTHONPATH=android/app/src/main/python python3.13 -m unittest discover -s android/app/src/test/python -v
```

No diretório `android`:

```bash
./gradlew :app:testDebugUnitTest --no-daemon --max-workers=2
```

Cobertura criada:

- Runner Python: stdout, stderr, exceção, SystemExit e truncamento.
- Facade Python: device info, launch bem-sucedido simulado, app ausente, tipo inválido e Bridge não configurada.
- Kotlin: parser do resultado, schema incompatível e validação de package name.
- Repositório: script default, timestamps, nomes, falhas de banco, Flow e cancelamento.

Esses testes foram escritos, mas ainda não foram executados.

## 2. Build do APK

No diretório `android`:

```bash
./gradlew :app:assembleDebug --no-daemon --max-workers=2
```

Artefato esperado:

`android/app/build/outputs/apk/debug/app-debug.apk`

O primeiro build precisa validar resolução real dos plugins, KSP/Room e empacotamento Chaquopy/Python 3.13.

## 3. Validação manual no Moto G06

1. Transferir o APK ao telefone pelo fluxo já utilizado no My-CT.
2. Instalar e abrir Androide PY.
3. Confirmar que o script default aparece no editor.
4. Executar `print("Olá, Androide PY!")` e conferir stdout.
5. Executar `import sys; print("aviso", file=sys.stderr)` e conferir stderr separado.
6. Executar `raise ValueError("falha de teste")` e conferir tipo, mensagem e traceback sem encerramento do app.
7. Inserir o exemplo de informações do aparelho; conferir manufacturer, brand, model, Android/API level e ABI.
8. Inserir o exemplo de abertura de aplicativo; substituir o pacote por um app instalado e conferir o resultado.
9. Tentar pacote inexistente e conferir erro estruturado sem crash.
10. Editar o script, salvar, fechar completamente o app, reabrir e confirmar que o código permanece.
11. Girar a tela durante uma execução curta e conferir que o executor continua e o console recupera o último estado.
12. Executar saída acima de 65.536 caracteres e conferir aviso de truncamento.

## 4. Evidências a registrar

- Resultado de cada comando automatizado, inclusive versão de Python e Gradle.
- Resultado completo do primeiro build ou o primeiro bloco `What went wrong`.
- Versão Android e ABI exibidas por `mobile.device.get_info()`.
- Resultado das ações de abrir app instalado e pacote inexistente.
- Confirmação de persistência após fechar/reabrir.
- Problemas de layout, teclado, rotação e leitor de tela.

## Limites

- Não usar ADB como requisito.
- Não declarar P0 aprovada antes de observar os oito critérios oficiais no Moto G06.
- A execução Python não é sandbox; scripts são considerados código confiável do próprio usuário.
- Processo Android encerrado pelo sistema encerra execução Python em andamento; retomada persistente será tratada em fase posterior.
