package com.mohammadkk.myaudioplayer.glide.loader

import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.utils.FileUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class AudioFileCoverFetcher(private val model: AudioFileCover) : DataFetcher<InputStream> {
    private var stream: InputStream? = null

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val settings = BaseSettings.getInstance()
        val retriever = MediaMetadataRetriever()
        try {
            if (model.filePath.startsWith("content://")) {
                retriever.setDataSource(
                    settings.getContext(),
                    model.filePath.toUri()
                )
            } else {
                retriever.setDataSource(model.filePath)
            }
            val picture = retriever.embeddedPicture
            stream = if (picture != null) {
                ByteArrayInputStream(picture)
            } else {
                FileUtils.fallback(model.filePath)
            }
            callback.onDataReady(stream)
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        } finally {
            retriever.release()
        }
    }
    override fun cleanup() {
        if (stream != null) {
            try {
                stream?.close()
            } catch (ignore: IOException) {
            }
        }
    }
    override fun cancel() {
        // cannot cancel
    }
    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }
    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}