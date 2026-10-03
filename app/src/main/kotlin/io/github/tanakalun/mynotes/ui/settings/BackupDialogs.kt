package io.github.tanakalun.mynotes.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.tanakalun.mynotes.R
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ExportPasswordDialog(
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.backup_password_export_title),
        summary = stringResource(R.string.backup_password_export_summary),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = password,
                onValueChange = { password = it; mismatch = false },
                label = stringResource(R.string.backup_password_label),
                useLabelAsPlaceholder = true,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = confirm,
                onValueChange = { confirm = it; mismatch = false },
                label = stringResource(R.string.backup_password_confirm_label),
                useLabelAsPlaceholder = true,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (mismatch) {
                Text(
                    text = stringResource(R.string.backup_password_mismatch),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.done),
                    onClick = {
                        if (password == confirm && password.isNotBlank()) {
                            onConfirm(password.toCharArray())
                        } else {
                            mismatch = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = top.yukonga.miuix.kmp.basic.TextButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        disabledColor = MiuixTheme.colorScheme.primaryContainer,
                        textColor = MiuixTheme.colorScheme.onPrimary,
                        disabledTextColor = MiuixTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun ImportPasswordDialog(
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    OverlayDialog(
        show = true,
        title = stringResource(R.string.backup_password_import_title),
        summary = stringResource(R.string.import_data_summary),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.backup_password_label),
                useLabelAsPlaceholder = true,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.done),
                    onClick = { onConfirm(password.toCharArray()) },
                    modifier = Modifier.weight(1f),
                    colors = top.yukonga.miuix.kmp.basic.TextButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        disabledColor = MiuixTheme.colorScheme.primaryContainer,
                        textColor = MiuixTheme.colorScheme.onPrimary,
                        disabledTextColor = MiuixTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun ImportConfirmDialog(
    count: Int,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        show = true,
        title = stringResource(R.string.import_dialog_title),
        summary = stringResource(R.string.import_dialog_found, count),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(R.string.import_notes_settings),
                onClick = { onConfirm(true) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                text = stringResource(R.string.import_notes_only),
                onClick = { onConfirm(false) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun BusyDialog(message: String) {
    OverlayDialog(
        show = true,
        enableWindowDim = true,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfiniteProgressIndicator(
                    color = MiuixTheme.colorScheme.primary,
                    size = 36.dp,
                )
                Text(
                    text = message,
                    style = MiuixTheme.textStyles.body1,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}