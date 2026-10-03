package dev.jotalac.feature.notebooks_management.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubmittableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    submit: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: StringResource? = null,
    visualTransformation: VisualTransformation? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        placeholder = { placeholder?.let { Text(stringResource(placeholder)) } },
        singleLine = true,
        visualTransformation = visualTransformation ?: VisualTransformation.None,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = modifier
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    submit()
                    true
                } else {
                    false
                }
            }
    )
}
