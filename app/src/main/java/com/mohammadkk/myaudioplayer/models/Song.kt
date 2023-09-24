package com.mohammadkk.myaudioplayer.models

data class Song(
    val id: Long,
    val albumId: Long,
    val artistId: Long,
    val title: String,
    val album: String,
    val artist: String,
    val path: String,
    val year: Int,
    val duration: Int,
    val dateAdded: Int,
    val dateModified: Long
) {
    companion object {
        val emptySong = Song(
            id = -1,
            albumId = -1,
            artistId = -1,
            title = "",
            album = "",
            artist = "",
            path = "",
            year = -1,
            duration = -1,
            dateAdded = -1,
            dateModified = -1
        )
    }
}