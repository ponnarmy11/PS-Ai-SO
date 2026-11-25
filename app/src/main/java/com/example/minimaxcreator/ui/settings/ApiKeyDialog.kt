package com.example.minimaxcreator.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeyDialog(
    onSave: (String, String, String) -> Unit
) {
    var miniMaxValue by remember { mutableStateOf("") }
    var qwenValue by remember { mutableStateOf("") }
    var agnesValue by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Configure API Keys") },
        text = {
            Column {
                OutlinedTextField(
                    value = miniMaxValue,
                    onValueChange = { miniMaxValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("MiniMax API Key (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = qwenValue,
                    onValueChange = { qwenValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Qwen (DashScope) API Key (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = agnesValue,
                    onValueChange = { agnesValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Agnes AI API Key (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(miniMaxValue, qwenValue, agnesValue) },
                enabled = miniMaxValue.isNotBlank() || qwenValue.isNotBlank() || agnesValue.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = {}, enabled = false) {
                Text("Configure at least one API Key")
            }
        }
    )
}
