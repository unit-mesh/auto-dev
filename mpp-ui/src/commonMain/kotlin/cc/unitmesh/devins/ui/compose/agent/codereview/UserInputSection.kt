package cc.unitmesh.devins.ui.compose.agent.codereview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.unitmesh.devins.ui.compose.icons.AutoDevComposeIcons

@Composable
fun UserInputSection(
    onGenerate: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Provide instructions for fix generation (optional):",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("E.g., 'Focus on performance', 'Use specific library'") },
                minLines = 3
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = { onGenerate(text) }) {
                    Icon(AutoDevComposeIcons.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate Fixes")
                }
            }
        }
    }
}
