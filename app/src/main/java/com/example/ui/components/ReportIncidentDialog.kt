package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.theme.Spacing

/**
 * What the user actually typed into the report dialog.
 *
 * The previous implementation discarded the form values and submitted hardcoded
 * numbers (₹150 asked / ₹75 expected / 5 km) for every report, which made the
 * community data meaningless. This carries the real input through.
 */
data class IncidentReportDraft(
    val vehicleNumber: String,
    val location: String,
    val askedFare: Double?,
    val description: String
)

/**
 * Report an overcharge or fare refusal.
 *
 * Validation is inline: Submit surfaces the reason it cannot proceed instead of
 * silently accepting an empty report.
 */
@Composable
fun ReportIncidentDialog(
    onDismiss: () -> Unit,
    onSubmit: (IncidentReportDraft) -> Unit
) {
    var vehicleNumber by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var askedFareText by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var showValidation by rememberSaveable { mutableStateOf(false) }

    val trimmedLocation = location.trim()
    val trimmedDescription = description.trim()
    val askedFare = askedFareText.trim().toDoubleOrNull()

    val locationError = showValidation && trimmedLocation.isEmpty()
    val descriptionError = showValidation && trimmedDescription.length < 10
    val canSubmit = trimmedLocation.isNotEmpty() && trimmedDescription.length >= 10

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Report overcharging",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.card)) {
                Text(
                    text = "Reports are anonymous and stored on this device to build a " +
                        "picture of fare problems in your city.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Where did this happen?") },
                    placeholder = { Text("Landmark, stand or junction") },
                    isError = locationError,
                    supportingText = if (locationError) {
                        { Text("Add a location so the report is useful") }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_report_location")
                )

                OutlinedTextField(
                    value = askedFareText,
                    onValueChange = { input ->
                        // Digits only: free text here produced a parse failure
                        // downstream when the report was persisted.
                        askedFareText = input.filter { it.isDigit() }.take(6)
                    },
                    label = { Text("Fare asked (optional)") },
                    prefix = { Text("₹") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_report_fare")
                )

                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it.uppercase().take(16) },
                    label = { Text("Vehicle number (optional)") },
                    placeholder = { Text("UP 32 AB 1234") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_report_vehicle")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(300) },
                    label = { Text("What happened?") },
                    placeholder = { Text("Refused the meter and demanded a flat fare") },
                    isError = descriptionError,
                    supportingText = {
                        if (descriptionError) {
                            Text("Describe the incident in a little more detail")
                        } else {
                            Text("${description.length}/300")
                        }
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_report_description")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSubmit) {
                        onSubmit(
                            IncidentReportDraft(
                                vehicleNumber = vehicleNumber.trim(),
                                location = trimmedLocation,
                                askedFare = askedFare,
                                description = trimmedDescription
                            )
                        )
                    } else {
                        showValidation = true
                    }
                },
                modifier = Modifier.testTag("btn_submit_report")
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

