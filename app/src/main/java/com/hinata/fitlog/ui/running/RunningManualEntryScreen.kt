package com.hinata.fitlog.ui.running

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hinata.fitlog.domain.formatPace
import com.hinata.fitlog.ui.common.DatePickerField
import com.hinata.fitlog.ui.common.DateUtil
import kotlinx.coroutines.launch

/**
 * 手入力での記録の追加。過去分の後入力や、GPSが使えない環境（トレッドミル等）向けの入り口。
 * メイン画面には常時出さず、リンクから開く。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningManualEntryScreen(
    onBack: () -> Unit,
    onSave: (date: String, distText: String, minText: String) -> Boolean,
) {
    var date by remember { mutableStateOf(DateUtil.today()) }
    var dist by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }

    val pace = formatPace(dist.trim().toDoubleOrNull(), minutes.trim().toDoubleOrNull())

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手入力で記録を追加") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp),
        ) {
            DatePickerField(value = date, onValueChange = { date = it })

            OutlinedTextField(
                value = dist,
                onValueChange = { dist = it },
                label = { Text("距離 (km) ※必須") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it },
                label = { Text("時間 (分) ※任意") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text(
                text = if (pace != null) "ペース: $pace /km" else "ペース: 距離と時間を入力すると表示されます",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            Button(
                onClick = {
                    val ok = onSave(date, dist, minutes)
                    scope.launch {
                        if (ok) {
                            dist = ""
                            minutes = ""
                            snackbarHostState.showSnackbar("保存しました")
                        } else {
                            snackbarHostState.showSnackbar("距離を入力し、時間は数値で入力してください")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text("保存")
            }
        }
    }
}
