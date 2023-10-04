package com.mohammadkk.myaudioplayer.glide

class MediaCover(
    val trackId: Long,
    val path: String,
    val dateModified: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaCover

        if (trackId != other.trackId) return false
        if (path != other.path) return false
        if (dateModified != other.dateModified) return false

        return true
    }
    override fun hashCode(): Int {
        var result = trackId.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + dateModified.hashCode()
        return result
    }
    override fun toString(): String {
        return "MediaCover(trackId=$trackId, path='$path', dateModified=$dateModified)"
    }
}