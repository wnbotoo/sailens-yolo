package com.sailens.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sailens.presentation.R
import com.sailens.ux.component.SettingRow
import com.sailens.ux.component.SettingsSection

@Composable
internal fun SettingsDiagnosticsSection(
    onOpenTraceReports: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_diagnostics),
        modifier = modifier,
    ) {
        SettingRow(
            title = stringResource(R.string.btn_open_trace_reports),
            supportingText = stringResource(R.string.settings_diagnostics_trace_reports_supporting),
            onClick = onOpenTraceReports,
        )
    }
}
