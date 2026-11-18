package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R

@Composable
fun DuelDisconnectDialog(
    onReconnect: () -> Unit,
    onReturnHome: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.duel_disconnect_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.duel_disconnect_dialog_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onReconnect,
                modifier = Modifier.testTag("disconnect_reconnect_button")
            ) {
                Text(text = stringResource(R.string.duel_disconnect_reconnect))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReturnHome,
                modifier = Modifier.testTag("disconnect_home_button")
            ) {
                Text(text = stringResource(R.string.duel_disconnect_home))
            }
        }
    )
}
