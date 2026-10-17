package com.jash.salescashtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jash.salescashtracker.ui.theme.AppColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** iOS-style inset grouped section: optional header, white rounded card, optional footer. */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Secondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 8.dp),
            )
        }
        Surface(shape = RoundedCornerShape(14.dp), color = AppColors.Card, modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Secondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            )
        }
    }
}

@Composable
fun RowDivider(startIndent: Int = 16) {
    HorizontalDivider(modifier = Modifier.padding(start = startIndent.dp), thickness = 0.5.dp, color = AppColors.Separator)
}

/** A tappable row inside a GroupedSection. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueColor: Color = AppColors.Secondary,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    chevron: Boolean = false,
    titleColor: Color = AppColors.Label,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor, maxLines = 1)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (chevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = com.jash.salescashtracker.ui.Glass.Muted)
        }
    }
}

/** Rounded colored square icon, like iOS Settings. */
@Composable
fun IconBadge(color: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(color),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun KpiCard(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null, valueColor: Color = AppColors.Label) {
    Surface(shape = RoundedCornerShape(14.dp), color = AppColors.Card, modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppColors.Secondary, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = AppColors.Secondary, maxLines = 1)
        }
    }
}

enum class PillKind { Good, Warn, Bad, Info, Neutral }

fun pillKindFor(status: String): PillKind = when (status.uppercase()) {
    "PAID", "COMPLETED", "USED" -> PillKind.Good
    "PARTIAL", "HOLD", "ENQUIRY" -> PillKind.Warn
    "PENDING", "CANCELLED", "REFUND", "REFUNDED" -> PillKind.Bad
    "SUBMITTED", "OPEN", "ACTIVE" -> PillKind.Info
    else -> PillKind.Neutral
}

@Composable
fun Pill(text: String, kind: PillKind = pillKindFor(text)) {
    val (bg, fg) = when (kind) {
        PillKind.Good -> com.jash.salescashtracker.ui.Glass.GreenBg to AppColors.GreenText
        PillKind.Warn -> com.jash.salescashtracker.ui.Glass.AmberBg to AppColors.OrangeText
        PillKind.Bad -> com.jash.salescashtracker.ui.Glass.RedBg to AppColors.RedText
        PillKind.Info -> com.jash.salescashtracker.ui.Glass.YellowSoft to AppColors.Label
        PillKind.Neutral -> AppColors.Fill to AppColors.Secondary
    }
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** iOS-style search field (filled, rounded). */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        placeholder = { Text(placeholder, color = AppColors.Secondary) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = AppColors.Secondary) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, "Clear", tint = AppColors.Secondary) }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = com.jash.salescashtracker.ui.Glass.Field,
            unfocusedContainerColor = com.jash.salescashtracker.ui.Glass.Field,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

/** Field colours matching the design's inputs: white 86% fill, white edge, blue when focused. */
@Composable
fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = com.jash.salescashtracker.ui.Glass.Field,
    unfocusedContainerColor = com.jash.salescashtracker.ui.Glass.Field,
    focusedBorderColor = AppColors.Blue,
    unfocusedBorderColor = com.jash.salescashtracker.ui.Glass.Edge,
    focusedLabelColor = AppColors.Blue,
    unfocusedLabelColor = AppColors.Secondary,
)

@Composable
fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    caps: Boolean = true,
    required: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(if (required) "$label *" else label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        shape = RoundedCornerShape(15.dp),
        colors = glassFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (caps && keyboard == KeyboardType.Text) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
        ),
    )
}

/** Read-only value shown like a field (for calculated amounts). */
@Composable
fun CalcField(label: String, value: String, modifier: Modifier = Modifier, color: Color = AppColors.Label) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Fill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.Secondary)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

/** Pick-one field. If [editable], the user can also type a new value. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val shown = if (editable && value.isNotBlank()) options.filter { it.contains(value, ignoreCase = true) && it != value } else options
    ExposedDropdownMenuBox(expanded = expanded && shown.isNotEmpty(), onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (editable) { onChange(it.uppercase()); expanded = true } },
            readOnly = !editable,
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            colors = glassFieldColors(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(if (editable) MenuAnchorType.PrimaryEditable else MenuAnchorType.PrimaryNotEditable, true),
        )
        ExposedDropdownMenu(expanded = expanded && shown.isNotEmpty(), onDismissRequest = { expanded = false }) {
            if (!editable) {
                DropdownMenuItem(text = { Text("—", color = AppColors.Secondary) }, onClick = { onChange(""); expanded = false })
            }
            shown.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onChange(opt); expanded = false })
            }
        }
    }
}

/** Date field with the Material 3 date picker. Value is ISO yyyy-MM-dd ("" for none). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    iso: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = if (iso.isBlank()) "" else fdate(iso),
            onValueChange = {},
            label = { Text(label) },
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            trailingIcon = { Icon(Icons.Filled.DateRange, null) },
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = com.jash.salescashtracker.ui.Glass.Field,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = com.jash.salescashtracker.ui.Glass.Edge,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth().clickable { open = true },
        )
    }
    if (open) {
        val initial = runCatching { LocalDate.parse(iso.take(10)) }.getOrNull() ?: LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (allowClear && iso.isNotBlank()) TextButton(onClick = { onChange(""); open = false }) { Text("Clear") }
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            },
        ) { DatePicker(state = state) }
    }
}

@Composable
fun SegmentedChips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(com.jash.salescashtracker.ui.Glass.Field)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) com.jash.salescashtracker.ui.Glass.Card else Color.Transparent)
                    .clickable { onSelect(key) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}

@Composable
fun RowScope.Weighted(content: @Composable () -> Unit) {
    Box(Modifier.weight(1f)) { content() }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = AppColors.Label)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.Secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
