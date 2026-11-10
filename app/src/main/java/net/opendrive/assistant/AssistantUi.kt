package net.opendrive.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.prototux.aare.R

/** Phone-only setup: no credential entry or long text input on the car display. */
@Composable
fun OpenDriveAssistantSettings() {
    val context = LocalContext.current
    val settings = remember { AssistantSettings(context) }
    var enabled by remember { mutableStateOf(settings.enabled) }
    var hasKey by remember { mutableStateOf(settings.hasKey) }
    var key by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(settings.model) }
    var preferences by remember { mutableStateOf(settings.preferences) }
    var speak by remember { mutableStateOf(settings.speakReplies) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.od_assistant_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.od_assistant_description), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.od_privacy), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.od_enable), Modifier.weight(1f))
            Switch(checked = enabled, enabled = hasKey, onCheckedChange = { enabled = it; settings.enabled = it })
        }
        Text(stringResource(if (hasKey) R.string.od_key_saved else R.string.od_key_missing), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = key, onValueChange = { key = it.take(1024) }, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.od_api_key)) }, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
        OutlinedTextField(value = model, onValueChange = { model = it.take(100) }, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.od_model)) }, singleLine = true)
        OutlinedTextField(value = preferences, onValueChange = { preferences = it.take(1000) }, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.od_preferences)) }, minLines = 2, maxLines = 4)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.od_speak_replies), Modifier.weight(1f))
            Switch(checked = speak, onCheckedChange = { speak = it; settings.speakReplies = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                try {
                    if (key.isNotBlank()) settings.saveKey(key)
                    settings.model = model
                    settings.preferences = preferences
                    key = ""
                    hasKey = settings.hasKey
                    message = context.getString(R.string.od_saved)
                } catch (_: Exception) { message = context.getString(R.string.od_save_failed) }
            }) { Text(stringResource(R.string.od_save)) }
            TextButton(enabled = hasKey, onClick = {
                try {
                    settings.forgetKey(); key = ""; hasKey = false; enabled = false
                    message = context.getString(R.string.od_removed)
                } catch (_: Exception) { message = context.getString(R.string.od_save_failed) }
            }) { Text(stringResource(R.string.od_remove_key)) }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text(stringResource(R.string.od_setup_help), style = MaterialTheme.typography.bodySmall)
    }
}

/** Compact speech UI over navigation/media; follow-ups use the same in-memory conversation. */
@Composable
fun OpenDriveAssistantPanel(assistant: OpenDriveAssistant, modifier: Modifier = Modifier) {
    if (!assistant.visible) return
    Surface(modifier.padding(12.dp).widthIn(max = 760.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), tonalElevation = 8.dp, shadowElevation = 8.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.od_assistant_title), style = MaterialTheme.typography.titleMedium)
            if (assistant.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            val status = when {
                assistant.listening -> stringResource(R.string.od_listening)
                assistant.busy -> stringResource(R.string.od_thinking)
                else -> stringResource(R.string.od_follow_up)
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            if (assistant.question.isNotBlank()) Text(assistant.question, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall)
            assistant.error?.let { Text(it, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.error) }
            if (assistant.answer.isNotBlank()) Text(assistant.answer, maxLines = 3, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = assistant::activate) {
                    Text(stringResource(if (assistant.listening) R.string.od_send else if (assistant.busy) R.string.od_interrupt else R.string.od_talk))
                }
                TextButton(onClick = assistant::forget) { Text(stringResource(R.string.od_new_conversation)) }
                TextButton(onClick = assistant::close) { Text(stringResource(R.string.od_close)) }
            }
        }
    }
}
