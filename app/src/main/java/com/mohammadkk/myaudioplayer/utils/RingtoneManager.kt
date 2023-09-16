package com.mohammadkk.myaudioplayer.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.extensions.toContentUri
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.models.Song

object RingtoneManager {
    fun setRingtone(context: Context, song: Song) {
        val uri = song.id.toContentUri()
        val resolver = context.contentResolver
        try {
            val cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns.TITLE),
                "${BaseColumns._ID} = ?",
                arrayOf(song.id.toString()), null
            )
            cursor.use { cursorSong ->
                if (cursorSong != null && cursorSong.count == 1) {
                    cursorSong.moveToFirst()
                    Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString())
                    val message = context.getString(R.string.set_as_ringtone_symbol, cursorSong.getString(0))
                    context.toast(message)
                }
            }
        } catch (ignored: SecurityException) {
        }
    }
    fun requiresDialog(context: Context): Boolean {
        if (Constant.isMarshmallowPlus()) {
            if (!Settings.System.canWrite(context)) {
                return true
            }
        }
        return false
    }
    @TargetApi(23)
    fun showDialog(context: Context) {
        var message = context.getString(R.string.set_ringtone_message)
        message = message.replace("My Audio Player", "<b>My Audio Player</b>")
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.set_ringtone)
            .setMessage(message.parseAsHtml())
            .setPositiveButton(android.R.string.ok) { d, _ ->
                d.dismiss()
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                val appId = context.packageName ?: context.applicationContext.packageName
                intent.data = ("package:$appId").toUri()
                context.startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}