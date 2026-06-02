package com.uglydeveloper.ulinhub

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)

        // Проверяем, не поделились ли с нами ссылкой
        handleIncomingIntent(intent)

        setContent {
            val context = LocalContext.current
            val darkTheme = isSystemInDarkTheme()
            val currentContextLocale = AppCompatDelegate.getApplicationLocales()[0]
            val configuration = LocalConfiguration.current

            // Если пользователь вручную сменил язык, принудительно обновляем конфигурацию Compose
            LaunchedEffect(currentContextLocale) {
                if (currentContextLocale != null) {
                    val locale = Locale(currentContextLocale.language)
                    Locale.setDefault(locale)
                    configuration.setLocale(locale)
                    // Обновляем контекст ресурсов
                    context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
                }
            }

            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LinkStashApp(dbHelper)
                }
            }
        }
    }

    // Логика перехвата
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                // Извлекаем заголовок страницы, если он есть (часто браузеры его передают)
                val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?:  getString(R.string.saved_link)

                // Сохраняем напрямую в нашу локальную базу данных!
                dbHelper.addLink(sharedText, sharedSubject)

                // Выводим легкое уведомление
                makeText(this, getString(R.string.toast_saved), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall", "LocalContextConfigurationRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkStashApp(dbHelper: DatabaseHelper) {
    val context = LocalContext.current

    val linksList = remember { mutableStateListOf<LinkModel>().apply { addAll(dbHelper.getAllLinks()) } }

    var inputUrl by remember { mutableStateOf("") }
    var inputTitle by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var linkToDelete by remember { mutableStateOf<LinkModel?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var linkToEdit by remember { mutableStateOf<LinkModel?>(null) }

    var editTitle by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.top_bar_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                actions = {
                    TextButton(
                        onClick = {
                            val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language
                                ?: LocaleListCompat.getDefault()[0]?.language

                            val newLocale = if (currentLocale == "ru") "en" else "ru"
                            val localeList = LocaleListCompat.forLanguageTags(newLocale)

                            // Изменяем системно
                            AppCompatDelegate.setApplicationLocales(localeList)

                            // Форсируем обновление для Compose прямо в текущем контексте
                            val locale = Locale(newLocale)
                            Locale.setDefault(locale)
                            val resources = context.resources
                            val configuration = resources.configuration
                            configuration.setLocale(locale)
                            resources.updateConfiguration(configuration, resources.displayMetrics)
                        }
                    ) {
                        val currentLocale = AppCompatDelegate.getApplicationLocales()[0]?.language
                        Text(
                            text = if (currentLocale == "en") "Ru" else "En",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    )
    { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Поля ввода
            OutlinedTextField(
                value = inputTitle,
                onValueChange = { inputTitle = it },
                label = { Text(stringResource(R.string.label_title)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text(stringResource(R.string.label_url)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Кнопка добавления
            Button(
                onClick = {
                    if (inputUrl.isNotBlank() && inputTitle.isNotBlank()) {
                        dbHelper.addLink(inputUrl, inputTitle)

                        // Обновляем UI список из базы
                        linksList.clear()
                        linksList.addAll(dbHelper.getAllLinks())

                        // Чистим поля
                        inputUrl = ""
                        inputTitle = ""
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.btn_save))
            }

            Spacer(modifier = Modifier.height(16.dp))

            val uriHandler = LocalUriHandler.current

            Text(text = stringResource(R.string.section_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = linksList,
                    key = { it.id }
                ) { link ->

                    val coroutineScope = rememberCoroutineScope()

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                linkToDelete = link
                                showDeleteDialog = true
                                false // Придерживаем карточку
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(300),
                            fadeOutSpec = tween(300),
                            placementSpec = tween(300)
                        ),
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val isTargeted =
                                dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                            val backgroundColor by animateColorAsState(
                                if (isTargeted) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                label = "bg_color"
                            )
                            val iconScale by animateFloatAsState(
                                if (isTargeted) 1.3f else 1.0f,
                                label = "icon_scale"
                            )

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(backgroundColor, shape = CardDefaults.shape)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.btn_delete),
                                    tint = if (isTargeted) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.scale(iconScale)
                                )
                            }
                        },
                        content = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            try {
                                                uriHandler.openUri(link.url)
                                            } catch (e: Exception) {
                                                makeText(
                                                    context,
                                                    context.getString(R.string.toast_invalid_url),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onLongClick = {
                                            linkToEdit = link
                                            editTitle = link.title
                                            editUrl = link.url
                                            showEditDialog = true
                                        }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = link.title,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = link.url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    )
                    if (showDeleteDialog && linkToDelete?.id == link.id) {
                        AlertDialog(
                            onDismissRequest = {
                                showDeleteDialog = false
                                linkToDelete = null
                                // Если кликнули мимо — плавно возвращаем карточку назад
                                coroutineScope.launch { dismissState.reset() }
                            },
                            title = { Text(stringResource(R.string.dialog_delete_title)) },
                            text = { Text(stringResource(R.string.dialog_delete_text, link.title)) }, // %1$s подставит имя автоматически!
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        dbHelper.deleteLink(link.id)
                                        linksList.remove(link)
                                        showDeleteDialog = false
                                        linkToDelete = null
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(stringResource(R.string.btn_delete))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteDialog = false
                                        linkToDelete = null
                                        // Пользователь нажал "Отмена" — плавно закрываем свайп!
                                        coroutineScope.launch { dismissState.reset() }
                                    }
                                ) {
                                    Text(stringResource(R.string.btn_cancel))
                                }
                            }
                        )
                    }
                }
            }
            if (showEditDialog && linkToEdit != null) {
                AlertDialog(
                    onDismissRequest = {
                        showEditDialog = false
                        linkToEdit = null
                    },
                    title = { Text(stringResource(R.string.dialog_edit_title)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text(stringResource(R.string.label_dialog)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editUrl,
                                onValueChange = { editUrl = it },
                                label = { Text(stringResource(R.string.label_url)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                linkToEdit?.let { oldLink ->
                                    if (editUrl.isNotBlank() && editTitle.isNotBlank()) {
                                        // 1. Обновляем в локальной базе
                                        dbHelper.updateLink(oldLink.id, editUrl, editTitle)

                                        // 2. Обновляем в стейт-списке на экране, чтобы UI сразу перерисовался
                                        val index = linksList.indexOf(oldLink)
                                        if (index != -1) {
                                            linksList[index] =
                                                LinkModel(oldLink.id, editUrl, editTitle)
                                        }
                                    }
                                }
                                showEditDialog = false
                                linkToEdit = null
                            }
                        ) {
                            Text(stringResource(R.string.btn_save))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showEditDialog = false
                            linkToEdit = null
                        }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                )
            }
        }
    }
}




