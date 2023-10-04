package com.mohammadkk.myaudioplayer.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.listeners.FragmentLibraries
import com.mohammadkk.myaudioplayer.models.Album
import com.mohammadkk.myaudioplayer.models.Artist
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.utils.Libraries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import kotlin.math.abs

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Application get() = getApplication()
    private val settings = BaseSettings.getInstance()
    val fragmentLibraries = hashMapOf<Int, FragmentLibraries?>()

    private val songsList = MutableLiveData<List<Song>?>()
    private val albumsList = MutableLiveData<List<Album>?>()
    private val artistsList = MutableLiveData<List<Artist>?>()

    fun getSongsList(): LiveData<List<Song>?> = songsList
    fun getAlbumsList(): LiveData<List<Album>?> = albumsList
    fun getArtistsList(): LiveData<List<Artist>?> = artistsList

    fun updateLibraries() = viewModelScope.launch(IO) {
        val songs = getAllSongs(true)
        val albums = getAllAlbums(songs)
        val artist = getAllArtists(songs)
        withContext(Dispatchers.Main) {
            songsList.value = songs
            albumsList.value = albums
            artistsList.value = artist
        }
    }
    private suspend fun fetchSongs() {
        val songs = getAllSongs(true)
        withContext(Dispatchers.Main) {
            songsList.value = songs
        }
    }
    private suspend fun fetchAlbums() {
        val albums = getAllAlbums(getAllSongs(false))
        withContext(Dispatchers.Main) {
            albumsList.value = albums
        }
    }
    private suspend fun fetchArtists() {
        val artists = getAllArtists(getAllSongs(false))
        withContext(Dispatchers.Main) {
            artistsList.value = artists
        }
    }
    private fun getAllSongs(isSorting: Boolean): List<Song> {
        val songs = Libraries.fetchAllSongs(context, null, null)
        if (!isSorting) return songs
        return Libraries.getSortedSongs(songs)
    }
    private fun getAllAlbums(items: List<Song>): List<Album> {
        val mItems = splitsIntoAlbums(items)
        if (mItems.isEmpty()) return mItems
        val sortOrder = settings.albumsSorting
        val collator = Collator.getInstance()
        val comparator = Comparator<Album> { o1, o2 ->
            var result = when (abs(sortOrder)) {
                Constant.SORT_BY_TITLE -> collator.compare(o1.title, o2.title)
                Constant.SORT_BY_ARTIST -> collator.compare(o1.artist, o2.artist)
                Constant.SORT_BY_YEAR -> o2.year.compareTo(o1.year)
                Constant.SORT_BY_DURATION -> o2.duration.compareTo(o1.duration)
                Constant.SORT_BY_SONGS -> o2.trackCount.compareTo(o1.trackCount)
                else -> return@Comparator 0
            }
            if (sortOrder < 0) result *= -1
            return@Comparator result
        }
        return mItems.sortedWith(comparator)
    }
    private fun getAllArtists(items: List<Song>): List<Artist> {
        val mItems = splitsIntoArtists(items)
        if (mItems.isEmpty()) return mItems
        val sortOrder = settings.artistsSorting
        val collator = Collator.getInstance()
        val comparator = Comparator<Artist> { o1, o2 ->
            var result = when (abs(sortOrder)) {
                Constant.SORT_BY_TITLE -> collator.compare(o1.title, o2.title)
                Constant.SORT_BY_DURATION -> o2.duration.compareTo(o1.duration)
                Constant.SORT_BY_SONGS -> o2.trackCount.compareTo(o1.trackCount)
                else -> return@Comparator 0
            }
            if (sortOrder < 0) result *= -1
            return@Comparator result
        }
        return mItems.sortedWith(comparator)
    }
    private fun splitsIntoAlbums(items: List<Song>): List<Album> {
        val itemsMap = linkedMapOf<Long, Album>()
        val albums = arrayListOf<Album>()
        items.forEach { song ->
            val album: Album
            if (itemsMap.containsKey(song.albumId)) {
                album = itemsMap[song.albumId]!!
                album.duration += song.duration
                album.songs.add(song)
            } else {
                val list = mutableListOf<Song>()
                list.add(song)
                album = Album(song.albumId, song.duration, list)
                albums.add(album)
            }
            itemsMap[song.albumId] = album
        }
        return albums
    }
    private fun splitsIntoArtists(items: List<Song>): List<Artist> {
        val itemsMap = linkedMapOf<Long, Artist>()
        for (song in items) {
            if (itemsMap.containsKey(song.artistId)) {
                val artist = itemsMap[song.artistId]!!
                artist.duration += song.duration
                artist.songs.add(song)
                itemsMap[song.artistId] = artist
            } else {
                val list = mutableListOf<Song>()
                list.add(song)
                val artist = Artist(song.artistId, song.duration, 0, list)
                itemsMap[song.artistId] = artist
            }
        }
        return itemsMap.values.map { artist ->
            artist.albumCount = Libraries.getAlbumCount(artist.songs)
            artist
        }
    }
    fun forceReload(mode: Int) = viewModelScope.launch(IO) {
        when (mode) {
            0 -> fetchSongs()
            1 -> fetchAlbums()
            2 -> fetchArtists()
        }
    }
}