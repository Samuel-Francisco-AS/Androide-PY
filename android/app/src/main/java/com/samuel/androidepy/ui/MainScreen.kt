package com.samuel.androidepy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.samuel.androidepy.AndroidePyApplication
import com.samuel.androidepy.data.ScriptRepository
import com.samuel.androidepy.execution.ExecutionStatus
import com.samuel.androidepy.execution.ScriptExecutionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(application: AndroidePyApplication) {
    val repository = application.scriptRepository
    val executor = application.scriptExecutor
    val scope = rememberCoroutineScope()

    var scriptId by rememberSaveable {
        mutableStateOf(ScriptRepository.DEFAULT_SCRIPT_ID)
    }
    var scriptName by rememberSaveable {
        mutableStateOf("Meu primeiro script")
    }
    var sourceCode by rememberSaveable {
        mutableStateOf("print(\"Olá, Androide PY!\")")
    }
    var isLoaded by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(!isLoaded) }
    var isSaving by remember { mutableStateOf(false) }
    var isPreparingExecution by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }
    var activeExecutionId by remember { mutableStateOf<String?>(null) }
    var executionStatus by remember { mutableStateOf("Pronto") }
    var consoleStdout by remember { mutableStateOf("") }
    var consoleStderr by remember { mutableStateOf("") }
    var consoleException by remember { mutableStateOf("") }
    var executionDurationMs by remember { mutableStateOf<Long?>(null) }
    var outputTruncated by remember { mutableStateOf(false) }
    var storageError by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }

    LaunchedEffect(repository) {
        if (!isLoaded) {
            isLoading = true
            storageError = ""
            var loadCompleted = false

            try {
                val script = withContext(Dispatchers.IO) {
                    repository.getOrCreateDefault()
                }
                scriptId = script.id
                scriptName = script.name
                sourceCode = script.sourceCode
                loadCompleted = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                storageError = error.message ?: "Não foi possível carregar o script."
                loadCompleted = true
            } finally {
                if (loadCompleted) isLoaded = true
                isLoading = false
            }
        }
    }

    LaunchedEffect(executor) {
        executor.updates.collect { update ->
            if (activeExecutionId == null) {
                activeExecutionId = update.executionId
            }
            if (update.executionId != activeExecutionId) return@collect

            when (update.status) {
                ExecutionStatus.QUEUED -> {
                    isExecuting = true
                    executionStatus = "Na fila"
                }

                ExecutionStatus.RUNNING -> {
                    isExecuting = true
                    executionStatus = "Executando Python..."
                }

                ExecutionStatus.SUCCEEDED -> {
                    isExecuting = false
                    executionStatus = "Concluído"
                    update.result?.let { result ->
                        consoleStdout = result.stdout
                        consoleStderr = result.stderr
                        consoleException = ""
                        executionDurationMs = result.durationMs
                        outputTruncated = result.stdoutTruncated || result.stderrTruncated
                    }
                }

                ExecutionStatus.FAILED -> {
                    isExecuting = false
                    executionStatus = "Falha na execução"
                    update.result?.let { result ->
                        consoleStdout = result.stdout
                        consoleStderr = result.stderr
                        consoleException = result.error?.let { error ->
                            buildString {
                                append(error.type)
                                append(": ")
                                append(error.message)
                                if (error.traceback.isNotBlank()) {
                                    append("\n")
                                    append(error.traceback)
                                }
                            }
                        }.orEmpty()
                        executionDurationMs = result.durationMs
                        outputTruncated = result.stdoutTruncated || result.stderrTruncated
                    }
                }

                ExecutionStatus.CANCELLED -> {
                    isExecuting = false
                    executionStatus = "Execução cancelada"
                }
            }
        }
    }

    val saveScript: () -> Unit = {
        scope.launch {
            isSaving = true
            storageError = ""
            saveMessage = ""

            try {
                val saved = withContext(Dispatchers.IO) {
                    repository.save(scriptId, scriptName, sourceCode)
                }
                scriptName = saved.name
                saveMessage = "Script salvo localmente."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                storageError = error.message ?: "Não foi possível salvar o script."
            } finally {
                isSaving = false
            }
        }
    }

    val executeScript: () -> Unit = {
        scope.launch {
            isPreparingExecution = true
            storageError = ""
            saveMessage = ""
            consoleStdout = ""
            consoleStderr = ""
            consoleException = ""
            executionDurationMs = null
            outputTruncated = false
            executionStatus = "Salvando antes da execução..."

            try {
                val saved = withContext(Dispatchers.IO) {
                    repository.save(scriptId, scriptName, sourceCode)
                }
                scriptName = saved.name

                val request = ScriptExecutionRequest(
                    sourceCode = saved.sourceCode,
                    scriptId = saved.id
                )
                activeExecutionId = request.executionId
                isExecuting = true
                executionStatus = "Na fila"
                executor.submit(request)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                isExecuting = false
                executionStatus = "Não foi possível iniciar"
                storageError = error.message ?: "Falha ao preparar a execução."
            } finally {
                isPreparingExecution = false
            }
        }
    }

    val editorEnabled = !isLoading && !isSaving && !isPreparingExecution && !isExecuting

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Androide PY",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Central local de automações Python",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = scriptName,
                onValueChange = {
                    scriptName = it
                    saveMessage = ""
                    storageError = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome do script") },
                singleLine = true,
                enabled = editorEnabled
            )

            OutlinedTextField(
                value = sourceCode,
                onValueChange = {
                    sourceCode = it
                    saveMessage = ""
                    storageError = ""
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Código Python") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                minLines = 8,
                maxLines = 16,
                enabled = editorEnabled
            )

            Text(
                text = "Exemplos da Android Bridge",
                style = MaterialTheme.typography.titleSmall
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        sourceCode = "import mobile.device\n\nprint(mobile.device.get_info())"
                        saveMessage = ""
                        storageError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editorEnabled
                ) {
                    Text("Inserir exemplo: informações do aparelho")
                }

                OutlinedButton(
                    onClick = {
                        sourceCode = "import mobile.apps\n\n# Substitua pelo pacote de um app instalado.\nresult = mobile.apps.launch(\"com.example.app\")\nprint(result)"
                        saveMessage = ""
                        storageError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editorEnabled
                ) {
                    Text("Inserir exemplo: abrir aplicativo")
                }
            }

            Text(
                text = "Os exemplos preenchem o editor; toque em Executar para testá-los.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = saveScript,
                    modifier = Modifier.weight(1f),
                    enabled = editorEnabled
                ) {
                    Text(if (isSaving) "Salvando..." else "Salvar")
                }

                Button(
                    onClick = executeScript,
                    modifier = Modifier.weight(1f),
                    enabled = editorEnabled
                ) {
                    Text(if (isPreparingExecution || isExecuting) "Executando..." else "Executar")
                }
            }

            if (isLoading) {
                Text(
                    text = "Carregando script salvo...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (saveMessage.isNotBlank()) {
                Text(
                    text = saveMessage,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (storageError.isNotBlank()) {
                Text(
                    text = storageError,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Console — $executionStatus",
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
                style = MaterialTheme.typography.titleMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "stdout",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = consoleStdout.ifBlank { "(sem saída)" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "stderr",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = consoleStderr.ifBlank { "(vazio)" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (consoleStderr.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )

                    if (consoleException.isNotBlank()) {
                        Text(
                            text = "Exceção Python",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = consoleException,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    executionDurationMs?.let { duration ->
                        Text(
                            text = "Duração: $duration ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (outputTruncated) {
                        Text(
                            text = "A saída atingiu o limite de captura.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
