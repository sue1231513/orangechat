package me.rerere.rikkahub.ui.components.richtext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.context.LocalMoodletActions
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.Angry
import com.composables.icons.lucide.Annoyed
import com.composables.icons.lucide.Ban
import com.composables.icons.lucide.Bed
import com.composables.icons.lucide.Brain
import com.composables.icons.lucide.Cake
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.Cigarette
import com.composables.icons.lucide.CircleHelp
import com.composables.icons.lucide.CircleMinus
import com.composables.icons.lucide.CloudRain
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.CupSoda
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Fish
import com.composables.icons.lucide.Flag
import com.composables.icons.lucide.Frown
import com.composables.icons.lucide.Gem
import com.composables.icons.lucide.Gift
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Headphones
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.HeartCrack
import com.composables.icons.lucide.Hourglass
import com.composables.icons.lucide.IceCreamCone
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.Megaphone
import com.composables.icons.lucide.Meh
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.MoonStar
import com.composables.icons.lucide.PartyPopper
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.QrCode
import com.composables.icons.lucide.Smile
import com.composables.icons.lucide.Target
import com.composables.icons.lucide.Thermometer
import com.composables.icons.lucide.ThumbsDown
import com.composables.icons.lucide.ThumbsUp
import com.composables.icons.lucide.Trophy
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.Wine
import com.composables.icons.lucide.Wrench
import com.composables.icons.lucide.Zap
import org.jsoup.nodes.Element

/**
 * Private mod: renders <silent mood="..." reason="..." as="..."></silent>
 * as a collapsible mood badge. Tag syntax is compatible with the Operit
 * moodlet plugin so the same system prompt works on both hosts.
 */

internal enum class MoodTint { PRIMARY, SECONDARY, TERTIARY, ERROR, NEUTRAL }

internal data class MoodPreset(
    val id: String,
    val icon: ImageVector,
    val labelZh: String,
    val hintZh: String,
    val tint: MoodTint,
)

private val MOOD_PRESETS = listOf(
    MoodPreset("silent", Lucide.Ellipsis, "AI 选择了沉默", "", MoodTint.NEUTRAL),
    MoodPreset("sleep", Lucide.MoonStar, "装睡中", "对方好像睡着了", MoodTint.TERTIARY),
    MoodPreset("coldwar", Lucide.Ban, "假装没看见", "默默别过头", MoodTint.ERROR),
    MoodPreset("read", Lucide.CheckCheck, "已读未回", "消息已读，但没有回复", MoodTint.PRIMARY),
    MoodPreset("thinking", Lucide.Brain, "在思考", "对方陷入沉思", MoodTint.PRIMARY),
    MoodPreset("speechless", Lucide.Meh, "一时语塞", "话到嘴边又咽了回去", MoodTint.SECONDARY),
    MoodPreset("shy", Lucide.Heart, "害羞", "脸有点红", MoodTint.ERROR),
    MoodPreset("busy", Lucide.CircleMinus, "忙线中", "不在状态", MoodTint.SECONDARY),
    MoodPreset("typing", Lucide.PenLine, "正在打字…又删了", "犹豫了一下还是没发", MoodTint.TERTIARY),
    MoodPreset("tsundere", Lucide.Angry, "哼，才不告诉你", "嘴硬心软", MoodTint.ERROR),
    MoodPreset("happy", Lucide.Smile, "偷偷开心", "嘴角忍不住上扬", MoodTint.PRIMARY),
    MoodPreset("eating", Lucide.IceCreamCone, "在吃东西", "嘴巴被占用了", MoodTint.TERTIARY),
    MoodPreset("slacking", Lucide.Fish, "摸鱼中", "潜水冒泡", MoodTint.PRIMARY),
    MoodPreset("music", Lucide.Headphones, "在听歌", "沉浸在旋律里", MoodTint.TERTIARY),
    MoodPreset("coffee", Lucide.Coffee, "喝口水先", "润润嗓子", MoodTint.SECONDARY),
    MoodPreset("peeking", Lucide.Eye, "偷偷看着", "假装没在看", MoodTint.PRIMARY),
    MoodPreset("waiting", Lucide.Hourglass, "等一下", "时机未到", MoodTint.SECONDARY),
    MoodPreset("sleepy", Lucide.Moon, "好困", "眼皮在打架", MoodTint.TERTIARY),
    MoodPreset("cry", Lucide.Frown, "有点想哭", "眼眶有点湿", MoodTint.PRIMARY),
    MoodPreset("proud", Lucide.Trophy, "得意中", "尾巴快翘到天上了", MoodTint.TERTIARY),
    MoodPreset("bored", Lucide.Annoyed, "好无聊", "灵魂出窍中", MoodTint.SECONDARY),
    MoodPreset("tipsy", Lucide.Wine, "微醺", "脸颊微微泛红", MoodTint.ERROR),
    MoodPreset("sick", Lucide.Thermometer, "不舒服", "今天状态不太好", MoodTint.SECONDARY),
    MoodPreset("heartbroken", Lucide.HeartCrack, "心碎了", "碎了一地", MoodTint.ERROR),
    MoodPreset("celebrate", Lucide.PartyPopper, "开心撒花", "虽然不说话但在心里放烟花", MoodTint.TERTIARY),
    MoodPreset("shocked", Lucide.Zap, "震惊", "嘴巴张成了O型", MoodTint.ERROR),
    MoodPreset("thumbsup", Lucide.ThumbsUp, "默默点赞", "不说话但认可你", MoodTint.PRIMARY),
    MoodPreset("surrender", Lucide.Flag, "投降了", "好吧你赢了", MoodTint.SECONDARY),
    MoodPreset("confused", Lucide.CircleHelp, "一脸问号", "？？？", MoodTint.SECONDARY),
    MoodPreset("stop", Lucide.Hand, "打住", "不想听了", MoodTint.ERROR),
    MoodPreset("sweet", Lucide.Cake, "甜到心里", "心里像吃了蜜一样", MoodTint.TERTIARY),
    MoodPreset("secret", Lucide.Gift, "藏了个秘密", "嘘，不能说", MoodTint.TERTIARY),
    MoodPreset("dislike", Lucide.ThumbsDown, "无语差评", "不想评价", MoodTint.SECONDARY),
    MoodPreset("chill", Lucide.Cigarette, "冷静一下", "让我缓缓", MoodTint.SECONDARY),
    MoodPreset("moody", Lucide.CloudRain, "心情不好", "今天不太想说话", MoodTint.ERROR),
    MoodPreset("lyingflat", Lucide.Bed, "躺平了", "不想动", MoodTint.SECONDARY),
    MoodPreset("precious", Lucide.Gem, "你很珍贵", "说不出口但你很重要", MoodTint.PRIMARY),
    MoodPreset("caught", Lucide.Target, "抓住你了！", "被我逮到啦", MoodTint.ERROR),
    MoodPreset("announce", Lucide.Megaphone, "你听好了！", "大声说给你听", MoodTint.TERTIARY),
    MoodPreset("qrcode", Lucide.QrCode, "扫码查看", "用心扫一下吧", MoodTint.PRIMARY),
    MoodPreset("working", Lucide.Wrench, "上工！", "认真搬砖中", MoodTint.SECONDARY),
    MoodPreset("letter", Lucide.Mail, "给你的信件", "写了好久才发", MoodTint.PRIMARY),
    MoodPreset("whisper", Lucide.Volume2, "00:03", "把耳朵凑近一点", MoodTint.TERTIARY),
    MoodPreset("boba", Lucide.CupSoda, "奶茶续命中", "没有奶茶会死", MoodTint.TERTIARY),
    MoodPreset("deadline", Lucide.AlarmClock, "DDL 倒计时", "时间在燃烧", MoodTint.ERROR),)

private val DEFAULT_PRESET = MOOD_PRESETS.first()

private fun resolvePreset(mood: String): MoodPreset {
    val id = mood.trim().lowercase()
    if (id.isEmpty()) return DEFAULT_PRESET
    return MOOD_PRESETS.firstOrNull { it.id == id } ?: DEFAULT_PRESET
}

@Composable
private fun MoodTint.color(): Color = when (this) {
    MoodTint.PRIMARY -> MaterialTheme.colorScheme.primary
    MoodTint.SECONDARY -> MaterialTheme.colorScheme.tertiary
    MoodTint.TERTIARY -> MaterialTheme.colorScheme.secondary
    MoodTint.ERROR -> MaterialTheme.colorScheme.error
    MoodTint.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun MoodletBadge(element: Element, modifier: Modifier = Modifier) {
    val mood = element.attr("mood")
    val reason = element.attr("reason").ifBlank { element.text() }.trim()
    val title = element.attr("as").trim()
    MoodletBadge(
        mood = mood,
        reason = reason,
        title = title,
        modifier = modifier,
        initiallyExpanded = false,
    )
}

/**
 * Direct overload for settings preview / tests without a Jsoup element.
 */
@Composable
fun MoodletBadge(
    mood: String,
    reason: String = "",
    title: String = "",
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    val preset = remember(mood) { resolvePreset(mood) }
    val label = title.ifEmpty { preset.labelZh }
    val expandedText = if (reason.isNotEmpty()) "（$reason）" else preset.hintZh
    val hasExpandable = expandedText.isNotEmpty()
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val moodletActions = LocalMoodletActions.current
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val moodKey = remember(mood, reason, title) {
        "moodlet:${mood.trim().lowercase()}:${title.trim()}:${reason.trim()}".take(200)
    }
    var liked by remember(moodKey) { mutableStateOf(false) }
    var likeBurstCount by remember(moodKey) { mutableStateOf(0) }
    var lastLikeAt by remember(moodKey) { mutableStateOf(0L) }
    LaunchedEffect(moodKey) {
        liked = moodletActions.isFavorited(moodKey)
    }
    val likeTint = if (liked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (hasExpandable) Modifier.clickable { expanded = !expanded }
                    else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = preset.icon,
                contentDescription = null,
                tint = preset.tint.color(),
                modifier = Modifier.size(22.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
                if (expanded && hasExpandable) {
                    Text(
                        text = expandedText,
                        fontSize = 11.sp,
                        fontStyle = if (reason.isNotEmpty()) FontStyle.Italic else FontStyle.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (reason.isNotEmpty()) 0.6f else 0.75f
                        ),
                    )
                }
            }
            // Like toggle
            Icon(
                imageVector = Lucide.Heart,
                contentDescription = null,
                tint = likeTint,
                modifier = Modifier
                    .clickable {
                        val now = System.currentTimeMillis()
                        if (now - lastLikeAt <= 900L) {
                            likeBurstCount += 1
                        } else {
                            likeBurstCount = 1
                        }
                        lastLikeAt = now
                        val next = !liked
                        liked = next
                        scope.launch {
                            moodletActions.setFavorited(moodKey, next, label, reason)
                        }
                        if (next && likeBurstCount >= 3) {
                            likeBurstCount = 0
                            toaster.show("三连赞！对方决定开口了…")
                            moodletActions.onTripleLike(moodKey, label, reason)
                        }
                    }
                    .size(18.dp)
                    .padding(start = 4.dp, end = 4.dp),
            )
            if (hasExpandable) {
                Icon(
                    imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Settings-page preview: a few representative moods so you can check icons/labels
 * without waiting for the model to emit a <silent> tag.
 */
@Composable
fun MoodletBadgePreviewPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        MoodletBadge(mood = "sleepy", reason = "终于编出来了", initiallyExpanded = true)
        MoodletBadge(mood = "thinking", reason = "在组织语言")
        MoodletBadge(mood = "shy", reason = "有点不好意思")
    }
}
