package com.mohammadkk.myaudioplayer.glide

import android.content.Context
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

class MediaCoverLoader(private val context: Context) : ModelLoader<MediaCover, InputStream> {
    override fun buildLoadData(
        model: MediaCover,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> {
        return ModelLoader.LoadData(
            ObjectKey(createStringKey(model)),
            MediaCoverFetcher(context, model)
        )
    }
    private fun createStringKey(model: MediaCover): String {
        return if (model.path.startsWith("content://")) {
            "${model.path}_${model.dateModified}"
        } else {
            "${model.trackId}_${model.dateModified}"
        }
    }
    override fun handles(model: MediaCover): Boolean {
        return true
    }
    class Factory(private val context: Context) : ModelLoaderFactory<MediaCover, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<MediaCover, InputStream> {
            return MediaCoverLoader(context)
        }
        override fun teardown() {
            //empty teardown
        }
    }
}