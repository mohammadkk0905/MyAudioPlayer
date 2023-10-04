package com.mohammadkk.myaudioplayer.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.mohammadkk.myaudioplayer.BuildConfig
import java.io.InputStream

@GlideModule
class AudioGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        var requestOptions = RequestOptions()
        requestOptions = requestOptions.diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        builder.setDefaultRequestOptions(requestOptions)
        if (BuildConfig.DEBUG) {
            builder.setLogLevel(Log.WARN)
        } else builder.setLogLevel(Log.ERROR)
    }
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(
            MediaCover::class.java,
            InputStream::class.java,
            MediaCoverLoader.Factory(context.applicationContext)
        )
    }
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}