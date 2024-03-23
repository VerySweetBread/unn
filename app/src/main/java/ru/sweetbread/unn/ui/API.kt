package ru.sweetbread.unn.ui

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import org.json.JSONArray
import org.json.JSONObject
import ru.sweetbread.unn.R
import ru.sweetbread.unn.ui.layout.LoginData
import ru.sweetbread.unn.ui.layout.client
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private lateinit var PHPSESSID: String
private lateinit var CSRF: String
lateinit var ME: User

const val portalURL = "https://portal.unn.ru"
const val ruzapiURL = "$portalURL/ruzapi"
const val vuzapiURL = "$portalURL/bitrix/vuz/api"
const val restURL = "$portalURL/rest"

enum class Type(val s: String) {
    Student("student"),
    Group("group"),
    Lecturer("lecturer"),
    Auditorium("auditorium")
}

enum class LecturerRank(val id: Int) {
    Assistant(R.string.assistant),
    Lecturer(R.string.lecturer),
    SLecturer(R.string.slecturer),
    AProfessor(R.string.aprofessor)
}

class ScheduleUnit(val oid: Int,
                   val auditorium: Auditorium,
                   val date: LocalDate,
                   val discipline: Discipline,
                   val kindOfWork: KindOfWork,
                   val lecturers: ArrayList<Lecturer>,
                   val stream: String,
                   val begin: LocalTime,
                   val end: LocalTime)

class Auditorium(  val name: String,
                   val oid: Int,
                   val floor: Int,
                   val building: Building)
class Building(    val name: String,
                   val gid: Int,
                   val oid: Int)

class Discipline(  val name: String,
                   val oid: Int,
                   val type: Int)

class KindOfWork(  val name: String,
                   val oid: Int,
                   val uid: String,
                   val complexity: Int)

class Lecturer(   val name: String,
                  val rank: LecturerRank,
                  val email: String,
                  val unnId: Int,
                  val uid: String)

class User (val unnId: Int?,
            val bitrixId: Int,
            val userId: Int,
            val type: Type,
            val email: String,
            val nameRu: String,
            val nameEn: String,
            val isMale: Boolean,
            val birthday: LocalDate,
            val avatar: ImageSet)

class Post(
    val id: Int,
    val authorId: Int,
    val enableComments: Boolean,
    val numComments: Int,
    val date: LocalDateTime,
    val content: String)

class ImageSet(val original: String,
               val thumbnail: String,
               val small: String)

/**
 * Authorize user by [login] and [password]
 *
 * Also defines local vars [PHPSESSID] and [ME]
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
        getCSRF()
        return true
    }
    return false
}

/**
 * Save info about current [User] in memory
 */
private suspend fun getMyself(login: String) {
    val studentinfo = JSONObject(client.get("$ruzapiURL/studentinfo") {
        parameter("uns", login.substring(1))
    }.bodyAsText())

    val user = JSONObject(
        client.get("$vuzapiURL/user") {
            header("Cookie", "PHPSESSID=${PHPSESSID}")
        }.bodyAsText()
    )

    ME = User(
        unnId = studentinfo.getString("id").toInt(),
        bitrixId = user.getInt("bitrix_id"),
        userId = user.getInt("id"),
        type = when(studentinfo.getString("type")) {
            "lecturer" -> Type.Lecturer  // ig,,,
            else -> Type.Student
        },
        email = user.getString("email"),
        nameRu = user.getString("fullname"),
        nameEn = user.getString("fullname_en"),
        isMale = user.getString("sex") == "M",
        birthday = LocalDate.parse(
            user.getString("birthdate"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        ),
        avatar = user.getJSONObject("photo").let {
            ImageSet(
                it.getString("orig"),
                it.getString("thumbnail"),
                it.getString("small"),
            )
        }
    )
}

suspend fun getSchedule(
    type: Type = ME.type,
    id: Int = ME.unnId!!,
    start: LocalDate,
    finish: LocalDate
): ArrayList<ScheduleUnit> {
    val unnDatePattern = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    val r = client.get("$ruzapiURL/schedule/${type.s}/$id") {
        parameter("start", start.format(unnDatePattern))
        parameter("finish", finish.format(unnDatePattern))
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
                    unnId = lecturer.getInt("lecturerOid"),
                    uid = lecturer.getString("lecturerUID"),
                    rank = when (lecturer.getString("lecturer_rank")) {
                        "АССИСТ" -> LecturerRank.Assistant
                        "СТПРЕП" -> LecturerRank.SLecturer
                        "ДОЦЕНТ" -> LecturerRank.AProfessor
                        else -> LecturerRank.Lecturer
                    }
                )
            )
        }

        out.add(
            ScheduleUnit(
                oid = unit.getInt("lessonOid"),
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
                date = LocalDate.parse(unit.getString("date"), unnDatePattern),
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

suspend fun getCSRF() {
    val r = client.get("$restURL/log.blogpost.get") {
        header("Cookie", "PHPSESSID=${PHPSESSID}")
        parameter("sessid", "")
    }
    CSRF = JSONObject(r.bodyAsText()).getString("sessid")
}

suspend fun getBlogposts(): ArrayList<Post> {
    val r = client.get("$restURL/log.blogpost.get") {
        header("Cookie", "PHPSESSID=${PHPSESSID}")
        parameter("sessid", CSRF)
    }
    val json = JSONObject(r.bodyAsText())
    val result = json.getJSONArray("result")

    val out = arrayListOf<Post>()
    for (i in 0 until result.length()) {
        val el = result.getJSONObject(i)
        out.add(
            Post(
                id = el.getString("ID").toInt(),
                authorId = el.getString("AUTHOR_ID").toInt(),
                enableComments = el.getString("ENABLE_COMMENTS") == "Y",
                numComments = el.getString("NUM_COMMENTS").toInt(),
                date = LocalDateTime.parse(
                    el.getString("DATE_PUBLISH"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+03:00'")
                ),
                content = el.getString("DETAIL_TEXT")
            )
        )
    }
    return out
}

suspend fun getUserByBitrixId(id: Int): User {
    val userId = JSONObject(client.get("$vuzapiURL/user/bx/$id") {
        header("Cookie", "PHPSESSID=${PHPSESSID}")
    }.bodyAsText()).getInt("id")
    return getUser(userId)
}

suspend fun getUser(id: Int): User {
    val json = JSONObject(
        client.get("$vuzapiURL/user/$id") {
            header("Cookie", "PHPSESSID=${PHPSESSID}")
        }.bodyAsText()
    )

    return User(
        unnId = null,
        bitrixId = json.getInt("bitrix_id"),
        userId = json.getInt("id"),
        type = when (json.getJSONArray("profiles").getJSONObject(0).getString("type")) {
            "lecturer" -> Type.Lecturer  // ig,,,
            else -> Type.Student
        },
        email = json.getString("email"),
        nameRu = json.getString("fullname"),
        nameEn = json.getString("fullname_en"),
        isMale = json.getString("sex") == "M",
        birthday = LocalDate.parse(
            json.getString("birthdate"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        ),
        avatar = json.getJSONObject("photo").let {
            ImageSet(
                it.getString("orig"),
                it.getString("thumbnail"),
                it.getString("small"),
            )
        }
    )
}