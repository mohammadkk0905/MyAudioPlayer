package com.mohammadkk.myaudioplayer.utils

import android.content.Context
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.extensions.getIntVal
import com.mohammadkk.myaudioplayer.extensions.getLongVal
import com.mohammadkk.myaudioplayer.extensions.getStringVal
import com.mohammadkk.myaudioplayer.models.Song
import java.text.Collator
import kotlin.math.abs

object Libraries {
    private const val IS_MUSIC = "${Audio.AudioColumns.IS_MUSIC} = 1 AND ${Audio.AudioColumns.DURATION} >= 5000"

    fun fetchAllSongs(context: Context, selection: String?, selectionArgs: Array<String>?): List<Song> {
        val songs = arrayListOf<Song>()
        val uri = if (Constant.isQPlus()) {
            Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            Audio.AudioColumns._ID,
            Audio.AudioColumns.ALBUM_ID,
            Audio.AudioColumns.ARTIST_ID,
            Audio.AudioColumns.TITLE,
            Audio.AudioColumns.ALBUM,
            Audio.AudioColumns.ARTIST,
            Audio.AudioColumns.DATA,
            Audio.AudioColumns.YEAR,
            Audio.AudioColumns.DURATION,
            Audio.AudioColumns.DATE_ADDED
        )
        val ms = if (selection != null && selection.trim { it <= ' ' } != "") {
            "$IS_MUSIC AND $selection"
        } else {
            IS_MUSIC
        }
        val sortOrder = Audio.Media.DEFAULT_SORT_ORDER
        try {
            val cursor = context.contentResolver.query(uri, projection, ms, selectionArgs, sortOrder)
            cursor?.use {
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getLongVal(Audio.AudioColumns._ID)
                        val albumId = cursor.getLongVal(Audio.AudioColumns.ALBUM_ID)
                        val artistId = cursor.getLongVal(Audio.AudioColumns.ARTIST_ID)
                        val title = cursor.getStringVal(Audio.AudioColumns.TITLE, MediaStore.UNKNOWN_STRING)
                        val album = cursor.getStringVal(Audio.AudioColumns.ALBUM, MediaStore.UNKNOWN_STRING)
                        val artist = cursor.getStringVal(Audio.AudioColumns.ARTIST, MediaStore.UNKNOWN_STRING)
                        val path = cursor.getStringVal(Audio.AudioColumns.DATA)
                        val year = cursor.getIntVal(Audio.AudioColumns.YEAR)
                        val duration = cursor.getIntVal(Audio.AudioColumns.DURATION)
                        val dateAdded = cursor.getIntVal(Audio.AudioColumns.DATE_ADDED)
                        songs.add(Song(id, albumId, artistId, title, album, artist, path, year, duration, dateAdded))
                    } while (cursor.moveToNext())
                }
            }
        } catch (ignored: Exception) {
        }
        return songs
    }
    fun getSortedSongs(songs: List<Song>?): List<Song> {
        if (songs.isNullOrEmpty()) return emptyList()
        val sortOrder = BaseSettings.getInstance().songsSorting
        val collator = Collator.getInstance()
        val comparator = Comparator<Song> { o1, o2 ->
            var result = when (abs(sortOrder)) {
                Constant.SORT_BY_TITLE -> collator.compare(o1.title, o2.title)
                Constant.SORT_BY_ALBUM -> collator.compare(o1.album, o2.album)
                Constant.SORT_BY_ARTIST-> collator.compare(o1.artist, o2.artist)
                Constant.SORT_BY_DURATION -> o2.duration.compareTo(o1.duration)
                Constant.SORT_BY_DATE_ADDED -> o2.dateAdded.compareTo(o1.dateAdded)
                else -> return@Comparator 0
            }
            if (sortOrder < 0) result *= -1
            return@Comparator result
        }
        return songs.sortedWith(comparator)
    }
    fun fetchSongsByAlbumId(context: Context, id: Long): List<Song> {
        val selection = "${Audio.AudioColumns.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return fetchAllSongs(context, selection, selectionArgs)
    }
    fun fetchSongsByArtistId(context: Context, id: Long): List<Song> {
        val selection = "${Audio.AudioColumns.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return fetchAllSongs(context, selection, selectionArgs)
    }
    fun getSectionName(mediaTitle: String?, stripPrefix: Boolean = false): String {
        var mMediaTitle = mediaTitle
        return try {
            if (mMediaTitle.isNullOrEmpty()) return "-"
            mMediaTitle = mMediaTitle.trim { it <= ' ' }.lowercase()
            if (stripPrefix) {
                if (mMediaTitle.startsWith("the ")) {
                    mMediaTitle = mMediaTitle.substring(4)
                } else if (mMediaTitle.startsWith("a ")) {
                    mMediaTitle = mMediaTitle.substring(2)
                }
            }
            mMediaTitle.firstOrNull()?.uppercase() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}