package com.mohammadkk.myaudioplayer.activities

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.dialogs.DeleteSongsDialog
import com.mohammadkk.myaudioplayer.extensions.errorToast
import com.mohammadkk.myaudioplayer.extensions.toContentUri
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.utils.FileUtils
import java.io.File

abstract class BaseActivity : AppCompatActivity() {
    protected val settings: BaseSettings get() = BaseSettings.getInstance()
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
            if (songs.first().isOTGMode()) {
                Constant.ensureBackgroundThread {
                    var result = false
                    for (song in songs) {
                        result = try {
                            FileUtils.deleteSingle(this, song.path.toUri())
                        } catch (e: Exception) {
                            false
                        }
                    }
                    if (result) runOnUiThread { callback() }
                }
                return
            }
            if (Constant.isRPlus()) {
                val uris = songs.map { it.id.toContentUri() }
                deleteSDK30Uris(uris) { success ->
                    if (success) {
                        callback()
                    } else {
                        DeleteSongsDialog.destroyDataset()
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
    protected fun isOTGRootFolder(uri: Uri): Boolean {
        return isExternalStorageDocument(uri) && isRootUri(uri) && !isInternalStorage(uri)
    }
    private fun isRootUri(uri: Uri): Boolean {
        return uri.lastPathSegment?.endsWith(":") ?: false
    }
    private fun isInternalStorage(uri: Uri): Boolean {
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        return isExternalStorageDocument(uri) && documentId.contains("primary")
    }
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return Constant.EXTERNAL_STORAGE_AUTHORITY == uri.authority
    }
    open fun onBindService() {
    }
    companion object {
        private var mAfterSdk30Action: ((success: Boolean) -> Unit)? = null
    }
}