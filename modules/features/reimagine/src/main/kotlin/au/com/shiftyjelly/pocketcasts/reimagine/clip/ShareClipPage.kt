package au.com.shiftyjelly.pocketcasts.reimagine.clip

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.BaseRowButton
import au.com.shiftyjelly.pocketcasts.compose.components.PagerDotIndicator
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.reimagine.clip.SharingState.Step
import au.com.shiftyjelly.pocketcasts.reimagine.ui.BackgroundAssetController
import au.com.shiftyjelly.pocketcasts.reimagine.ui.CardCoordinates
import au.com.shiftyjelly.pocketcasts.reimagine.ui.ClipSelector
import au.com.shiftyjelly.pocketcasts.reimagine.ui.CloseButton
import au.com.shiftyjelly.pocketcasts.reimagine.ui.EpisodeCard
import au.com.shiftyjelly.pocketcasts.reimagine.ui.HorizontalEpisodeCard
import au.com.shiftyjelly.pocketcasts.reimagine.ui.PlatformBar
import au.com.shiftyjelly.pocketcasts.reimagine.ui.ShareColors
import au.com.shiftyjelly.pocketcasts.reimagine.ui.SharingThemedSnackbar
import au.com.shiftyjelly.pocketcasts.reimagine.ui.scrollBottomFade
import au.com.shiftyjelly.pocketcasts.sharing.CardType
import au.com.shiftyjelly.pocketcasts.sharing.Clip
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform
import au.com.shiftyjelly.pocketcasts.sharing.VisualCardType
import java.sql.Date
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal interface ShareClipPageListener {
    fun onShareClip(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, platform: SocialPlatform, cardType: CardType)
    fun onClickPlay()
    fun onClickPause()
    fun onUpdateClipStart(duration: Duration)
    fun onUpdateClipEnd(duration: Duration)
    fun onUpdateClipProgress(duration: Duration)
    fun onUpdateTimeline(scale: Float, secondsPerTick: Int)
    fun onShowPlatformSelection()
    fun onShowClipSelection()
    fun onClose()

    companion object {
        val Preview = object : ShareClipPageListener {
            override fun onShareClip(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, platform: SocialPlatform, cardType: CardType) = Unit
            override fun onClickPlay() = Unit
            override fun onClickPause() = Unit
            override fun onUpdateClipStart(duration: Duration) = Unit
            override fun onUpdateClipEnd(duration: Duration) = Unit
            override fun onUpdateClipProgress(duration: Duration) = Unit
            override fun onUpdateTimeline(scale: Float, secondsPerTick: Int) = Unit
            override fun onShowPlatformSelection() = Unit
            override fun onShowClipSelection() = Unit
            override fun onClose() = Unit
        }
    }
}

@Composable
internal fun ShareClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    sharingState: SharingState,
    shareColors: ShareColors,
    platforms: Set<SocialPlatform>,
    useEpisodeArtwork: Boolean,
    useKeyboardInput: Boolean,
    assetController: BackgroundAssetController,
    onNavigationButtonClick: () -> Unit,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
    listener: ShareClipPageListener,
    state: ClipPageState = rememberClipPageState(
        firstVisibleItemIndex = (clipRange.startInSeconds - 10).coerceAtLeast(0),
    ),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    when (LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> HorizontalClipPage(
            episode = episode,
            podcast = podcast,
            clipRange = clipRange,
            playbackProgress = playbackProgress,
            isPlaying = isPlaying,
            sharingState = sharingState,
            useEpisodeArtwork = useEpisodeArtwork,
            platforms = platforms,
            shareColors = shareColors,
            useKeyboardInput = useKeyboardInput,
            assetController = assetController,
            listener = listener,
            onNavigationButtonClick = onNavigationButtonClick,
            onEditClick = onEditClick,
            onCloseClick = onCloseClick,
            state = state,
            snackbarHostState = snackbarHostState,
        )
        else -> VerticalClipPage(
            episode = episode,
            podcast = podcast,
            clipRange = clipRange,
            playbackProgress = playbackProgress,
            isPlaying = isPlaying,
            sharingState = sharingState,
            useEpisodeArtwork = useEpisodeArtwork,
            platforms = platforms,
            shareColors = shareColors,
            useKeyboardInput = useKeyboardInput,
            assetController = assetController,
            listener = listener,
            onNavigationButtonClick = onNavigationButtonClick,
            onEditClick = onEditClick,
            onCloseClick = onCloseClick,
            state = state,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun VerticalClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    sharingState: SharingState,
    shareColors: ShareColors,
    platforms: Set<SocialPlatform>,
    useEpisodeArtwork: Boolean,
    useKeyboardInput: Boolean,
    assetController: BackgroundAssetController,
    listener: ShareClipPageListener,
    onNavigationButtonClick: () -> Unit,
    onEditClick: () -> Unit,
    onCloseClick: () -> Unit,
    state: ClipPageState,
    snackbarHostState: SnackbarHostState,
) {
    Box(
        modifier = Modifier
            .background(shareColors.background)
            .fillMaxSize(),
    ) {
        AnimatedVisiblity(podcast = podcast, episode = episode) { podcast, episode ->
            Column {
                // Hide audio card until audio clips are available
                val pagerState = rememberPagerState(pageCount = { CardType.visualEntries.size })
                val scrollState = rememberScrollState()
                val selectedCard = CardType.visualEntries[pagerState.currentPage]
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .scrollBottomFade(scrollState)
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .verticalScroll(scrollState),
                ) {
                    DescriptionContent(
                        sharingState = sharingState,
                        shareColors = shareColors,
                        selectedCard = selectedCard,
                        onEditClick = onEditClick,
                        listener = listener,
                        state = state,
                    )
                    PagingContent(
                        episode = episode,
                        podcast = podcast,
                        sharingState = sharingState,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                        assetController = assetController,
                        state = state,
                        scrollState = scrollState,
                        pagerState = pagerState,
                    )
                }
                PageControlsContent(
                    episode = episode,
                    podcast = podcast,
                    clipRange = clipRange,
                    playbackProgress = playbackProgress,
                    isPlaying = isPlaying,
                    sharingState = sharingState,
                    platforms = platforms,
                    shareColors = shareColors,
                    useKeyboardInput = useKeyboardInput,
                    selectedCard = selectedCard,
                    onNavigationButtonClick = onNavigationButtonClick,
                    listener = listener,
                    state = state,
                )
            }
        }
        CloseButton(
            shareColors = shareColors,
            onClick = {
                onCloseClick()
                listener.onClose()
            },
            modifier = Modifier
                .padding(top = 12.dp, end = 12.dp)
                .align(Alignment.TopEnd),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { data -> SharingThemedSnackbar(data, shareColors) },
        )
    }
}

@Composable
private fun HorizontalClipPage(
    episode: PodcastEpisode?,
    podcast: Podcast?,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    sharingState: SharingState,
    shareColors: ShareColors,
    platforms: Set<SocialPlatform>,
    useEpisodeArtwork: Boolean,
    useKeyboardInput: Boolean,
    assetController: BackgroundAssetController,
    listener: ShareClipPageListener,
    onNavigationButtonClick: () -> Unit,
    onCloseClick: () -> Unit,
    onEditClick: () -> Unit,
    state: ClipPageState,
    snackbarHostState: SnackbarHostState,
) {
    Box(
        modifier = Modifier
            .background(shareColors.background)
            .fillMaxSize(),
    ) {
        AnimatedVisiblity(podcast, episode) { podcast, episode ->
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                DescriptionContent(
                    sharingState = sharingState,
                    shareColors = shareColors,
                    selectedCard = CardType.Horizontal,
                    onEditClick = onEditClick,
                    listener = listener,
                    state = state,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Spacer(
                        modifier = Modifier.weight(0.1f),
                    )
                    HorizontalEpisodeCard(
                        episode = episode,
                        podcast = podcast,
                        useEpisodeArtwork = useEpisodeArtwork,
                        shareColors = shareColors,
                        captureController = assetController.captureController(CardType.Horizontal),
                        constrainedSize = { maxWidth, maxHeight -> DpSize(maxWidth.coerceAtMost(400.dp), maxHeight) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(
                        modifier = Modifier.weight(0.05f),
                    )
                    PageControlsContent(
                        episode = episode,
                        podcast = podcast,
                        clipRange = clipRange,
                        playbackProgress = playbackProgress,
                        isPlaying = isPlaying,
                        sharingState = sharingState,
                        platforms = platforms,
                        shareColors = shareColors,
                        useKeyboardInput = useKeyboardInput,
                        selectedCard = CardType.Horizontal,
                        listener = listener,
                        onNavigationButtonClick = onNavigationButtonClick,
                        state = state,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(
                        modifier = Modifier.weight(0.1f),
                    )
                }
            }
        }
        CloseButton(
            shareColors = shareColors,
            onClick = {
                onCloseClick()
                listener.onClose()
            },
            modifier = Modifier
                .padding(top = 12.dp, end = 12.dp)
                .align(Alignment.TopEnd),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { data -> SharingThemedSnackbar(data, shareColors) },
        )
    }
}

@Composable
private fun DescriptionContent(
    sharingState: SharingState,
    shareColors: ShareColors,
    selectedCard: CardType,
    onEditClick: () -> Unit,
    listener: ShareClipPageListener,
    state: ClipPageState,
) {
    val titleId = if (selectedCard is CardType.Audio) LR.string.share_clip_create_audio_label else LR.string.share_clip_create_label
    val descriptionId = if (selectedCard is CardType.Audio) LR.string.share_clip_create_audio_description else LR.string.single_space
    val orientation = LocalConfiguration.current.orientation
    AnimatedContent(
        label = "TopContent",
        targetState = Triple(sharingState.step, titleId, descriptionId),
        modifier = Modifier.onGloballyPositioned { coordinates -> state.topContentHeight = coordinates.size.height },
    ) { (step, titleId, descriptionId) ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(
                modifier = Modifier.height(
                    when (step) {
                        Step.ClipSelection -> 40.dp
                        Step.PlatformSelection -> 24.dp
                    },
                ),
            )
            TextH30(
                text = stringResource(
                    when (step) {
                        Step.ClipSelection -> titleId
                        Step.PlatformSelection -> LR.string.share_clip_share_label
                    },
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = shareColors.onBackgroundPrimary,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(
                modifier = Modifier.height(8.dp),
            )
            val alpha by animateFloatAsState(targetValue = if (sharingState.iSharing) 0.3f else 1f)
            when (step) {
                Step.ClipSelection -> when (orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> Unit
                    else -> TextH40(
                        text = stringResource(descriptionId),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        color = shareColors.onBackgroundSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                Step.PlatformSelection -> TextH40(
                    text = stringResource(LR.string.share_clip_edit_label),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    color = shareColors.onContainerPrimary,
                    fontWeight = FontWeight.W400,
                    modifier = Modifier
                        .alpha(alpha)
                        .background(shareColors.container, CircleShape)
                        .defaultMinSize(minHeight = 24.dp)
                        .clickable(
                            interactionSource = remember(::MutableInteractionSource),
                            indication = ripple(color = shareColors.accent),
                            onClickLabel = stringResource(LR.string.share_clip_edit_label),
                            role = Role.Button,
                            onClick = {
                                onEditClick()
                                listener.onShowClipSelection()
                            },
                        )
                        .padding(vertical = 4.dp, horizontal = 16.dp),
                )
            }
            val bottomSpace = when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> 0.dp
                else -> when (step) {
                    Step.ClipSelection -> 12.dp
                    Step.PlatformSelection -> 48.dp
                }
            }
            Spacer(
                modifier = Modifier.height(bottomSpace),
            )
        }
    }
}

@Composable
private fun ColumnScope.PagingContent(
    episode: PodcastEpisode,
    podcast: Podcast,
    sharingState: SharingState,
    shareColors: ShareColors,
    useEpisodeArtwork: Boolean,
    assetController: BackgroundAssetController,
    state: ClipPageState,
    scrollState: ScrollState,
    pagerState: PagerState,
) {
    AnimatedVisibility(
        label = "DotPagerIndicator",
        visible = sharingState.step == Step.ClipSelection,
        modifier = Modifier.onGloballyPositioned { coordinates -> state.pagerIndicatorHeight = coordinates.size.height },
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            PagerDotIndicator(
                state = pagerState,
                activeDotColor = shareColors.onBackgroundPrimary,
                inactiveDotColor = shareColors.onBackgroundSecondary,
            )
        }
    }

    val coordiantes = CardCoordinates.estimate(
        topContentHeight = state.topContentHeight + state.pagerIndicatorHeight,
        scrollState = scrollState,
    )
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = sharingState.step == Step.ClipSelection && !sharingState.iSharing,
        modifier = Modifier.height(coordiantes.size.height),
    ) { pageIndex ->
        val cardType = CardType.entires[pageIndex]
        val offset by animateIntOffsetAsState(
            targetValue = coordiantes.offset(cardType),
        )
        val modifier = Modifier
            .offset { offset }
            .fillMaxSize()
            .padding(coordiantes.padding)

        when (cardType) {
            is VisualCardType -> EpisodeCard(
                cardType = cardType,
                episode = episode,
                podcast = podcast,
                useEpisodeArtwork = useEpisodeArtwork,
                shareColors = shareColors,
                captureController = assetController.captureController(cardType),
                constrainedSize = { _, _ -> coordiantes.size },
                modifier = modifier,
            )
            is CardType.Audio -> Box(
                contentAlignment = Alignment.Center,
                modifier = modifier,
            ) {
                Image(
                    painter = painterResource(IR.drawable.ic_audio_card),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(shareColors.onBackgroundPrimary),
                )
            }
        }
    }
}

@Composable
private fun PageControlsContent(
    episode: PodcastEpisode,
    podcast: Podcast,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    sharingState: SharingState,
    shareColors: ShareColors,
    platforms: Set<SocialPlatform>,
    useKeyboardInput: Boolean,
    selectedCard: CardType,
    listener: ShareClipPageListener,
    onNavigationButtonClick: () -> Unit,
    state: ClipPageState,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        label = "BottomContent",
        targetState = sharingState.step,
        modifier = modifier,
    ) { step ->
        when (step) {
            Step.ClipSelection -> ClipControls(
                episode = episode,
                podcast = podcast,
                clipRange = clipRange,
                playbackProgress = playbackProgress,
                isPlaying = isPlaying,
                sharingState = sharingState,
                shareColors = shareColors,
                useKeyboardInput = useKeyboardInput,
                selectedCard = selectedCard,
                listener = listener,
                onNavigationButtonClick = onNavigationButtonClick,
                state = state,
            )
            Step.PlatformSelection -> SharingControls(
                episode = episode,
                podcast = podcast,
                clipRange = clipRange,
                sharingState = sharingState,
                platforms = platforms,
                shareColors = shareColors,
                selectedCard = selectedCard,
                listener = listener,
            )
        }
    }
}

@Composable
private fun ClipControls(
    episode: PodcastEpisode,
    podcast: Podcast,
    clipRange: Clip.Range,
    playbackProgress: Duration,
    isPlaying: Boolean,
    sharingState: SharingState,
    shareColors: ShareColors,
    useKeyboardInput: Boolean,
    selectedCard: CardType,
    onNavigationButtonClick: () -> Unit,
    listener: ShareClipPageListener,
    state: ClipPageState,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        ClipSelector(
            episodeDuration = episode.duration.seconds,
            clipRange = clipRange,
            playbackProgress = playbackProgress,
            isPlaying = isPlaying,
            shareColors = shareColors,
            useKeyboardInput = useKeyboardInput,
            listener = listener,
            state = state.selectorState,
        )
        Spacer(
            modifier = Modifier.height(12.dp),
        )
        BaseRowButton(
            onClick = {
                onNavigationButtonClick()

                if (!sharingState.iSharing) {
                    when (selectedCard) {
                        CardType.Vertical, CardType.Horizontal, CardType.Square -> {
                            listener.onShowPlatformSelection()
                        }
                        CardType.Audio -> {
                            listener.onShareClip(
                                podcast = podcast,
                                episode = episode,
                                clipRange = clipRange,
                                cardType = CardType.Audio,
                                platform = SocialPlatform.More,
                            )
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = shareColors.accent),
            elevation = null,
            includePadding = false,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            val buttonText = stringResource(if (selectedCard is CardType.Audio) LR.string.share else LR.string.next)
            AnimatedContent(
                label = "ButtonText",
                targetState = buttonText to sharingState.iSharing,
            ) { (buttonText, isSharing) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextP40(
                        // Keep in the UI for to keep correct button size
                        text = if (isSharing) " " else buttonText,
                        textAlign = TextAlign.Center,
                        color = shareColors.onAccent,
                        modifier = Modifier.padding(6.dp),
                    )
                    if (isSharing) {
                        CircularProgressIndicator(
                            color = shareColors.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
        Spacer(
            modifier = Modifier.height(12.dp),
        )
    }
}

@Composable
private fun SharingControls(
    episode: PodcastEpisode,
    podcast: Podcast,
    clipRange: Clip.Range,
    sharingState: SharingState,
    shareColors: ShareColors,
    platforms: Set<SocialPlatform>,
    selectedCard: CardType,
    listener: ShareClipPageListener,
) {
    AnimatedContent(
        label = "SharingControls",
        targetState = sharingState.iSharing,
    ) { iSharing ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(vertical = 24.dp)
                .heightIn(min = 80.dp),
        ) {
            if (!iSharing) {
                PlatformBar(
                    platforms = platforms,
                    shareColors = shareColors,
                    onClick = { platform ->
                        if (!iSharing) {
                            listener.onShareClip(
                                podcast = podcast,
                                episode = episode,
                                clipRange = clipRange,
                                cardType = selectedCard,
                                platform = platform,
                            )
                        }
                    },
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextH40(
                        text = stringResource(LR.string.share_clip_sharing_clip),
                        color = shareColors.onBackgroundPrimary,
                    )
                    Spacer(
                        modifier = Modifier.height(24.dp),
                    )
                    LinearProgressIndicator(
                        color = shareColors.accent,
                    )
                    Spacer(
                        modifier = Modifier.height(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedVisiblity(
    podcast: Podcast?,
    episode: PodcastEpisode?,
    content: @Composable (Podcast, PodcastEpisode) -> Unit,
) = AnimatedVisibility(
    label = "ScreenContent",
    visible = podcast != null && episode != null,
    enter = fadeIn(),
    exit = fadeOut(),
) {
    if (podcast != null && episode != null) {
        content(podcast, episode)
    }
}

@Preview(name = "Regular", device = Devices.PORTRAIT_REGULAR, group = "vertical")
@Composable
private fun ShareClipVerticalRegularPreview() = ShareClipPagePreview()

@Preview(name = "Regular keyboard", device = Devices.PORTRAIT_REGULAR, group = "vertical")
@Composable
private fun ShareClipVerticalRegularKeyboardPreview() = ShareClipPagePreview(
    useKeyboardInput = true,
)

@Preview(name = "Regular sharing", device = Devices.PORTRAIT_REGULAR, group = "vertical")
@Composable
private fun ShareClipVerticalRegularSharingPreview() = ShareClipPagePreview(
    sharingState = SharingState(step = Step.PlatformSelection, iSharing = false),
)

@Preview(name = "Regular clipping", device = Devices.PORTRAIT_REGULAR, group = "vertical")
@Composable
private fun ShareClipVerticalRegularClippingPreview() = ShareClipPagePreview(
    sharingState = SharingState(step = Step.PlatformSelection, iSharing = true),
)

@Preview(name = "Small", device = Devices.PORTRAIT_SMALL, group = "vertical")
@Composable
private fun ShareClipVerticalSmallPreviewPreview() = ShareClipPagePreview()

@Preview(name = "Regular", device = Devices.LANDSCAPE_REGULAR, group = "horizontal")
@Composable
private fun ShareClipHorizontalRegularPreview() = ShareClipPagePreview()

@Preview(name = "Regular keyboard", device = Devices.LANDSCAPE_REGULAR, group = "horizontal")
@Composable
private fun ShareClipHorizontalRegularKeyboardPreview() = ShareClipPagePreview(
    useKeyboardInput = true,
)

@Preview(name = "Regular sharing", device = Devices.LANDSCAPE_REGULAR, group = "horizontal")
@Composable
private fun ShareClipHorizontalRegularSharingPreview() = ShareClipPagePreview(
    sharingState = SharingState(step = Step.PlatformSelection, iSharing = false),
)

@Preview(name = "Regular clipping", device = Devices.LANDSCAPE_REGULAR, group = "horizontal")
@Composable
private fun ShareClipHorizontalRegularClippingPreview() = ShareClipPagePreview(
    sharingState = SharingState(step = Step.PlatformSelection, iSharing = true),
)

@Preview(name = "Small", device = Devices.LANDSCAPE_SMALL, group = "horizontal")
@Composable
private fun ShareClipHorizontalSmallPreviewPreview() = ShareClipPagePreview()

@Preview(name = "Foldable", device = Devices.PORTRAIT_FOLDABLE, group = "irregular")
@Composable
private fun ShareClipVerticalFoldablePreviewPreview() = ShareClipPagePreview()

@Preview(name = "Foldable", device = Devices.LANDSCAPE_FOLDABLE, group = "irregular")
@Composable
private fun ShareClipHorizontalFoldablePreviewPreview() = ShareClipPagePreview()

@Preview(name = "Tablet", device = Devices.PORTRAIT_TABLET, group = "irregular")
@Composable
private fun ShareClipVerticalTabletPreview() = ShareClipPagePreview()

@Preview(name = "Tablet", device = Devices.LANDSCAPE_TABLET, group = "irregular")
@Composable
private fun ShareClipHorizontalTabletPreview() = ShareClipPagePreview()

@Composable
internal fun ShareClipPagePreview(
    color: Long = 0xFFEC0404,
    sharingState: SharingState = SharingState(step = Step.ClipSelection, iSharing = false),
    useKeyboardInput: Boolean = false,
) {
    val clipRange = Clip.Range(0.seconds, 15.seconds)
    ShareClipPage(
        episode = PodcastEpisode(
            uuid = "episode-id",
            podcastUuid = "podcast-id",
            publishedDate = Date.from(Instant.parse("2024-12-03T10:15:30.00Z")),
            title = "Episode title",
            duration = 250.0,
        ),
        podcast = Podcast(
            uuid = "podcast-id",
            title = "Podcast title",
            episodeFrequency = "monthly",
        ),
        clipRange = clipRange,
        playbackProgress = 8.seconds,
        isPlaying = false,
        sharingState = sharingState,
        useEpisodeArtwork = true,
        platforms = SocialPlatform.entries.toSet(),
        shareColors = ShareColors(Color(color)),
        useKeyboardInput = useKeyboardInput,
        assetController = BackgroundAssetController.preview(),
        listener = ShareClipPageListener.Preview,
        onNavigationButtonClick = {},
        onCloseClick = {},
        onEditClick = {},
        state = rememberClipPageState(
            firstVisibleItemIndex = 0,
        ),
    )
}
