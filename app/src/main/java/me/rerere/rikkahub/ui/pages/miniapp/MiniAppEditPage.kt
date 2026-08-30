/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.miniapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import me.rerere.rikkahub.ui.theme.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Rocket01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.MiniApp
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.RikkaConfirmDialog
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppEditPage(
    id: String?,
    vm: MiniAppViewModel = koinViewModel(),
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val existing = id?.let { uuid ->
        settings.miniApps.find { it.id == Uuid.parse(uuid) }
    }

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var url by remember(existing?.id) { mutableStateOf(existing?.url ?: "") }
    var icon by remember(existing?.id) { mutableStateOf(existing?.icon ?: "") }
    var description by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isEditing = existing != null
    val canSave = name.isNotBlank() && url.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑 Mini App" else "添加 Mini App") },
                navigationIcon = { BackButton() },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = HugeIcons.Delete01,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = CustomColors.topBarColors,
            )
        },
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("名称") },
                placeholder = { Text("例如：每日一言") },
                singleLine = true,
                leadingIcon = {
                    Icon(HugeIcons.Rocket01, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL") },
                placeholder = { Text("https://example.com/miniapp") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = icon,
                onValueChange = { icon = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("图标 URL（可选）") },
                placeholder = { Text("留空使用默认图标") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("描述（可选）") },
                placeholder = { Text("一句话描述这个 Mini App") },
                minLines = 2,
                maxLines = 4,
            )

            Button(
                onClick = {
                    val miniApp = MiniApp(
                        id = existing?.id ?: Uuid.random(),
                        name = name.trim(),
                        url = url.trim(),
                        icon = icon.trim(),
                        description = description.trim(),
                    )
                    if (existing == null) {
                        vm.add(miniApp)
                    } else {
                        vm.update(miniApp)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            ) {
                Text("保存")
            }

            if (!isEditing) {
                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("取消")
                }
            }
        }
    }

    RikkaConfirmDialog(
        show = showDeleteDialog,
        title = "删除 Mini App",
        confirmText = "删除",
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            existing?.let { vm.delete(it.id) }
            showDeleteDialog = false
            navController.popBackStack()
        },
        onDismiss = { showDeleteDialog = false },
    ) {
        Text("确定要删除 \"${existing?.name ?: ""}\" 吗？")
    }
}
