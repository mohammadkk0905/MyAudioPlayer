package com.mohammadkk.myaudioplayer.extensions

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.models.Song
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SECOND_MILLIS = 1000
private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS

fun Long.toContentUri(): Uri = ContentUris.withAppendedId(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    this
)
fun Long.toAlbumArtURI(): Uri = ContentUris.withAppendedId(
    "content://media/external/audio/albumart".toUri(),
    this
)
fun Int.toFormattedDuration(isSeekBar: Boolean): String {
    val seconds = this / 1000
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (seconds >= 3600) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        val defFormat = if (!isSeekBar) "%02dm:%02ds" else "%02d:%02d"
        String.format(Locale.getDefault(), defFormat, m, s)
    }
}
fun Int.toFormattedDate(): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val mDate = Date(this * 1000L)
        sdf.format(mDate)
    } catch (e: Exception) {
        ""
    }
}
fun Int.toLocaleYear(): String {
    return if (this > 0) {
        String.format(Locale.getDefault(), "%d", this)
    } else "-"
}
fun Song.toProviderUri(context: Context): Uri {
    val uri = Uri.parse(path)
    return if (uri.scheme == "content") {
        uri
    } else {
        val newPath = if (uri.toString().startsWith('/')) uri.toString() else uri.path
        val file = File(newPath ?: return id.toContentUri())
        return try {
            if (Constant.isNougatPlus()) {
                val applicationId = context.packageName ?: context.applicationContext.packageName
                FileProvider.getUriForFile(context, "$applicationId.provider", file)
            } else file.toUri()
        } catch (e: IllegalArgumentException) {
            id.toContentUri()
        }
    }
}
fun Song.getTrackArt(context: Context): Bitmap? {
    val mmr = MediaMetadataRetriever()
    return try {
        mmr.setDataSource(context, id.toContentUri())
        val art = mmr.embeddedPicture
        mmr.release()
        if (art == null) return null
        BitmapFactory.decodeByteArray(art, 0, art.size, BitmapFactory.Options())
    } catch (e: Exception) {
        null
    }
}
fun Song.getAlbumArt(context: Context): Bitmap? {
    return try {
        val fd = context.contentResolver.openFileDescriptor(albumId.toAlbumArtURI(), "r") ?: return null
        val bitmap = BitmapFactory.decodeFileDescriptor(fd.fileDescriptor)
        fd.close()
        bitmap
    } catch (e: Exception) {
        null
    }
}