package ru.sweetbread.unn.ui

import android.util.Log
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import org.json.JSONArray
import org.json.JSONObject
import ru.sweetbread.unn.ui.layout.LoginData
import ru.sweetbread.unn.ui.layout.client
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private lateinit var PHPSESSID: String
lateinit var ME: User

const val portalURL = "https://portal.unn.ru"
const val ruzapiURL = "$portalURL/ruzapi"

enum class Type(val s: String) {
    Student("student"),
    Group("group"),
    Lecturer("lecturer"),
    Auditorium("auditorium")
}

enum class LecturerRank(val s: String) {
    Lecturer("Lecturer"),
    SLecturer("Senior Lecturer")
}

class ScheduleUnit(
    val auditorium: Auditorium,
    val date: LocalDate,
    val discipline: Discipline,
    val kindOfWork: KindOfWork,
    val lecturers: ArrayList<Lecturer>,
    val stream: String,
    val begin: LocalTime,
    val end: LocalTime)

class Auditorium (val name: String,
                  val oid: Int,
                  val floor: Int,
                  val building: Building)
class Building(val name: String,
               val gid: Int,
               val oid: Int)

class Discipline (val name: String,
                  val oid: Int,
                  val type: Int)

class KindOfWork (val name: String,
                  val oid: Int,
                  val uid: String,
                  val complexity: Int)

class Lecturer (val name: String,
                val rank: LecturerRank,
                val email: String,
                val oid: Int,
                val uid: String)

class User (val id: String,
            val uns: String,
            val type: Type,
            val email: String,
            val name: String,
            val info: String)

/**
 * Authorize user by [login] and [password]
 *
 * Also defines local vars [PHPSESSID] and [ME.id]
 */
suspend fun auth(login: String = LoginData.login, password: String = LoginData.password, forced: Boolean = false): Boolean {
    if (!forced) {
        if (::PHPSESSID.isInitialized and ::ME.isInitialized)
            return true
    }
    val r = client.submitForm("$portalURL/auth/?login=yes",
        formParameters = parameters {
            append("AUTH_FORM", "Y")
            append("TYPE", "AUTH")
            append("backurl", "/")
            append("USER_LOGIN", login)
            append("USER_PASSWORD", password)
        }
    )
    if (r.status.value == 302) {
        PHPSESSID = """PHPSESSID=([\w\d]+)""".toRegex().find(r.headers["Set-Cookie"]!!)!!.groupValues[1]
        getMyself(login)
        return true
    }
    return false
}

private suspend fun getMyself(login: String) {
    val r = client.get("$ruzapiURL/studentinfo") {
        parameter("uns", login.substring(1))
    }
    val json = JSONObject(r.bodyAsText())
    ME = User(
        id = json.getString("id"),
        uns = json.getString("uns"),
        type = when(json.getString("type")) {
            "lecturer" -> Type.Lecturer  // ig,,,
            else -> Type.Student
        },
        email = json.getString("email"),
        name = json.getString("fio"),
        info = json.getString("info")
    )
}

suspend fun getSchedule(type: Type = Type.Student, id: String = ME.id, start: LocalDate, finish: LocalDate): ArrayList<ScheduleUnit> {
    val r = client.get("$ruzapiURL/schedule/${type.s}/$id") {
        parameter("start", start.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
        parameter("finish", finish.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
        parameter("lng", "1")
    }
    val json = JSONArray(r.bodyAsText())
    val out = arrayListOf<ScheduleUnit>()
    for (i in 0 until json.length()) {
        val unit = json.getJSONObject(i)
        val lecturesJson = unit.getJSONArray("listOfLecturers")
        val lecturers = arrayListOf<Lecturer>()

        for (j in 0 until lecturesJson.length()) {
            val lecturer = lecturesJson.getJSONObject(j)
            lecturers.add(
                Lecturer(
                    name = lecturer.getString("lecturer"),
                    email = lecturer.getString("lecturerEmail"),
                    oid = lecturer.getInt("lecturerOid"),
                    uid = lecturer.getString("lecturerUID"),
                    rank = when (lecturer.getString("lecturer_rank")) {
                        "СТПРЕП" -> LecturerRank.SLecturer
                        else -> LecturerRank.Lecturer
                    }
                )
            )
        }

        out.add(
            ScheduleUnit(
                auditorium = Auditorium(
                    name = unit.getString("auditorium"),
                    oid = unit.getInt("auditoriumOid"),
                    floor = unit.getInt("auditoriumfloor"),
                    building = Building(
                        name = unit.getString("building"),
                        gid = unit.getInt("buildingGid"),
                        oid = unit.getInt("buildingOid")
                    )
                ),
                date = LocalDate.parse(unit.getString("date"), DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                discipline = Discipline(
                    name = unit.getString("discipline"),
                    oid = unit.getInt("disciplineOid"),
                    type = unit.getInt("disciplinetypeload")
                ),
                kindOfWork = KindOfWork(
                    name = unit.getString("kindOfWork"),
                    complexity = unit.getInt("kindOfWorkComplexity"),
                    oid = unit.getInt("kindOfWorkOid"),
                    uid = unit.getString("kindOfWorkUid")
                ),
                lecturers = lecturers,
                stream = unit.getString("stream"),
                begin = LocalTime.parse(unit.getString("beginLesson"), DateTimeFormatter.ofPattern("HH:mm")),
                end = LocalTime.parse(unit.getString("endLesson"), DateTimeFormatter.ofPattern("HH:mm"))
            )
        )
    }
    return out
}
