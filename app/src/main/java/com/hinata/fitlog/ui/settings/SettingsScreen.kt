package com.hinata.fitlog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hinata.fitlog.ui.navigation.Destination

/**
 * 設定画面。下部ナビゲーションに表示するタブを選べる。
 * ホームと設定自体は常に表示されるため一覧には出さない。
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val visibleTabs by viewModel.visibleTabs.collectAsState()
    val toggleableTabs = Destination.entries.filter { it.toggleable }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("設定", style = MaterialTheme.typography.titleLarge)

            Text("表示するタブ", style = MaterialTheme.typography.titleMedium)
            Text(
                "下部ナビゲーションに表示するタブを選べます。使わないタブは非表示にでき、" +
                    "後からいつでもここで表示に戻せます。ホームと設定は常に表示されます。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column {
                    toggleableTabs.forEachIndexed { index, destination ->
                        TabVisibilityRow(
                            destination = destination,
                            checked = destination in visibleTabs,
                            onCheckedChange = { checked ->
                                viewModel.setTabVisible(destination, checked)
                            },
                        )
                        if (index != toggleableTabs.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabVisibilityRow(
    destination: Destination,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(destination.icon, contentDescription = null)
            Text(destination.label, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
