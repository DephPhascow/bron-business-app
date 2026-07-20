package com.dphascow.app.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import org.jetbrains.compose.resources.stringResource
import ui.theme.T
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Picks a date and then a time, and reports the result as the ISO string the
 * `DateTime` scalar expects ("2026-07-20T14:00:00").
 *
 * [value] is that same ISO string, or blank when nothing is chosen yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeField(
    label: String,
    value: String,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    var showDate by remember { mutableStateOf(false) }
    // Holds the day chosen in the first dialog while the second one asks for the time.
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    val parsed = remember(value) { value.toLocalDateTime() }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(T.d.xs)) {
        Text(label, color = T.c.dark7, style = T.t.t4SamiBold)
        OutlinedButton(
            onClick = { showDate = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(parsed?.display() ?: stringResource(Res.string.datetime_choose))
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            // The picker works in UTC-midnight millis, which is exactly one civil day.
            initialSelectedDateMillis = (parsed?.date ?: today()).toEpochMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        showDate = false
                        if (millis != null) pendingDate = millis.toLocalDate()
                    },
                ) { Text(stringResource(Res.string.common_next)) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text(stringResource(Res.string.common_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    pendingDate?.let { date ->
        val state = rememberTimePickerState(
            initialHour = parsed?.hour ?: DEFAULT_HOUR,
            initialMinute = parsed?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pendingDate = null },
            title = { Text(stringResource(Res.string.datetime_time_title)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(LocalDateTime(date, state.hour, state.minute).toIso())
                        pendingDate = null
                    },
                ) { Text(stringResource(Res.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDate = null }) { Text(stringResource(Res.string.common_cancel)) }
            },
        )
    }
}

/** Sensible default when opening the time picker on a fresh field. */
private const val DEFAULT_HOUR = 12

private data class LocalDate(val year: Int, val month: Int, val day: Int)

private data class LocalDateTime(val date: LocalDate, val hour: Int, val minute: Int)

private fun LocalDateTime.toIso(): String =
    "${date.year.pad(4)}-${date.month.pad(2)}-${date.day.pad(2)}T${hour.pad(2)}:${minute.pad(2)}:00"

private fun LocalDateTime.display(): String =
    "${date.day.pad(2)}.${date.month.pad(2)}.${date.year} ${hour.pad(2)}:${minute.pad(2)}"

private fun Int.pad(width: Int): String = toString().padStart(width, '0')

/** Parses the ISO form we emit; anything else is treated as "nothing chosen". */
private fun String.toLocalDateTime(): LocalDateTime? {
    val date = substringBefore('T').split('-')
    val time = substringAfter('T', "").split(':')
    if (date.size != 3) return null
    val year = date[0].toIntOrNull() ?: return null
    val month = date[1].toIntOrNull() ?: return null
    val day = date[2].toIntOrNull() ?: return null
    return LocalDateTime(
        date = LocalDate(year, month, day),
        hour = time.getOrNull(0)?.toIntOrNull() ?: 0,
        minute = time.getOrNull(1)?.toIntOrNull() ?: 0,
    )
}

@OptIn(ExperimentalTime::class)
private fun today(): LocalDate = Clock.System.now().toEpochMilliseconds().toLocalDate()

private const val MILLIS_PER_DAY = 86_400_000L

private fun LocalDate.toEpochMillis(): Long = daysFromCivil(year, month, day) * MILLIS_PER_DAY

private fun Long.toLocalDate(): LocalDate = civilFromDays(floorDiv(MILLIS_PER_DAY))

// Howard Hinnant's civil-calendar conversions. The date picker speaks UTC-midnight
// millis, so plain day arithmetic round-trips the chosen day without a timezone.
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = (month + 9) % 12
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

private fun civilFromDays(days: Long): LocalDate {
    val z = days + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    return LocalDate(
        year = (if (month <= 2) y + 1 else y).toInt(),
        month = month.toInt(),
        day = day.toInt(),
    )
}
