package com.nicholasbergesen.gunsout.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.CurrentUserIdProvider
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    currentUserIdProvider: CurrentUserIdProvider,
    userPrefs: UserPreferences
) : ViewModel() {
    val themeStyle: StateFlow<ThemeStyle> = currentUserIdProvider.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(ThemeStyle.Default)
            } else {
                userPrefs.profile(userId).map { it.themeStyle }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeStyle.Default)
}
