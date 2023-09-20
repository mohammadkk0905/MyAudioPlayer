package com.mohammadkk.myaudioplayer.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mohammadkk.myaudioplayer.R
enum class PlaybackRepeat(
    @DrawableRes val iconRes: Int,
    @StringRes val descriptionRes: Int
) {
    REPEAT_OFF(
        iconRes = R.drawable.ic_repeat,
        descriptionRes = R.string.repeat_off
    ),
    REPEAT_PLAYLIST(
        iconRes = R.drawable.ic_repeat,
        descriptionRes = R.string.repeat_all
    ),
    REPEAT_SONG(
        iconRes = R.drawable.ic_repeat_one,
        descriptionRes = R.string.repeat_song
    ),
    STOP_AFTER_CURRENT_SONG(
        iconRes = R.drawable.ic_play_one_song,
        descriptionRes = R.string.stop_playback_after_curr_song
    );

    val nextPlayBackRepeat: PlaybackRepeat
        get() = when (this) {
            REPEAT_OFF -> REPEAT_PLAYLIST
            REPEAT_PLAYLIST -> REPEAT_SONG
            REPEAT_SONG -> STOP_AFTER_CURRENT_SONG
            STOP_AFTER_CURRENT_SONG -> REPEAT_OFF
        }
}