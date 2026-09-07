package pl.foodhub.pos.update

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

/**
 * Surfaces [AppUpdateUiState] as a dismissible dialog (offer -> download progress),
 * then hands the downloaded APK to the system installer via [FileProvider]. Placed
 * once, app-wide, over [pl.foodhub.pos.navigation.FoodHubNavHost] in
 * [pl.foodhub.pos.MainActivity] rather than blocking any particular screen -- a POS
 * terminal mid-sale should never be interrupted by this.
 */
@Composable
fun AppUpdateHost(
    state: AppUpdateUiState,
    onInstallRequested: () -> Unit,
    onDismiss: () -> Unit,
    onInstallLaunched: () -> Unit,
) {
    val context = LocalContext.current

    when (state) {
        is AppUpdateUiState.Available -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Dostępna aktualizacja") },
                text = { Text("Nowa wersja aplikacji kasy jest gotowa do zainstalowania.") },
                confirmButton = {
                    TextButton(onClick = onInstallRequested) { Text("Zainstaluj aktualizację") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Później") }
                },
            )
        }
        AppUpdateUiState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = { Text("Pobieranie aktualizacji") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Proszę czekać...")
                    }
                },
            )
        }
        is AppUpdateUiState.ReadyToInstall -> {
            LaunchedEffect(state.apkFile) {
                val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", state.apkFile)
                val installIntent =
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, APK_MIME_TYPE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(installIntent)
                onInstallLaunched()
            }
        }
        AppUpdateUiState.Idle -> Unit
    }
}
