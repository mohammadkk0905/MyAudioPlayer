package com.mohammadkk.myaudioplayer.glide.loader

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

class AudioFileCoverLoader : ModelLoader<AudioFileCover, InputStream> {
    override fun buildLoadData(
        model: AudioFileCover,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> {
        return ModelLoader.LoadData(
            ObjectKey(model.filePath),
            AudioFileCoverFetcher(model)
        )
    }
    override fun handles(model: AudioFileCover): Boolean {
        return true
    }
    class Factory : ModelLoaderFactory<AudioFileCover, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AudioFileCover, InputStream> {
            return AudioFileCoverLoader()
        }
        override fun teardown() {
        }
    }
}