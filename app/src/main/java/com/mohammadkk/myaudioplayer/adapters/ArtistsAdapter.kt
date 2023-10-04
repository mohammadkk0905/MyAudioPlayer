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
import com.mohammadkk.myaudioplayer.databinding.ItemArtistsBinding
import com.mohammadkk.myaudioplayer.extensions.getDrawableCompat
import com.mohammadkk.myaudioplayer.extensions.notifyOnDataChanged
import com.mohammadkk.myaudioplayer.extensions.toAlbumArtURI
import com.mohammadkk.myaudioplayer.extensions.toFormattedDuration
import com.mohammadkk.myaudioplayer.glide.CoverMode
import com.mohammadkk.myaudioplayer.glide.MediaCover
import com.mohammadkk.myaudioplayer.models.Artist
import com.mohammadkk.myaudioplayer.utils.Libraries
import me.zhanghai.android.fastscroll.PopupTextProvider
import kotlin.math.abs

class ArtistsAdapter(
    private val context: FragmentActivity,
    var dataSet: MutableList<Artist>
) : RecyclerView.Adapter<ArtistsAdapter.ArtistHolder>(), PopupTextProvider {
    val settings = BaseSettings.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ArtistHolder(ItemArtistsBinding.inflate(inflater, parent, false))
    }
    override fun getItemCount(): Int {
        return dataSet.size
    }
    override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
        holder.bindItems(dataSet[holder.absoluteAdapterPosition])
    }
    override fun getPopupText(view: View, position: Int): CharSequence {
        val artist = dataSet.getOrNull(position)
        val result = when (abs(settings.artistsSorting)) {
            Constant.SORT_BY_TITLE -> artist?.title
            Constant.SORT_BY_DURATION -> return artist?.duration?.toFormattedDuration(true) ?: "-"
            else -> artist?.title
        }
        return Libraries.getSectionName(result, true)
    }
    fun swapDataSet(dataSet: List<Artist>) {
        this.dataSet = ArrayList(dataSet)
        notifyOnDataChanged()
    }
    private fun startTracks(artist: Artist) {
        Intent(context, TracksActivity::class.java).apply {
            putExtra(Constant.ARTIST_ID, artist.id)
            context.startActivity(this)
            context.overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }
    private fun loadCover(imageView: ImageView, artist: Artist) {
        val default = context.getDrawableCompat(R.drawable.ic_artist)
        when (val mode = settings.coverMode) {
            CoverMode.OFF -> imageView.setImageDrawable(default)
            else -> {
                val cover: Any = if (mode == CoverMode.MEDIA_STORE) {
                    artist.albumId.toAlbumArtURI()
                } else {
                    val song = artist.getSafeSong()
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
    inner class ArtistHolder(private val binding: ItemArtistsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(artist: Artist) {
            with(binding) {
                tvNameItem.text = artist.title
                tvTrackCountItem.text = context.getString(
                    R.string.artists_symbol,
                    artist.albumCount,
                    artist.trackCount
                )
                loadCover(imgArtTrackItem, artist)
                root.setOnClickListener {
                    if (!Constant.isBlockingClick()) {
                        startTracks(artist)
                    }
                }
            }
        }
    }
}