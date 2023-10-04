package com.mohammadkk.myaudioplayer.models

data class Artist(
    val id: Long,
    var duration: Int,
    var albumCount: Int,
    val songs: MutableList<Song>
) {
    val albumId: Long get() = getSafeSong().albumId
    val title: String get() = getSafeSong().artist
    val trackCount: Int get() = songs.size

    fun getSafeSong(): Song {
        return songs.firstOrNull() ?: Song.emptySong
    }
}