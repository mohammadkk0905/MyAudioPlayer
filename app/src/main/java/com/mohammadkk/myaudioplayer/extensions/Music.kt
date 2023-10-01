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
import com.mohammadkk.myaudioplayer.models.FileItem
import com.mohammadkk.myaudioplayer.models.Song
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toContentUri(): Uri = ContentUris.withAppendedId(
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
    this
)
fun Long.toAlbumArtURI(): Uri = ContentUris.withAppendedId(
    "content://media/external/audio/albumart".toUri(),
    this
)
fun Int.toFormattedDuration(isSeekBar: Boolean): String {
    var mSeconds = this / 1000
    var hours = 0
    var minutes = 0
    if (mSeconds >= 3600) {
        hours = mSeconds / 3600
        mSeconds -= hours * 3600
    }
    if (mSeconds >= 60) {
        minutes = mSeconds / 60
        mSeconds -= minutes * 60
    }
    val seconds = mSeconds

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        val defFormat = if (!isSeekBar) "%02dm:%02ds" else "%02d:%02d"
        String.format(Locale.getDefault(), defFormat, minutes, seconds)
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
    return if (isOTGMode() || path.startsWith("content://")) {
        Uri.parse(path)
    } else {
        try {
            val file = File(path)
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
        mmr.setDataSource(context, requireContentUri())
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
fun FileItem.parseSong(id: Int = 0): Song? {
    val title = filename?.substringBeforeLast('.', "")
    val album = "Unknown album"
    val artist = "Unknown artist"
    if (title == null || title == "") return null
    val path = contentUri.toString()
    val dateAdded = modified.toInt()
    return Song(id.toLong(), 0, 0, title, album, artist, path, 0, 0, dateAdded, modified)
}