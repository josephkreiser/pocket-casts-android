package au.com.shiftyjelly.pocketcasts.settings.privacy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.text.LinkText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.startActivityViewUrl
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PrivacyFragment : BaseFragment() {

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var settings: Settings
    private val viewModel: PrivacyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        LaunchedEffect(Unit) {
            if (!viewModel.isFragmentChangingConfigurations) {
                analyticsTracker.track(AnalyticsEvent.PRIVACY_SHOWN)
            }
        }
        val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
        val context = LocalContext.current
        AppThemeWithBackground(theme.activeTheme) {
            val state: PrivacyViewModel.UiState by viewModel.uiState.collectAsState()
            PrivacySettings(
                state = state,
                onAnalyticsClick = {
                    viewModel.updateAnalyticsSetting(it)
                },
                onAnalyticsThirdPartyClick = {
                    viewModel.updateAnalyticsThirdPartySetting(it)
                },
                onCrashReportsClick = {
                    viewModel.updateCrashReportsSetting(it)
                },
                onLinkAccountClick = {
                    viewModel.updateLinkAccountSetting(it)
                },
                onPrivacyPolicyClick = {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_SHOW_PRIVACY_POLICY)
                    context.startActivityViewUrl(Settings.INFO_PRIVACY_URL)
                },
                onBackPress = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
            )
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.isFragmentChangingConfigurations = activity?.isChangingConfigurations ?: false
    }

    @Composable
    private fun PrivacySettings(
        state: PrivacyViewModel.UiState,
        onAnalyticsClick: (Boolean) -> Unit,
        onAnalyticsThirdPartyClick: (Boolean) -> Unit,
        onCrashReportsClick: (Boolean) -> Unit,
        onLinkAccountClick: (Boolean) -> Unit,
        onPrivacyPolicyClick: () -> Unit,
        onBackPress: () -> Unit,
        bottomInset: Dp,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxHeight(),
        ) {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_title_privacy),
                onNavigationClick = onBackPress,
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomInset),
            ) {
                item {
                    TextP50(
                        text = stringResource(LR.string.settings_privacy_summary),
                        color = MaterialTheme.theme.colors.primaryText02,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                if (state is PrivacyViewModel.UiState.Loaded) {
                    item {
                        val primaryText = if (FeatureFlag.isEnabled(Feature.APPSFLYER_ANALYTICS)) LR.string.settings_privacy_analytics_first_party else LR.string.settings_privacy_analytics
                        SettingRow(
                            primaryText = stringResource(primaryText),
                            secondaryText = stringResource(LR.string.settings_privacy_analytics_summary),
                            toggle = SettingRowToggle.Switch(checked = state.analytics),
                            modifier = Modifier.toggleable(
                                value = state.analytics,
                                role = Role.Switch,
                            ) { onAnalyticsClick(!state.analytics) },
                            indent = false,
                        )
                    }
                    if (FeatureFlag.isEnabled(Feature.APPSFLYER_ANALYTICS)) {
                        item {
                            SettingRow(
                                primaryText = stringResource(LR.string.settings_privacy_analytics_third_party),
                                secondaryText = stringResource(LR.string.settings_privacy_analytics_third_party_summary),
                                toggle = SettingRowToggle.Switch(checked = state.analyticsThirdParty),
                                modifier = Modifier.toggleable(
                                    value = state.analyticsThirdParty,
                                    role = Role.Switch,
                                ) { onAnalyticsThirdPartyClick(!state.analyticsThirdParty) },
                                indent = false,
                            )
                        }
                    }
                    item {
                        SettingRow(
                            primaryText = stringResource(LR.string.settings_privacy_crash),
                            secondaryText = stringResource(LR.string.settings_privacy_crash_summary),
                            toggle = SettingRowToggle.Switch(checked = state.crashReports),
                            modifier = Modifier.toggleable(
                                value = state.crashReports,
                                role = Role.Switch,
                            ) { onCrashReportsClick(!state.crashReports) },
                            indent = false,
                        )
                    }
                    if (state.shouldShowLinkUserSetting()) {
                        item {
                            SettingRow(
                                primaryText = stringResource(LR.string.settings_privacy_crash_link),
                                secondaryText = stringResource(LR.string.settings_privacy_crash_link_summary),
                                toggle = SettingRowToggle.Switch(checked = state.linkAccount),
                                modifier = Modifier.toggleable(
                                    value = state.linkAccount,
                                    role = Role.Switch,
                                ) { onLinkAccountClick(!state.linkAccount) },
                                indent = false,
                            )
                        }
                    }
                    item {
                        LinkText(
                            text = stringResource(LR.string.profile_privacy_policy_read),
                            textAlign = TextAlign.Start,
                            onClick = onPrivacyPolicyClick,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
