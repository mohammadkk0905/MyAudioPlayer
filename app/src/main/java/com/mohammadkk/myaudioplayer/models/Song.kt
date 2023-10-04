package com.mohammadkk.myaudioplayer.models

import android.net.Uri
import androidx.core.net.toUri
import com.mohammadkk.myaudioplayer.extensions.toAlbumArtURI
import com.mohammadkk.myaudioplayer.extensions.toContentUri

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
    fun isOTGMode(): Boolean {
        return albumId == 0L || artistId == 0L
    }
    fun requireContentUri(): Uri {
        var uri = id.toContentUri()
        if (isOTGMode()) uri = path.toUri()
        return uri
    }
    fun requireArtworkUri(): Uri {
        var uri = albumId.toAlbumArtURI()
        if (isOTGMode()) uri = path.toUri()
        return uri
    }
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