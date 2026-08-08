/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.context

import androidx.compose.runtime.compositionLocalOf

/**
 * Actions for moodlet badge interactions (real favorite + triple-like bonus reply).
 */
data class MoodletActions(
    val isFavorited: suspend (moodKey: String) -> Boolean = { false },
    val setFavorited: suspend (moodKey: String, favorited: Boolean, label: String, reason: String) -> Unit = { _, _, _, _ -> },
    val onTripleLike: (moodKey: String, label: String, reason: String) -> Unit = { _, _, _ -> },
)

val LocalMoodletActions = compositionLocalOf { MoodletActions() }
