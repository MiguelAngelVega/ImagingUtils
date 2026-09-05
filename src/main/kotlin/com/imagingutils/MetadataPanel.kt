package com.imagingutils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MetadataPanel(selected: ImageEntry?) {
    if (selected == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select an image", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    var sections by remember(selected) { mutableStateOf<List<MetaSection>>(emptyList()) }
    var filter by remember(selected) { mutableStateOf("") }
    LaunchedEffect(selected) {
        sections = withContext(Dispatchers.IO) { readMetadata(selected) }
    }
    val query = filter.trim()
    val filtered = if (query.isEmpty()) sections else sections.mapNotNull { section ->
        if (section.title == "File") return@mapNotNull section
        val rows = section.rows.filter {
            it.key.contains(query, ignoreCase = true) ||
                it.value.contains(query, ignoreCase = true) ||
                it.type.contains(query, ignoreCase = true)
        }
        if (rows.isEmpty()) null else section.copy(rows = rows)
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            label = { Text("Filter headers") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            for (section in filtered) {
                Text(section.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                for (row in section.rows) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text(
                            row.key,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.fillMaxWidth(0.38f),
                        )
                        Text(
                            row.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(0.30f),
                        )
                        Text(row.value, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
