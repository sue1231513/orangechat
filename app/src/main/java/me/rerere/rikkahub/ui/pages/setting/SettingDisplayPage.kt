/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus

@Composable
fun SettingDisplayPage() {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.setting_display_page_title))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor)
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.setting_display_page_title)) },
                ) {
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayTheme) },
                        headlineContent = { Text("主题外观") },
                        supportingContent = { Text("动态色、预设主题、AMOLED 暗黑模式") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayColor) },
                        headlineContent = { Text("颜色自定义") },
                        supportingContent = { Text("气泡、背景、主色调等颜色") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayTransparency) },
                        headlineContent = { Text("透明度设置") },
                        supportingContent = { Text("气泡、思维链、侧边栏透明度") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayIllustration) },
                        headlineContent = { Text("插图素材") },
                        supportingContent = { Text("输入框背景、侧边栏背景、头像挂件") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayMessage) },
                        headlineContent = { Text("消息显示") },
                        supportingContent = { Text("头像、气泡、字体大小、自定义字体") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayCodeInteraction) },
                        headlineContent = { Text("代码与交互") },
                        supportingContent = { Text("代码块、回车发送、滚动、音量键等") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayGeneral) },
                        headlineContent = { Text("通用设置") },
                        supportingContent = { Text("启动时新建对话、更新提醒") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayNotification) },
                        headlineContent = { Text("通知与TTS") },
                        supportingContent = { Text("消息生成通知、TTS 自动朗读") },
                    )
                }
            }
        }
    }
}