package com.mohammadkk.myaudioplayer.activities

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.extensions.errorToast
import com.mohammadkk.myaudioplayer.extensions.toContentUri
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.models.Song
import java.io.File

abstract class BaseActivity : AppCompatActivity() {
    private var mLaunchActivity: ActivityResultLauncher<IntentSenderRequest>? = null
    internal var isFadeAnimation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLaunchActivity = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            mAfterSdk30Action?.invoke(it.resultCode == Activity.RESULT_OK)
            mAfterSdk30Action = null
        }
    }
    private fun deleteSDK30Uris(uris: List<Uri>, callback: (success: Boolean) -> Unit) {
        if (Constant.isRPlus()) {
            mAfterSdk30Action = callback
            try {
                val deleteRequest = MediaStore.createDeleteRequest(contentResolver, uris)
                mLaunchActivity?.launch(IntentSenderRequest.Builder(deleteRequest.intentSender).build())
            } catch (e: Exception) {
                errorToast(e)
            }
        } else {
            callback(false)
        }
    }
    fun deleteSongs(songs: List<Song>, callback: () -> Unit) {
        if (songs.isNotEmpty()) {
            if (Constant.isRPlus()) {
                val uris = songs.map { it.id.toContentUri() }
                deleteSDK30Uris(uris) { success ->
                    if (success) {
                        callback()
                    } else {
                        toast(R.string.unknown_error_occurred)
                    }
                }
            } else {
                val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                songs.forEach { song ->
                    try {
                        val where = "${MediaStore.Audio.Media._ID} = ?"
                        val args = arrayOf(song.id.toString())
                        contentResolver.delete(contentUri, where, args)
                        File(song.path).delete()
                    } catch (ignored: Exception) {
                    }
                }
                callback()
            }
        }
    }
    open fun onReloadLibrary() {
    }
    companion object {
        private var mAfterSdk30Action: ((success: Boolean) -> Unit)? = null
    }
}