package com.mohammadkk.myaudioplayer.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.utils.Libraries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubViewModel : ViewModel() {
    private val liveData = MutableLiveData<List<Song>>()

    fun getListData(): LiveData<List<Song>> = liveData

    fun updateList(list: List<Song>) {
        liveData.value = list
    }
    fun handleOTG(context: Context) {
        viewModelScope.launch(IO) {
            val libraries = Libraries.fetchSongsByOtg(context)
            withContext(Dispatchers.Main) {
                liveData.value = libraries
            }
        }
    }
}