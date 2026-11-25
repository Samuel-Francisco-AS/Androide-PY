package com.samuel.androidepy.ui

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.samuel.androidepy.AndroidePyApplication
import com.samuel.androidepy.data.ScriptEntity
import com.samuel.androidepy.execution.ScriptExecutionRequest
import com.samuel.androidepy.execution.ScriptParameterResolver
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

private const val MAX_SCRIPT_BYTES = 1_048_576

@Composable
fun MainScreen(application: AndroidePyApplication) {
    val repository = application.scriptRepository
    val executionRepository = application.scriptExecutionRepository
    val executor = application.scriptExecutor
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scripts by repository.observeAll().collectAsState(initial = emptyList())

    var selectedScriptId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ScriptEntity?>(null) }
    var pendingExport by remember { mutableStateOf("") }
    var exportName by remember { mutableStateOf("script.py") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val fileName = withContext(Dispatchers.IO) {
                        queryDisplayName(context, uri)
                            ?: uri.lastPathSegment?.substringAfterLast('/')
                            ?: "script.py"
                    }
                    require(fileName.endsWith(".py", ignoreCase = true)) {
                        "Selecione um arquivo com extensão .py."
                    }
                    val source = withContext(Dispatchers.IO) { readUtf8Script(context, uri) }
                    val imported = withContext(Dispatchers.IO) {
                        repository.create(
                            name = fileName.substringBeforeLast('.').takeIf { it.isNotBlank() } ?: "Script importado",
                            sourceCode = source,
                            description = "Importado de arquivo Python"
                        )
                    }
                    selectedScriptId = imported.id
                    feedback = "Script importado. Revise o código antes de executar."
                    errorMessage = ""
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    errorMessage = error.message ?: "Não foi possível importar o arquivo."
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/x-python")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(pendingExport.toByteArray(StandardCharsets.UTF_8))
                        } ?: error("Não foi possível abrir o destino para exportação.")
                    }
                    feedback = "Arquivo Python exportado."
                    errorMessage = ""
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    errorMessage = error.message ?: "Não foi possível exportar o script."
                }
            }
        }
    }

    LaunchedEffect(executor, executionRepository) {
        executor.updates.collect { update ->
            try {
                withContext(Dispatchers.IO) { executionRepository.recordUpdate(update) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("AndroidePY.History", "Unable to persist execution update ${update.executionId}", error)
            }
        }
    }

    LaunchedEffect(scripts, initialized) {
        if (!initialized) {
            if (scripts.isEmpty()) {
                val defaultScript = withContext(Dispatchers.IO) { repository.getOrCreateDefault() }
                selectedScriptId = defaultScript.id
            } else {
                selectedScriptId = scripts.first().id
            }
            initialized = true
        } else if (selectedScriptId != null && scripts.none { it.id == selectedScriptId }) {
            selectedScriptId = scripts.firstOrNull()?.id
        }
    }

    val selectedScript = scripts.firstOrNull { it.id == selectedScriptId }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (selectedScript == null) {
            LibraryScreen(
                scripts = scripts,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                feedback = feedback,
                errorMessage = errorMessage,
                onCreate = {
                    scope.launch {
                        try {
                            val created = withContext(Dispatchers.IO) {
                                repository.create("Novo script")
                            }
                            selectedScriptId = created.id
                            feedback = ""
                            errorMessage = ""
                        } catch (error: Exception) {
                            errorMessage = error.message ?: "Não foi possível criar o script."
                        }
                    }
                },
                onImport = { importLauncher.launch(arrayOf("text/x-python", "text/plain")) },
                onOpen = { selectedScriptId = it },
                onDuplicate = { script ->
                    scope.launch {
                        try {
                            val copy = withContext(Dispatchers.IO) { repository.duplicate(script.id) }
                            feedback = "Script duplicado."
                            selectedScriptId = copy.id
                        } catch (error: Exception) {
                            errorMessage = error.message ?: "Não foi possível duplicar o script."
                        }
                    }
                },
                onExport = { script ->
                    pendingExport = script.sourceCode
                    exportName = safePythonFilename(script.name)
                    exportLauncher.launch(exportName)
                },
                onDelete = { pendingDelete = it }
            )
        } else {
            ScriptEditorScreen(
                script = selectedScript,
                repository = repository,
                executionRepository = executionRepository,
                executor = executor,
                onBack = {
                    selectedScriptId = null
                    feedback = ""
                    errorMessage = ""
                },
                onExport = {
                    pendingExport = it.sourceCode
                    exportName = safePythonFilename(it.name)
                    exportLauncher.launch(exportName)
                },
                feedback = feedback,
                errorMessage = errorMessage,
                onFeedback = { feedback = it; errorMessage = "" },
                onError = { errorMessage = it; feedback = "" }
            )
        }
    }

    pendingDelete?.let { script ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir script?") },
            text = {
                Text("“${script.name}” será removido da biblioteca e suas versões serão apagadas. O histórico de execuções será preservado.")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { repository.delete(script.id) }
                            feedback = "Script excluído."
                            errorMessage = ""
                        } catch (error: Exception) {
                            errorMessage = error.message ?: "Não foi possível excluir o script."
                        } finally {
                            pendingDelete = null
                        }
                    }
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun LibraryScreen(
    scripts: List<ScriptEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    feedback: String,
    errorMessage: String,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    onOpen: (String) -> Unit,
    onDuplicate: (ScriptEntity) -> Unit,
    onExport: (ScriptEntity) -> Unit,
    onDelete: (ScriptEntity) -> Unit
) {
    val filtered = scripts.filter {
        searchQuery.isBlank() ||
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.tagsJson.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Androide PY", style = MaterialTheme.typography.headlineLarge)
        Text("Biblioteca de scripts", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreate, modifier = Modifier.weight(1f)) { Text("Novo script") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Importar .py") }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Pesquisar por nome, descrição ou tag") },
            singleLine = true
        )
        if (feedback.isNotBlank()) Text(feedback, color = MaterialTheme.colorScheme.primary)
        if (errorMessage.isNotBlank()) Text(errorMessage, color = MaterialTheme.colorScheme.error)
        if (filtered.isEmpty()) {
            Text(
                if (scripts.isEmpty()) "Sua biblioteca está vazia. Crie ou importe um script."
                else "Nenhum script corresponde à pesquisa.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { script ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text(script.name, style = MaterialTheme.typography.titleMedium)
                            if (script.description.isNotBlank()) {
                                Text(script.description, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Atualizado: ${script.updatedAtEpochMs}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { onOpen(script.id) }) { Text("Abrir") }
                                TextButton(onClick = { onDuplicate(script) }) { Text("Duplicar") }
                                TextButton(onClick = { onExport(script) }) { Text("Exportar") }
                                TextButton(onClick = { onDelete(script) }) { Text("Excluir") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptEditorScreen(
    script: ScriptEntity,
    repository: com.samuel.androidepy.data.ScriptRepository,
    executionRepository: com.samuel.androidepy.data.ScriptExecutionRepository,
    executor: com.samuel.androidepy.execution.ScriptExecutor,
    onBack: () -> Unit,
    onExport: (ScriptEntity) -> Unit,
    feedback: String,
    errorMessage: String,
    onFeedback: (String) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember(script.id) { mutableStateOf(script.name) }
    var description by remember(script.id) { mutableStateOf(script.description) }
    var tagsText by remember(script.id) {
        mutableStateOf(parseStringArray(script.tagsJson).joinToString(", "))
    }
    var parametersJson by remember(script.id) { mutableStateOf(script.parametersJson) }
    var dependenciesText by remember(script.id) {
        mutableStateOf(parseStringArray(script.dependenciesJson).joinToString("\n"))
    }
    var parameterValuesJson by remember(script.id) { mutableStateOf("{}") }
    var sourceCode by remember(script.id) { mutableStateOf(script.sourceCode) }
    var executionStatus by remember { mutableStateOf("Pronto") }
    var activeExecutionId by remember { mutableStateOf<String?>(null) }
    var consoleStdout by remember { mutableStateOf("") }
    var consoleStderr by remember { mutableStateOf("") }
    var consoleError by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val history by executionRepository.observeForScript(script.id).collectAsState(initial = emptyList())
    val versions by repository.observeVersions(script.id).collectAsState(initial = emptyList())

    LaunchedEffect(script) {
        name = script.name
        description = script.description
        tagsText = parseStringArray(script.tagsJson).joinToString(", ")
        parametersJson = script.parametersJson
        dependenciesText = parseStringArray(script.dependenciesJson).joinToString("\n")
        sourceCode = script.sourceCode
    }

    LaunchedEffect(executor, script.id) {
        executor.updates.collect { update ->
            if (update.executionId != activeExecutionId) return@collect
            executionStatus = update.status.name
            update.result?.let { result ->
                consoleStdout = result.stdout
                consoleStderr = result.stderr
                consoleError = result.error?.let { "${it.type}: ${it.message}\n${it.traceback}" }.orEmpty()
            }
        }
    }

    fun tagsAsJson(): String = JSONArray(
        tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    ).toString()

    fun dependenciesAsJson(): String = JSONArray(
        dependenciesText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    ).toString()

    suspend fun saveCurrent(): ScriptEntity {
        val currentName = name
        val currentSource = sourceCode
        val currentDescription = description
        val currentTags = tagsAsJson()
        val currentParameters = parametersJson
        val currentDependencies = dependenciesAsJson()
        return withContext(Dispatchers.IO) {
            repository.save(
                scriptId = script.id,
                name = currentName,
                sourceCode = currentSource,
                description = currentDescription,
                tagsJson = currentTags,
                parametersJson = currentParameters,
                dependenciesJson = currentDependencies
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                scope.launch {
                    try {
                        saveCurrent()
                        onBack()
                    } catch (error: Exception) {
                        onError(error.message ?: "Não foi possível salvar antes de sair.")
                    }
                }
            }) { Text("‹ Biblioteca") }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onExport(script.copy(name = name, sourceCode = sourceCode)) }) {
                Text("Exportar .py")
            }
        }
        Text("Editor Python", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Nome") }, singleLine = true
        )
        OutlinedTextField(
            value = description, onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Descrição") }, minLines = 2
        )
        OutlinedTextField(
            value = tagsText, onValueChange = { tagsText = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Tags (separadas por vírgula)") },
            singleLine = true
        )
        Text("Parâmetros — JSON", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = parametersJson, onValueChange = { parametersJson = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Definições de parâmetros") },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            minLines = 3
        )
        Text("Use uma lista de objetos: [{\"name\":\"cidade\",\"type\":\"string\",\"required\":true}]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Dependências — uma declaração por linha", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = dependenciesText, onValueChange = { dependenciesText = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Ex.: requests>=2.31") },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            minLines = 2
        )
        Text(
            "As declarações são verificadas localmente. Pacotes adicionais precisam ser compatíveis com Chaquopy/Python 3.13 e incluídos no build; não há instalação PyPI automática no aparelho.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Código", style = MaterialTheme.typography.titleMedium)
        PythonCodeEditor(
            sourceCode = sourceCode,
            onSourceCodeChange = { sourceCode = it }
        )
        Text("Valores para esta execução — JSON", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = parameterValuesJson, onValueChange = { parameterValuesJson = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Ex.: {\"cidade\":\"Recife\"}") },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            minLines = 2
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            saveCurrent()
                            onFeedback("Script salvo. Uma versão é criada quando o código ou os metadados mudam.")
                        } catch (error: Exception) {
                            onError(error.message ?: "Falha ao salvar.")
                        } finally { busy = false }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !busy
            ) { Text("Salvar") }
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val saved = saveCurrent()
                            val resolvedParameters = ScriptParameterResolver.resolve(
                                saved.parametersJson, parameterValuesJson
                            )
                            val latestVersion = withContext(Dispatchers.IO) {
                                repository.getVersions(saved.id).firstOrNull()?.versionNumber
                            }
                            val request = ScriptExecutionRequest(
                                sourceCode = saved.sourceCode,
                                scriptId = saved.id,
                                parametersJson = resolvedParameters
                            )
                            withContext(Dispatchers.IO) {
                                executionRepository.recordQueued(
                                    saved, request.executionId, latestVersion,
                                    resolvedParameters, request.queuedAtEpochMs
                                )
                            }
                            activeExecutionId = request.executionId
                            executionStatus = "QUEUED"
                            consoleStdout = ""
                            consoleStderr = ""
                            consoleError = ""
                            executor.submit(request)
                            onFeedback("Execução enviada para a fila.")
                        } catch (error: Exception) {
                            onError(error.message ?: "Não foi possível executar o script.")
                        } finally { busy = false }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !busy
            ) { Text("Executar") }
        }
        Text("Histórico de versões", style = MaterialTheme.typography.titleMedium)
        if (versions.isEmpty()) {
            Text("Ainda não há versões salvas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            versions.take(10).forEach { version ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Versão ${version.versionNumber}", style = MaterialTheme.typography.labelLarge)
                            Text("Salva em: ${version.createdAtEpochMs}", style = MaterialTheme.typography.bodySmall)
                            Text(version.sourceCode.lineSequence().take(2).joinToString(" ").take(100),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        repository.restoreVersion(script.id, version.versionNumber)
                                    }
                                    onFeedback("Versão ${version.versionNumber} restaurada como uma nova versão.")
                                } catch (error: Exception) {
                                    onError(error.message ?: "Não foi possível restaurar a versão.")
                                }
                            }
                        }) { Text("Restaurar") }
                    }
                }
            }
        }
        Text("Console — $executionStatus", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("stdout", style = MaterialTheme.typography.labelLarge)
                Text(consoleStdout.ifBlank { "(sem saída)" }, fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall)
                Text("stderr", style = MaterialTheme.typography.labelLarge)
                Text(consoleStderr.ifBlank { "(vazio)" }, fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall)
                if (consoleError.isNotBlank()) {
                    Text("Exceção", color = MaterialTheme.colorScheme.error)
                    Text(consoleError, color = MaterialTheme.colorScheme.error,
                        fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Text("Histórico de execuções", style = MaterialTheme.typography.titleMedium)
        if (history.isEmpty()) {
            Text("Ainda não há execuções registradas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            history.take(20).forEach { execution ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${execution.status} • versão ${execution.scriptVersionNumber ?: "—"}",
                            style = MaterialTheme.typography.labelLarge)
                        Text("ID: ${execution.executionId}", style = MaterialTheme.typography.labelSmall)
                        Text("Início: ${execution.startedAtEpochMs ?: execution.queuedAtEpochMs}",
                            style = MaterialTheme.typography.bodySmall)
                        if (execution.stdout.isNotBlank()) {
                            Text(execution.stdout.take(500), fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (execution.stderr.isNotBlank()) {
                            Text(execution.stderr.take(500), color = MaterialTheme.colorScheme.error,
                                fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                        if (execution.errorJson != null) {
                            Text(execution.errorJson.take(500), color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (feedback.isNotBlank()) Text(feedback, color = MaterialTheme.colorScheme.primary)
        if (errorMessage.isNotBlank()) Text(errorMessage, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private fun parseStringArray(json: String): List<String> = try {
    val array = JSONArray(json)
    (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
} catch (_: Exception) {
    emptyList()
}

private fun safePythonFilename(name: String): String {
    val safe = name.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_', '.')
    val base = safe.removeSuffix(".py").ifBlank { "script" }
    return "$base.py".take(100)
}

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}

private fun readUtf8Script(context: android.content.Context, uri: Uri): String {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_SCRIPT_BYTES) { "O arquivo excede o limite de 1 MiB." }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: error("Não foi possível abrir o arquivo selecionado.")

    val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    return decoder.decode(ByteBuffer.wrap(bytes)).toString()
}
