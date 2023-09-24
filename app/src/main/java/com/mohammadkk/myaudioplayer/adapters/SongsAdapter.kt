package com.mohammadkk.myaudioplayer.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.MediaStoreSignature
import com.google.gson.GsonBuilder
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.activities.BaseActivity
import com.mohammadkk.myaudioplayer.activities.PlayerActivity
import com.mohammadkk.myaudioplayer.databinding.ItemSongsBinding
import com.mohammadkk.myaudioplayer.dialogs.DeleteSongsDialog
import com.mohammadkk.myaudioplayer.extensions.getColorCompat
import com.mohammadkk.myaudioplayer.extensions.getDrawableCompat
import com.mohammadkk.myaudioplayer.extensions.hasNotificationApi
import com.mohammadkk.myaudioplayer.extensions.notifyOnDataChanged
import com.mohammadkk.myaudioplayer.extensions.setTitleColor
import com.mohammadkk.myaudioplayer.extensions.shareSongIntent
import com.mohammadkk.myaudioplayer.extensions.shareSongsIntent
import com.mohammadkk.myaudioplayer.extensions.toAlbumArtURI
import com.mohammadkk.myaudioplayer.extensions.toFormattedDate
import com.mohammadkk.myaudioplayer.extensions.toFormattedDuration
import com.mohammadkk.myaudioplayer.listeners.AdapterListener
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.models.StateMode
import com.mohammadkk.myaudioplayer.services.MusicService
import com.mohammadkk.myaudioplayer.utils.Libraries
import com.mohammadkk.myaudioplayer.utils.RingtoneManager
import me.zhanghai.android.fastscroll.PopupTextProvider
import kotlin.math.abs

class SongsAdapter(
    private val context: FragmentActivity,
    var dataSet: MutableList<Song>,
    private var mode: String
) : RecyclerView.Adapter<SongsAdapter.SongHolder>(), PopupTextProvider {
    private val settings = BaseSettings.getInstance()
    private var adapterListener: AdapterListener? = null
    private var itemsSelected = mutableListOf<Int>()
    private var actionMode: ActionMode? = null
    private val isActionMode get() = actionMode != null
    private val actionCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_action_songs, menu)
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.action_share -> {
                    handleActionShare(null)
                    true
                }
                R.id.action_remove_files -> {
                    val items = itemsSelected.map { dataSet[it] }
                    handleActionDelete(items)
                    true
                }
                else -> false
            }
        }
        override fun onDestroyActionMode(mode: ActionMode?) {
            adapterListener?.onDestroyActionMode()
            actionMode = null
            itemsSelected.clear()
            notifyOnDataChanged()
        }
    }

    init {
        try {
            adapterListener = context as AdapterListener
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongHolder {
        val inflater = LayoutInflater.from(parent.context)
        return SongHolder(ItemSongsBinding.inflate(inflater, parent, false))
    }
    override fun getItemCount(): Int {
        return dataSet.size
    }
    override fun onBindViewHolder(holder: SongHolder, position: Int) {
        holder.bindItems(dataSet[holder.absoluteAdapterPosition])
    }
    override fun getPopupText(view: View, position: Int): CharSequence {
        val song = dataSet.getOrNull(position)
        val result = when (abs(settings.songsSorting)) {
            Constant.SORT_BY_TITLE -> song?.title
            Constant.SORT_BY_ALBUM -> song?.album
            Constant.SORT_BY_ARTIST -> song?.artist
            Constant.SORT_BY_DURATION -> return song?.duration?.toFormattedDuration(true) ?: "-"
            else -> song?.title
        }
        return Libraries.getSectionName(result, true)
    }
    fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = ArrayList(dataSet)
        notifyOnDataChanged()
    }
    fun swapDeleted() {
        if (mode != "MAIN") {
            DeleteSongsDialog.getDataset().forEach { s ->
                val index = dataSet.indexOf(s)
                if (index != -1) {
                    dataSet.removeAt(index)
                    notifyItemRemoved(index)
                }
            }
            DeleteSongsDialog.destroyDataset()
        }
    }
    fun startPlayer(position: Int) {
        val intent = Intent(context, PlayerActivity::class.java)
        val song = dataSet[position]
        intent.putExtra(Constant.SONG, GsonBuilder().create().toJson(song))
        intent.putExtra(Constant.RESTART_PLAYER, true)
        MusicService.mCurrSong = song
        settings.lastStateMode = StateMode(mode, when (mode) {
            "ALBUM" -> song.albumId
            "ARTIST" -> song.artistId
            else -> song.id
        })
        if (mode != "MAIN") {
            if (context is BaseActivity) {
                context.isFadeAnimation = false
            }
        }
        context.startActivity(intent)
    }
    private fun startActionMode() {
        if (!isActionMode) {
            actionMode = adapterListener?.onCreateActionMode(actionCallback)
        }
    }
    private fun setItemViewSelected(position: Int) {
        if (!itemsSelected.remove(position)) {
            itemsSelected.add(position)
        }
        actionMode?.title = "${itemsSelected.size} / ${dataSet.size}"
        notifyItemChanged(position)
        if (itemsSelected.isEmpty()) {
            actionMode = null
            adapterListener?.onDestroyActionMode()
        }
    }
    private fun clickHandlePopupMenu(item: MenuItem, song: Song): Boolean {
        when (item.itemId) {
            R.id.action_share -> {
                handleActionShare(song)
                return true
            }
            R.id.action_ringtone -> {
                if (isActionMode) {
                    actionMode?.finish()
                    actionMode = null
                }
                if (RingtoneManager.requiresDialog(context)) {
                    RingtoneManager.showDialog(context)
                } else {
                    RingtoneManager.setRingtone(context, song)
                }
                return true
            }
            R.id.action_remove_file -> {
                handleActionDelete(listOf(song))
                return true
            }
        }
        return false
    }
    private fun handleActionShare(song: Song?) {
        if (isActionMode) {
            actionMode?.finish()
            actionMode = null
        }
        if (song != null) {
            context.shareSongIntent(song)
        } else {
            val items = itemsSelected.map { dataSet[it] }
            context.shareSongsIntent(items)
        }
    }
    private fun handleActionDelete(items: List<Song>) {
        if (isActionMode) {
            actionMode?.finish()
            actionMode = null
        }
        DeleteSongsDialog.create(items).show(
            context.supportFragmentManager,
            "DELETE_SONGS"
        )
    }
    inner class SongHolder(private val binding: ItemSongsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(song: Song) {
            with(binding) {
                if (itemsSelected.contains(absoluteAdapterPosition)) {
                    root.isActivated = true
                    ivMoreOptions.setImageResource(R.drawable.ic_check_circle)
                    ivMoreOptions.rotation = 0f
                    ivMoreOptions.setBackgroundResource(android.R.color.transparent)
                } else {
                    root.isActivated = false
                    ivMoreOptions.setImageResource(R.drawable.ic_more_vert)
                    ivMoreOptions.rotation = 90f
                    ivMoreOptions.setBackgroundResource(R.drawable.round_selector)
                }
                tvTitleTrackItem.text = song.title
                tvTrackHistroyItem.text = context.getString(
                    R.string.duration_date_symbol,
                    song.duration.toFormattedDuration(false),
                    song.dateAdded.toFormattedDate()
                )
                val defIcon = context.getDrawableCompat(R.drawable.ic_audiotrack)
                Glide.with(context)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .error(defIcon)
                    .placeholder(defIcon)
                    .signature(MediaStoreSignature("", song.dateModified, 0))
                    .load(song.albumId.toAlbumArtURI())
                    .into(binding.imgArtTrackItem)

                root.setOnClickListener {
                    if (isActionMode) {
                        setItemViewSelected(absoluteAdapterPosition)
                    } else {
                        if (context.hasNotificationApi()) {
                            startPlayer(absoluteAdapterPosition)
                        }
                    }
                }
                root.setOnLongClickListener {
                    startActionMode()
                    setItemViewSelected(absoluteAdapterPosition)
                    true
                }
                ivMoreOptions.setOnClickListener { v ->
                    if (itemsSelected.contains(absoluteAdapterPosition)) {
                        setItemViewSelected(absoluteAdapterPosition)
                    } else {
                        if (!Constant.isBlockingClick()) {
                            val popupMenu = PopupMenu(context, v)
                            popupMenu.inflate(R.menu.menu_action_song)
                            val deleted = popupMenu.menu.findItem(R.id.action_remove_file)
                            deleted.setTitleColor(context.getColorCompat(R.color.red_500))
                            popupMenu.setOnMenuItemClickListener {
                                clickHandlePopupMenu(it, dataSet[absoluteAdapterPosition])
                            }
                            popupMenu.show()
                        }
                    }
                }
            }
        }
    }
}