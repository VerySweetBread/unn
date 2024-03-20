package ru.sweetbread.unn.ui.composes

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.sweetbread.unn.R
import ru.sweetbread.unn.ui.ImageSet
import ru.sweetbread.unn.ui.Post
import ru.sweetbread.unn.ui.Type
import ru.sweetbread.unn.ui.User
import ru.sweetbread.unn.ui.getBlogposts
import ru.sweetbread.unn.ui.getUserByBitrixId
import ru.sweetbread.unn.ui.portalURL
import ru.sweetbread.unn.ui.theme.UNNTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val defUser = User(
    null,
    123,
    123,
    Type.Student,
    "cool.email@domain.com",
    "Джон Сигма Омегович",
    "Jon Sigma Omega",
    true,
    LocalDate.now(),
    ImageSet(
        "https://upload.wikimedia.org/wikipedia/ru/thumb/9/94/%D0%93%D0%B8%D0%B3%D0%B0%D1%87%D0%B0%D0%B4.jpg/500px-%D0%93%D0%B8%D0%B3%D0%B0%D1%87%D0%B0%D0%B4.jpg",
        "https://upload.wikimedia.org/wikipedia/ru/thumb/9/94/%D0%93%D0%B8%D0%B3%D0%B0%D1%87%D0%B0%D0%B4.jpg/500px-%D0%93%D0%B8%D0%B3%D0%B0%D1%87%D0%B0%D0%B4.jpg",
        "https://upload.wikimedia.org/wikipedia/ru/thumb/9/94/%D0%93%D0%B8%D0%B3%D0%B0%D1%87%D0%B0%D0%B4.jpg/500px-%D0%93%D0%B8%D0%B3%D0%B0%D1%87%D0%B0%D0%B4.jpg"
    )
)


@Composable
fun Blogposts(viewModel: PostViewModel = viewModel()) {
    val posts by viewModel.posts.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPosts()
    }

    if (posts.isNotEmpty()) {
        Log.d("Another fuck", posts.size.toString())
        LazyColumn {
            items(posts) {
                PostItem(
                    Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    post = it
                )
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
    }
}


class PostRepository {
    suspend fun loadPosts(): List<Post> {
        return getBlogposts()
    }
}

class PostViewModel : ViewModel() {
    private val repository = PostRepository()
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    suspend fun loadPosts() {
        _posts.value = repository.loadPosts()
    }
}


@Composable
@NonRestartableComposable
fun UserItem(modifier: Modifier = Modifier, user: User, info: String? = null) {
    Row(
        modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AsyncImage(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(50)),
            model = portalURL + user.avatar.thumbnail,
            contentDescription = user.nameEn
        )

        Column {
            Text(user.nameRu, fontWeight = FontWeight.Bold)
            if (!info.isNullOrBlank())
                Text(
                    text = info,
                    fontStyle = FontStyle.Italic,
                    fontSize = MaterialTheme.typography.labelLarge.fontSize
                )
        }
    }
}

@Composable
@NonRestartableComposable
fun PostItem(modifier: Modifier = Modifier, post: Post) {
    var user: User? by remember { mutableStateOf(null) }

    LaunchedEffect(post) {
        user = getUserByBitrixId(post.authorId)
    }

    Column(modifier.padding(16.dp)) {
        Log.d("FUUUUUUUUUUUCK", user.toString())
        if (user != null)
            UserItem(user = user!!)
        else
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                trackColor = MaterialTheme.colorScheme.secondary,
            )

        Text(text = post.content)
        Text(text = post.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
    }
}


@Preview
@Composable
fun UserItemPreview() {
    UNNTheme {
        Surface {
            UserItem(
                Modifier
                    .width(300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer), defUser, Type.Student.s
            )
        }
    }
}

@Preview
@Composable
fun PostItemPreview() {
    val post = Post(
        id = 154923,
        authorId = 165945,
        enableComments = true,
        numComments = 0,
        date = LocalDateTime.of(2024, 3, 20, 18, 55, 20),
        content = stringResource(id = R.string.lorem)
    )

    UNNTheme {
        Surface {
            PostItem(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer), post
            )
        }
    }
}