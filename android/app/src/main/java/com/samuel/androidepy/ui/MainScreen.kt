package com.samuel.androidepy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.samuel.androidepy.AndroidePyApplication
import com.samuel.androidepy.data.ScriptRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(application: AndroidePyApplication) {
    val repository = application.scriptRepository
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
    var storageError by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }

    LaunchedEffect(repository) {
        if (!isLoaded) {
            isLoading = true
            storageError = ""

            try {
                val script = withContext(Dispatchers.IO) {
                    repository.getOrCreateDefault()
                }
                scriptId = script.id
                scriptName = script.name
                sourceCode = script.sourceCode
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                storageError = error.message ?: "Não foi possível carregar o script."
            } finally {
                isLoaded = true
                isLoading = false
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Androide PY",
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
                enabled = !isLoading
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
                enabled = !isLoading
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = saveScript,
                    enabled = !isLoading && !isSaving
                ) {
                    Text(if (isSaving) "Salvando..." else "Salvar")
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
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Console",
                style = MaterialTheme.typography.titleMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "A saída da execução aparecerá aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
