package com.mohammadkk.myaudioplayer.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.mohammadkk.myaudioplayer.glide.loader.AudioFileCover
import com.mohammadkk.myaudioplayer.glide.loader.AudioFileCoverLoader
import java.io.InputStream

@GlideModule
class MyMusicGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        registry.prepend(
            AudioFileCover::class.java,
            InputStream::class.java,
            AudioFileCoverLoader.Factory()
        )
    }
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}