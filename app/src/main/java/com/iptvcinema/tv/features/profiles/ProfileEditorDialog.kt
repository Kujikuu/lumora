package com.iptvcinema.tv.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaConfirmDialog
import com.iptvcinema.tv.core.design.components.CinemaTextField
import com.iptvcinema.tv.core.design.components.FilterChipRow
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.ProfileType

private val EditableTypes = listOf(ProfileType.MAIN, ProfileType.FAMILY, ProfileType.KIDS, ProfileType.GUEST)

data class ProfileEditorActions(
    val onNameChange: (String) -> Unit,
    val onTypeChange: (ProfileType) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onDismiss: () -> Unit,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileEditorDialog(state: ProfileEditorState, actions: ProfileEditorActions) {
    var confirmDelete by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { nameFocus.requestFocus() } }

    if (confirmDelete) {
        CinemaConfirmDialog(
            title = stringResource(R.string.profile_delete_confirm_title, state.name.trim()),
            message = stringResource(R.string.profile_delete_confirm_body),
            confirmLabel = stringResource(R.string.profile_delete),
            cancelLabel = stringResource(R.string.btn_cancel),
            destructive = true,
            isWorking = state.isSaving,
            onConfirm = {
                confirmDelete = false
                actions.onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
        return
    }

    Dialog(
        onDismissRequest = actions.onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .background(CinemaColors.Surface, CinemaShapes.Large)
                .padding(CinemaSpacing.SectionGap),
            verticalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
        ) {
            Text(
                text = stringResource(if (state.editingId == null) R.string.profile_add else R.string.profile_edit),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
            )
            CinemaTextField(
                value = state.name,
                onValueChange = actions.onNameChange,
                label = stringResource(R.string.profile_name_label),
                modifier = Modifier.focusRequester(nameFocus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { actions.onSave() }),
            )
            state.nameError?.let { error ->
                Text(
                    text = stringResource(nameErrorText(error)),
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.Danger),
                )
            }
            Text(
                text = stringResource(R.string.profile_type_label),
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
            )
            val selectedTypeIndex = EditableTypes.indexOf(state.type).coerceAtLeast(0)
            FilterChipRow(
                items = EditableTypes.map { stringResource(typeLabel(it)) },
                selectedIndex = selectedTypeIndex,
                onSelected = { index -> actions.onTypeChange(EditableTypes[index]) },
                focusedChipIndex = selectedTypeIndex,
            )
            if (state.saveFailed) {
                Text(
                    text = stringResource(R.string.profile_save_failed),
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.Danger),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                CinemaButton(
                    text = stringResource(R.string.profile_save),
                    variant = CinemaButtonVariant.PrimaryAccent,
                    onClick = actions.onSave,
                    enabled = !state.isSaving,
                )
                CinemaButton(
                    text = stringResource(R.string.btn_cancel),
                    variant = CinemaButtonVariant.SecondaryDark,
                    onClick = actions.onDismiss,
                    enabled = !state.isSaving,
                )
                if (state.editingId != null && state.canDelete) {
                    CinemaButton(
                        text = stringResource(R.string.profile_delete),
                        variant = CinemaButtonVariant.Danger,
                        onClick = { confirmDelete = true },
                        enabled = !state.isSaving,
                        modifier = Modifier.padding(start = CinemaSpacing.ButtonGap),
                    )
                }
            }
        }
    }
}

private fun nameErrorText(error: ProfileNameError): Int = when (error) {
    ProfileNameError.Empty -> R.string.profile_error_empty
    ProfileNameError.TooLong -> R.string.profile_error_too_long
    ProfileNameError.Duplicate -> R.string.profile_error_duplicate
}

private fun typeLabel(type: ProfileType): Int = when (type) {
    ProfileType.MAIN -> R.string.profile_type_main
    ProfileType.FAMILY -> R.string.profile_type_family
    ProfileType.KIDS -> R.string.profile_type_kids
    ProfileType.GUEST -> R.string.profile_type_guest
}
