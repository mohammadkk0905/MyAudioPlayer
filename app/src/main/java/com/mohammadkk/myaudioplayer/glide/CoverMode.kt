package com.mohammadkk.myaudioplayer.glide

enum class CoverMode {
    OFF, MEDIA_STORE, QUALITY;

    companion object {
        fun getCoverMode(mode: Int): CoverMode {
            return values().getOrNull(mode) ?: MEDIA_STORE
        }
    }
}