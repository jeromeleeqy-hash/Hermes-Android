package com.qingyu.hermescompanion.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.ui.theme.HermesSpacing

@Composable
internal fun AppNotice(message: String, isError: Boolean) {
    Snackbar(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = HermesSpacing.floatingInset, vertical = 8.dp).testTag("app_notice"),
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface,
        contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface,
    ) { Text(message) }
}
