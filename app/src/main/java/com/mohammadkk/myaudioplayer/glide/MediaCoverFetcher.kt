package com.mohammadkk.myaudioplayer.glide

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.mohammadkk.myaudioplayer.extensions.toContentUri
import com.mohammadkk.myaudioplayer.utils.FileUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class MediaCoverFetcher(private val context: Context, private val cover: MediaCover) : DataFetcher<InputStream> {
    private var mStream: InputStream? = null
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val retriever = MediaMetadataRetriever()
        try {
            if (cover.path.startsWith("content://")) {
                retriever.setDataSource(context, cover.path.toUri())
            } else {
                retriever.setDataSource(context, cover.trackId.toContentUri())
            }
            val picture = retriever.embeddedPicture
            mStream = if (picture != null) {
                ByteArrayInputStream(picture)
            } else {
                FileUtils.fallback(cover.path)
            }
            callback.onDataReady(mStream)
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        } finally {
            retriever.release()
        }
    }
    override fun cleanup() {
        if (mStream != null) {
            try {
                mStream?.close()
            } catch (ignored: IOException) {
                //can't do much about it
            }
        }
    }
    override fun cancel() {
        //empty cancel
    }
    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }
    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}