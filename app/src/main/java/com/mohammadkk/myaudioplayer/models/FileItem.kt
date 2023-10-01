package com.mohammadkk.myaudioplayer.models

import android.net.Uri

data class FileItem(
    val filename: String?,
    val isDirectory: Boolean,
    val modified: Long,
    val contentUri: Uri
)