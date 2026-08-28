/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.event

sealed class AppEvent {
    data class Speak(val text: String) : AppEvent()
    data class EmojiSelected(val emoji: String) : AppEvent()

    /**
     * AI 在文字聊天中主动请求发起语音通话.
     * 由 request_voice_call 工具发出, RouteActivity 监听后弹出来电界面.
     */
    data class RequestVoiceCall(val conversationId: String) : AppEvent()
}
