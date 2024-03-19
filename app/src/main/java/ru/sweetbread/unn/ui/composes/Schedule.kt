package ru.sweetbread.unn.ui.composes

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import ru.sweetbread.unn.R
import ru.sweetbread.unn.ui.Auditorium
import ru.sweetbread.unn.ui.Building
import ru.sweetbread.unn.ui.Discipline
import ru.sweetbread.unn.ui.KindOfWork
import ru.sweetbread.unn.ui.Lecturer
import ru.sweetbread.unn.ui.LecturerRank
import ru.sweetbread.unn.ui.ScheduleUnit
import ru.sweetbread.unn.ui.getSchedule
import ru.sweetbread.unn.ui.theme.UNNTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun Schedule() {
    val state = rememberWeekCalendarState(
        firstDayOfWeek = DayOfWeek.MONDAY  // TODO: set start and end weeks to September and July of current year
    )

    Column {
        var curDate by remember { mutableStateOf(LocalDate.now()) }
        WeekCalendar(
            state = state,
            dayContent = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .aspectRatio(1f) // This is important for square sizing!
                        .offset(2.dp)
                        .background(if (it.date == curDate) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(
                            onClick = { curDate = it.date },
                            enabled = curDate != it.date
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = it.date.dayOfMonth.toString(),
                        fontWeight = if (it.date == LocalDate.now()) FontWeight.Bold else null
                    )
                }
            }
        )
        ScheduleDay(date = curDate)
    }
}

@Composable
fun ScheduleDay(modifier: Modifier = Modifier, date: LocalDate) {
    val scope = rememberCoroutineScope()
    var loadedDate by remember { mutableStateOf(LocalDate.MIN) }
    val lessons = remember { mutableListOf<ScheduleUnit>() }
    var expanded by remember { mutableIntStateOf(0) }

    if (loadedDate == date) {
        Log.d("Loaded", "${date.format(DateTimeFormatter.ISO_DATE)} ${lessons.size}")
        LazyColumn (modifier) {
            items(lessons) { // TODO: Add empty list notification
                ScheduleItem(unit = it, modifier = Modifier.clickable {
                    expanded = if (it.oid == expanded) 0
                    else it.oid
                }, expanded = expanded == it.oid)
            }
        }
    } else {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            trackColor = MaterialTheme.colorScheme.secondary,
        )
        LaunchedEffect(date != loadedDate) {
            scope.launch(Dispatchers.IO) {
                lessons.clear()
                lessons.addAll(getSchedule(start = date, finish = date))
                loadedDate = date
            }
        }
    }
}

@Composable
fun ScheduleItem(modifier: Modifier = Modifier, unit: ScheduleUnit, expanded: Boolean = false) {
    val begin = unit.begin.format(DateTimeFormatter.ofPattern("HH:mm"))
    val end = unit.end.format(DateTimeFormatter.ofPattern("HH:mm"))

    Row (
        modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if ((LocalDateTime.of(
                        unit.date,
                        unit.begin
                    ) < LocalDateTime.now()) and (LocalDateTime.now() < LocalDateTime.of(
                        unit.date,
                        unit.end
                    ))
                )
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            )
            .padding(8.dp)
    ){
        Column (Modifier.weight(1f)) {
            Column {
                Text(
                    text = unit.discipline.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.zIndex(1f),
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column {
                Text(
                    text = unit.kindOfWork.name,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedVisibility (expanded) {
                    Text(text = unit.stream)
                }
            }

            AnimatedVisibility (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            AnimatedVisibility (!expanded) {
                Row(Modifier) {
                    Text(
                        text = unit.auditorium.name,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = unit.auditorium.building.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AnimatedVisibility (expanded) {
                Column {
                    Text(
                        text = "${stringResource(R.string.auditorium)}: ${unit.auditorium.name}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "${stringResource(R.string.building)}: ${unit.auditorium.building.name}",
                        overflow = TextOverflow.Ellipsis
                    )
                    if (unit.auditorium.floor != 0) {
                        Text(
                            text = "${stringResource(R.string.floor)}: ${unit.auditorium.floor}",
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Column {
                        Text(text = unit.lecturers[0].name, fontWeight = FontWeight.Bold)
                        Text(text = stringResource(unit.lecturers[0].rank.id))
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(begin.toString(), fontWeight = FontWeight.Bold)
                        Text(end.toString())
                    }
                }
            }
        }

        AnimatedVisibility (!expanded) {
            Column {
                Text(begin.toString(), fontWeight = FontWeight.Bold)
                Text(end.toString())
            }
        }
    }
}

@Preview
@Composable
fun ScheduleItemPreview() {
    val unit = ScheduleUnit(
        oid = 1,
        Auditorium(
            name = "с/з 1(110)",
            oid = 3752,
            floor = 0,
            building = Building(
                name = "Корпус 6",
                gid = 30,
                oid = 155
            ),
        ),
        date = LocalDate.of(2024, 3, 11),
        discipline = Discipline(
            name = "Физическая культура и спорт (элективная дисциплина)",
            oid = 67895,
            type = 0
        ),
        kindOfWork = KindOfWork(
            name = "Практика (семинарские занятия)",
            oid = 261,
            uid = "281474976710661",
            complexity = 1
        ),
        lecturers = arrayListOf(
            Lecturer(
                name = "Фамилия Имя Отчество",
                rank = LecturerRank.SLecturer,
                email = "",
                oid = 28000,
                uid = "51000"
            )
        ),
        stream = "3823Б1ПР1|3823Б1ПР2|3823Б1ПР3|3823Б1ПР4|3823Б1ПР5-В-OUP",
        begin = LocalTime.of(10, 50),
        end = LocalTime.of(12, 20)
    )

    UNNTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ScheduleItem(unit = unit)
        }
    }
}

@Preview
@Composable
fun ScheduleExpandedItemPreview() {
    val unit = ScheduleUnit(
        oid = 1,
        Auditorium(
            name = "с/з 1(110)",
            oid = 3752,
            floor = 0,
            building = Building(
                name = "Корпус 6",
                gid = 30,
                oid = 155
            ),
        ),
        date = LocalDate.of(2024, 3, 11),
        discipline = Discipline(
            name = "Физическая культура и спорт (элективная дисциплина)",
            oid = 67895,
            type = 0
        ),
        kindOfWork = KindOfWork(
            name = "Практика (семинарские занятия)",
            oid = 261,
            uid = "281474976710661",
            complexity = 1
        ),
        lecturers = arrayListOf(
            Lecturer(
                name = "Фамилия Имя Отчество",
                rank = LecturerRank.SLecturer,
                email = "",
                oid = 28000,
                uid = "51000"
            )
        ),
        stream = "3823Б1ПР1|3823Б1ПР2|3823Б1ПР3|3823Б1ПР4|3823Б1ПР5-В-OUP",
        begin = LocalTime.of(10, 50),
        end = LocalTime.of(12, 20)
    )

    UNNTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ScheduleItem(unit = unit, expanded = true)
        }
    }
}