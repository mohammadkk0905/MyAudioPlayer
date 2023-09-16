package com.mohammadkk.myaudioplayer.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.services.PlaybackStateManager

class PlaybackViewModel : ViewModel(), PlaybackStateManager.Callback {
    private val mSong = MutableLiveData<Song?>()
    private val mIsPlaying = MutableLiveData(false)
    private val mIsPermission = MutableLiveData(true)
    private val mPosition = MutableLiveData(0)

    val song: LiveData<Song?> get() = mSong
    val isPlaying: LiveData<Boolean> get() = mIsPlaying
    val isPermission: LiveData<Boolean> get() = mIsPermission
    val position: LiveData<Int> get() = mPosition

    private val playbackManager = PlaybackStateManager.getInstance()

    init {
        playbackManager.addCallback(this)
    }

    override fun onNoStoragePermission() {
        mIsPermission.value = false
    }
    override fun onProgressUpdated(progress: Int) {
        mPosition.postValue(progress)
    }
    override fun onSongChanged(song: Song?) {
        mSong.value = song
    }
    override fun onSongStateChanged(isPlaying: Boolean) {
        mIsPlaying.value = isPlaying
    }
    override fun onCleared() {
        playbackManager.removeCallback(this)
        super.onCleared()
    }
}