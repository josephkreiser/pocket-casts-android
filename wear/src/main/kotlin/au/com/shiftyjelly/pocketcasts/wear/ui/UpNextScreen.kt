package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object UpNextScreen {
    const val ROUTE = "up_next_screen"
}

@Composable
fun UpNextScreen(
    columnState: ScalingLazyColumnState,
    navigateToEpisode: (episodeUuid: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpNextViewModel = hiltViewModel(),
) {
    val queueState by viewModel.upNextQueue.subscribeAsState(initial = null)
    val artworkConfiguration by viewModel.artworkConfiguration.collectAsState()

    when (queueState) {
        null -> { /* Show nothing while loading */ }

        UpNextQueue.State.Empty -> EmptyQueueState()

        is UpNextQueue.State.Loaded -> {
            val list = (queueState as UpNextQueue.State.Loaded).queue
            if (list.isEmpty()) {
                EmptyQueueState()
            } else {
                Scaffold(
                    positionIndicator = { PositionIndicator(scalingLazyListState = columnState.state) },
                    modifier = modifier,
                ) {
                    ScalingLazyColumn(
                        columnState = columnState,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        item { ScreenHeaderChip(LR.string.up_next) }

                        items(list) { episode ->
                            EpisodeChip(
                                episode = episode,
                                useEpisodeArtwork = artworkConfiguration.useEpisodeArtwork,
                                useUpNextIcon = false,
                                onClick = {
                                    navigateToEpisode(episode.uuid)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyQueueState() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(LR.string.player_up_next_empty),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.title2,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(LR.string.player_up_next_empty_desc_watch),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.body1,
        )
    }
}
