package ru.sweetbread.unn.ui.composes

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun Schedule() {
    val state = rememberWeekCalendarState(
        firstDayOfWeek = DayOfWeek.MONDAY
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
                        .background(if (it.date == curDate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(
                            onClick = {
                                curDate = it.date
                                Log.d("Here bug (olClick)",
                                    curDate.format(DateTimeFormatter.ISO_DATE)
                                )
                            }
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

    if (loadedDate == date) {
        Log.d("Loaded", "${date.format(DateTimeFormatter.ISO_DATE)} ${lessons.size}")
        LazyColumn (modifier) {
            items(lessons) {
                ScheduleItem(unit = it)
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
                Log.d("Loading", "${date.format(DateTimeFormatter.ISO_DATE)} ${lessons.size}")
                Log.d("Here bug", "${loadedDate.format(DateTimeFormatter.ISO_DATE)} ${date.format(DateTimeFormatter.ISO_DATE)}")
            }
        }
    }
}

@Composable
fun ScheduleItem(modifier: Modifier = Modifier, unit: ScheduleUnit) {
    Row (
        modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp)
    ){
        Column (Modifier.weight(1f)) {
            Text(
                text = unit.discipline.name,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.zIndex(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = unit.kindOfWork.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row (Modifier) {
                Text(text = unit.auditorium.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
                Text(text = unit.auditorium.building.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Column {
            val begin = unit.begin.format(DateTimeFormatter.ofPattern("HH:mm"))
            val end = unit.end.format(DateTimeFormatter.ofPattern("HH:mm"))
            Text(begin.toString(), fontWeight = FontWeight.Bold)
            Text(end.toString())
        }
    }
}

@Preview
@Composable
fun ScheduleItemPreview() {
    val unit = ScheduleUnit(
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
                oid = 28407,
                uid = "51769"
            )
        ),
        stream = "3823Б1ПР1|3823Б1ПР2|3823Б1ПР3|3823Б1ПР4|3823Б1ПР5-В-OUP",
        begin = LocalTime.of(10, 50),
        end = LocalTime.of(12, 20)
    )

    UNNTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colorScheme.background) {
            ScheduleItem(unit = unit)
        }
    }
}