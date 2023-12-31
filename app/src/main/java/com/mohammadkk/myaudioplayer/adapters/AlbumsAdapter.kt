package com.mohammadkk.myaudioplayer.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.activities.TracksActivity
import com.mohammadkk.myaudioplayer.databinding.ItemAlbumsBinding
import com.mohammadkk.myaudioplayer.extensions.getDrawableCompat
import com.mohammadkk.myaudioplayer.extensions.notifyOnDataChanged
import com.mohammadkk.myaudioplayer.extensions.toAlbumArtURI
import com.mohammadkk.myaudioplayer.extensions.toFormattedDuration
import com.mohammadkk.myaudioplayer.extensions.toLocaleYear
import com.mohammadkk.myaudioplayer.glide.CoverMode
import com.mohammadkk.myaudioplayer.glide.MediaCover
import com.mohammadkk.myaudioplayer.models.Album
import com.mohammadkk.myaudioplayer.utils.Libraries
import me.zhanghai.android.fastscroll.PopupTextProvider
import kotlin.math.abs

class AlbumsAdapter(
    private val context: FragmentActivity,
    var dataSet: MutableList<Album>
) : RecyclerView.Adapter<AlbumsAdapter.AlbumHolder>(), PopupTextProvider {
    val settings = BaseSettings.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        val inflater = LayoutInflater.from(parent.context)
        return AlbumHolder(ItemAlbumsBinding.inflate(inflater, parent, false))
    }
    override fun getItemCount(): Int {
        return dataSet.size
    }
    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        holder.bindItems(dataSet[holder.absoluteAdapterPosition])
    }
    override fun getPopupText(view: View, position: Int): CharSequence {
        val album = dataSet.getOrNull(position)
        val result = when (abs(settings.albumsSorting)) {
            Constant.SORT_BY_TITLE -> album?.title
            Constant.SORT_BY_ARTIST -> album?.artist
            Constant.SORT_BY_YEAR -> return album?.year?.toLocaleYear() ?: "-"
            Constant.SORT_BY_DURATION -> return album?.duration?.toFormattedDuration(true) ?: "-"
            else -> album?.title
        }
        return Libraries.getSectionName(result, true)
    }
    fun swapDataSet(dataSet: List<Album>) {
        this.dataSet = ArrayList(dataSet)
        notifyOnDataChanged()
    }
    private fun startTracks(album: Album) {
        Intent(context, TracksActivity::class.java).apply {
            putExtra(Constant.ALBUM_ID, album.id)
            context.startActivity(this)
            context.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }
    private fun loadCover(imageView: ImageView, album: Album) {
        val default = context.getDrawableCompat(R.drawable.ic_album)
        when (val mode = settings.coverMode) {
            CoverMode.OFF -> imageView.setImageDrawable(default)
            else -> {
                val cover: Any = if (mode == CoverMode.MEDIA_STORE) {
                    album.id.toAlbumArtURI()
                } else {
                    val song = album.getSafeSong()
                    MediaCover(song.id, song.path, song.dateModified)
                }
                Glide.with(context)
                    .load(cover)
                    .apply(
                        RequestOptions()
                            .placeholder(default)
                            .error(default)
                            .transform(CenterCrop())
                    )
                    .into(imageView)
            }
        }
    }
    inner class AlbumHolder(private val binding: ItemAlbumsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(album: Album) {
            with(binding) {
                tvNameItem.text = album.title
                tvTrackCountItem.text = context.resources.getQuantityString(
                    R.plurals.songs_plural,
                    album.trackCount, album.trackCount
                )
                loadCover(imgCoverItem, album)
                root.setOnClickListener {
                    if (!Constant.isBlockingClick()) {
                        startTracks(album)
                    }
                }
            }
        }
    }
}