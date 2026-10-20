package io.github.tanakalun.llyfr.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tanakalun.llyfr.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NumberPicker
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Calendar

/**
 * 待办提醒时间选择器：OverlayBottomSheet + 年月日/时分 两行 NumberPicker。
 * 取消走手势/点外/返回（onDismissRequest），仅保留 endAction 确认按钮。
 */
@Composable
fun DateTimePickerSheet(
    show: Boolean,
    initialMillis: Long? = null,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { Calendar.getInstance() }
    val default = initialMillis?.let { Calendar.getInstance().apply { timeInMillis = it } }
        ?: remember { Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) } }

    var year by remember { mutableIntStateOf(default.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(default.get(Calendar.MONTH) + 1) }
    var day by remember { mutableIntStateOf(default.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(default.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(default.get(Calendar.MINUTE)) }
    val curYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.choose_reminder_time),
        startAction = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(R.string.cancel),
                )
            }
        },
        endAction = {
            IconButton(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(year, month - 1, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(cal.timeInMillis)
                },
            ) {
                Icon(
                    imageVector = MiuixIcons.Ok,
                    contentDescription = stringResource(R.string.done),
                )
            }
        },
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberPicker(
                    value = year,
                    onValueChange = { year = it },
                    range = curYear..curYear + 20,
                    label = { it.toString() },
                    wrapAround = false,
                    modifier = Modifier.weight(1f),
                )
                UnitLabel("年")
                NumberPicker(
                    value = month,
                    onValueChange = { month = it },
                    range = 1..12,
                    label = { it.toString().padStart(2, '0') },
                    wrapAround = true,
                    modifier = Modifier.weight(1f),
                )
                UnitLabel("月")
                NumberPicker(
                    value = day,
                    onValueChange = { day = it },
                    range = 1..31,
                    label = { it.toString().padStart(2, '0') },
                    wrapAround = true,
                    modifier = Modifier.weight(1f),
                )
                UnitLabel("日")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                NumberPicker(
                    value = hour,
                    onValueChange = { hour = it },
                    range = 0..23,
                    label = { it.toString().padStart(2, '0') },
                    wrapAround = true,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = ":",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                NumberPicker(
                    value = minute,
                    onValueChange = { minute = it },
                    range = 0..59,
                    label = { it.toString().padStart(2, '0') },
                    wrapAround = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun UnitLabel(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}