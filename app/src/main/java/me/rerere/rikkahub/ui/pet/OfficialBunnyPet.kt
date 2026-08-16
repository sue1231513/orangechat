package me.rerere.rikkahub.ui.pet

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.rerere.rikkahub.R

enum class OfficialBunnyPose {
    IDLE,
    SIT,
    WALK,
    HAPPY,
    HEART,
    BACK,
    LOOK,
    SLEEP,
    SHY,
    ANGRY,
    TOUCHED,
    POKED,
    RECONCILE,
}

sealed interface BunnyFrame {
    data class Drawable(@param:DrawableRes val id: Int) : BunnyFrame
    data class Atlas(val index: Int) : BunnyFrame
}

data class BunnyAnimationSpec(
    val frames: List<BunnyFrame>,
    val frameDurationMillis: Long,
)

fun PetMotion.toOfficialBunnyPose(): OfficialBunnyPose = when (this) {
    PetMotion.IDLE -> OfficialBunnyPose.IDLE
    PetMotion.GENTLE -> OfficialBunnyPose.SIT
    PetMotion.APPROACH -> OfficialBunnyPose.WALK
    PetMotion.AFFECTIONATE -> OfficialBunnyPose.HAPPY
    PetMotion.STAY_CLOSE -> OfficialBunnyPose.HEART
    PetMotion.CAUTIOUS -> OfficialBunnyPose.BACK
    PetMotion.LOOK -> OfficialBunnyPose.LOOK
    PetMotion.SLEEP -> OfficialBunnyPose.SLEEP
    PetMotion.SHY -> OfficialBunnyPose.SHY
    PetMotion.ANGRY -> OfficialBunnyPose.ANGRY
    PetMotion.TOUCHED -> OfficialBunnyPose.TOUCHED
    PetMotion.POKED -> OfficialBunnyPose.POKED
    PetMotion.RECONCILE -> OfficialBunnyPose.RECONCILE
}

private fun drawable(@DrawableRes id: Int) = BunnyFrame.Drawable(id)
private fun atlas(index: Int) = BunnyFrame.Atlas(index)

fun OfficialBunnyPose.animationSpec(): BunnyAnimationSpec = when (this) {
    OfficialBunnyPose.IDLE -> BunnyAnimationSpec(
        frames = listOf(drawable(R.drawable.pet_bunny_idle_1), drawable(R.drawable.pet_bunny_idle_2)),
        frameDurationMillis = 850L,
    )
    OfficialBunnyPose.SIT -> BunnyAnimationSpec(
        frames = listOf(drawable(R.drawable.pet_bunny_sit_1), drawable(R.drawable.pet_bunny_sit_2)),
        frameDurationMillis = 780L,
    )
    OfficialBunnyPose.WALK -> BunnyAnimationSpec(
        frames = listOf(
            drawable(R.drawable.pet_bunny_walk_right_1),
            drawable(R.drawable.pet_bunny_walk_right_2),
            drawable(R.drawable.pet_bunny_walk_right_3),
        ),
        frameDurationMillis = 190L,
    )
    OfficialBunnyPose.HAPPY -> BunnyAnimationSpec(
        frames = listOf(drawable(R.drawable.pet_bunny_happy_1), drawable(R.drawable.pet_bunny_happy_2)),
        frameDurationMillis = 330L,
    )
    OfficialBunnyPose.HEART -> BunnyAnimationSpec(
        frames = listOf(drawable(R.drawable.pet_bunny_heart_1), drawable(R.drawable.pet_bunny_heart_2)),
        frameDurationMillis = 520L,
    )
    OfficialBunnyPose.BACK -> BunnyAnimationSpec(
        frames = listOf(drawable(R.drawable.pet_bunny_back_1), drawable(R.drawable.pet_bunny_back_2)),
        frameDurationMillis = 900L,
    )
    OfficialBunnyPose.LOOK -> BunnyAnimationSpec(
        frames = listOf(atlas(0), atlas(1), atlas(2)),
        frameDurationMillis = 420L,
    )
    OfficialBunnyPose.SLEEP -> BunnyAnimationSpec(
        frames = listOf(atlas(3), atlas(4)),
        frameDurationMillis = 980L,
    )
    OfficialBunnyPose.SHY -> BunnyAnimationSpec(
        frames = listOf(atlas(5), atlas(6)),
        frameDurationMillis = 560L,
    )
    OfficialBunnyPose.ANGRY -> BunnyAnimationSpec(
        frames = listOf(atlas(7), atlas(8)),
        frameDurationMillis = 360L,
    )
    OfficialBunnyPose.TOUCHED -> BunnyAnimationSpec(
        frames = listOf(atlas(9), atlas(10)),
        frameDurationMillis = 240L,
    )
    OfficialBunnyPose.POKED -> BunnyAnimationSpec(
        frames = listOf(atlas(11), atlas(12)),
        frameDurationMillis = 180L,
    )
    OfficialBunnyPose.RECONCILE -> BunnyAnimationSpec(
        frames = listOf(atlas(13), atlas(14)),
        frameDurationMillis = 480L,
    )
}

@Composable
fun OfficialBunnyPet(
    presentation: PetPresentation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val pose = presentation.motion.toOfficialBunnyPose()
    val animation = remember(pose) { pose.animationSpec() }
    var frameIndex by remember(pose) { mutableIntStateOf(0) }

    LaunchedEffect(pose, animation.frameDurationMillis, animation.frames.size) {
        frameIndex = 0
        while (animation.frames.size > 1) {
            delay(animation.frameDurationMillis)
            frameIndex = (frameIndex + 1) % animation.frames.size
        }
    }

    val transition = rememberInfiniteTransition(label = "official-bunny")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = when (pose) {
            OfficialBunnyPose.WALK -> -2.5f
            OfficialBunnyPose.HAPPY -> -3.5f
            OfficialBunnyPose.HEART -> -2f
            OfficialBunnyPose.TOUCHED -> -4f
            OfficialBunnyPose.POKED -> -2.5f
            OfficialBunnyPose.RECONCILE -> -2f
            OfficialBunnyPose.SLEEP -> -0.4f
            OfficialBunnyPose.BACK -> -0.7f
            else -> -1.2f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (pose) {
                    OfficialBunnyPose.HAPPY -> 620
                    OfficialBunnyPose.WALK -> 570
                    OfficialBunnyPose.TOUCHED -> 420
                    OfficialBunnyPose.POKED -> 320
                    OfficialBunnyPose.ANGRY -> 500
                    else -> 1450
                },
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bunny-bob",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.992f,
        targetValue = when (pose) {
            OfficialBunnyPose.BACK, OfficialBunnyPose.SLEEP -> 1f
            OfficialBunnyPose.TOUCHED -> 1.018f
            else -> 1.008f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1550),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bunny-pulse",
    )

    val frame = animation.frames[frameIndex]
    val context = LocalContext.current
    val atlasBitmap = if (frame is BunnyFrame.Atlas) {
        remember(context.resources) {
            try {
                BitmapFactory.decodeResource(context.resources, R.drawable.pet_bunny_state_atlas)
                    ?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    } else {
        null
    }
    val painter = when (frame) {
        is BunnyFrame.Drawable -> painterResource(frame.id)
        is BunnyFrame.Atlas -> {
            val bitmap = atlasBitmap
            if (bitmap == null) {
                painterResource(R.drawable.pet_bunny_idle_1)
            } else {
                remember(frame.index, bitmap) {
                    val column = frame.index % 5
                    val row = frame.index / 5
                    BitmapPainter(
                        image = bitmap,
                        srcOffset = IntOffset(column * 96, row * 96),
                        srcSize = IntSize(96, 96),
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = "兔眠兔",
            modifier = Modifier
                .size(112.dp)
                .offset(y = bob.dp)
                .scale(pulse),
        )
    }
}