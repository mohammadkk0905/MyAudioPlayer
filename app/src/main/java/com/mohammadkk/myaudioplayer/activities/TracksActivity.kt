package com.mohammadkk.myaudioplayer.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AndroidRuntimeException
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.adapters.SongsAdapter
import com.mohammadkk.myaudioplayer.databinding.ActivityTracksBinding
import com.mohammadkk.myaudioplayer.extensions.isLandscape
import com.mohammadkk.myaudioplayer.extensions.sendIntent
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.listeners.AdapterListener
import com.mohammadkk.myaudioplayer.services.MusicService
import com.mohammadkk.myaudioplayer.utils.Libraries
import com.mohammadkk.myaudioplayer.viewmodels.SubViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class TracksActivity : BaseActivity(), AdapterListener {
    private lateinit var binding: ActivityTracksBinding
    private val subViewModel: SubViewModel by viewModels()
    private var mActionMode: ActionMode? = null
    private var fastScroller: FastScroller? = null
    private var songsAdapter: SongsAdapter? = null
    private val receiverUsb = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d("TracksActivity", "Usb otg connected")
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    if (MusicService.isMusicPlayer()) {
                        sendIntent(Constant.FINISH)
                        finish()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(mBackPressedCallback)
        binding = ActivityTracksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainActionBar)
        initRefreshing(true)
        getInputMethod()
        registerUSBReceiver()
        subViewModel.getListData().observe(this) {
            val items = Libraries.getSortedSongs(it)
            songsAdapter?.swapDataSet(items)
            initRefreshing(false)
        }
        binding.tracksRefresh.setOnRefreshListener {
            initRefreshing(true)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.tracksRefresh.isRefreshing = false
                getInputMethod()
            }, 200)
        }
        binding.mainActionBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
    private val mBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (mActionMode != null) {
                onDestroyActionMode()
            } else {
                finish()
            }
        }
    }
    private fun initRefreshing(isDisabled: Boolean) {
        binding.progressCircular.isVisible = isDisabled
        binding.progressCircular.isIndeterminate = isDisabled
        binding.tracksRefresh.isEnabled = !isDisabled
    }
    private fun getInputMethod() {
        if (intent.hasExtra(Constant.ALBUM_ID)) {
            supportActionBar?.title = getString(R.string.album)
            val albumId = intent.getLongExtra(Constant.ALBUM_ID, -1L)
            if (albumId == -1L) {
                toast(R.string.unknown_error_occurred)
                finish()
                return
            }
            initializeList("ALBUM")
            albumManager(albumId)
        } else if (intent.hasExtra(Constant.ARTIST_ID)) {
            supportActionBar?.title = getString(R.string.artist)
            val artistId = intent.getLongExtra(Constant.ARTIST_ID, -1L)
            if (artistId == -1L) {
                toast(R.string.unknown_error_occurred)
                finish()
                return
            }
            initializeList("ARTIST")
            artistManager(artistId)
        } else if (intent.getBooleanExtra("otg", false)) {
            initializeList("OTG")
            subViewModel.handleOTG(this)
        }
    }
    private fun albumManager(albumId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val songs = Libraries.fetchSongsByAlbumId(applicationContext, albumId)
            withContext(Dispatchers.Main) {
                subViewModel.updateList(songs)
            }
        }
    }
    private fun artistManager(artistId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val songs = Libraries.fetchSongsByArtistId(applicationContext, artistId)
            withContext(Dispatchers.Main) {
                subViewModel.updateList(songs)
            }
        }
    }
    private fun initializeList(mode: String) {
        val dataSet = if (songsAdapter != null) songsAdapter!!.dataSet else mutableListOf()
        songsAdapter = SongsAdapter(this, dataSet, mode)
        val spanCount = if (isLandscape) {
            resources.getInteger(R.integer.def_list_columns_land)
        } else {
            resources.getInteger(R.integer.def_list_columns)
        }
        binding.tracksRv.setHasFixedSize(true)
        binding.tracksRv.layoutManager = GridLayoutManager(this, spanCount)
        binding.tracksRv.adapter = songsAdapter
        if (fastScroller == null) {
            fastScroller = FastScrollerBuilder(binding.tracksRv).useMd2Style().setPadding(0, 0, 0, 0).build()
        }
    }
    private fun registerUSBReceiver() {
        if (intent.getBooleanExtra("otg", false)) {
            val intentFilter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            try {
                applicationContext.registerReceiver(receiverUsb, intentFilter)
            } catch (e: AndroidRuntimeException) {
                LocalBroadcastManager.getInstance(applicationContext).registerReceiver(receiverUsb, intentFilter)
            }
        }
    }
    private fun unregisterUSBReceiver() {
        if (intent.getBooleanExtra("otg", false)) {
            try {
                applicationContext.unregisterReceiver(receiverUsb)
            } catch (e: AndroidRuntimeException) {
                LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(receiverUsb)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (MusicService.isMusicPlayer()) {
            val visibility = binding.navFragPlayer.visibility
            if (visibility != View.VISIBLE) {
                binding.navFragPlayer.visibility = View.VISIBLE
            }
        } else {
            val visibility = binding.navFragPlayer.visibility
            if (visibility != View.GONE) {
                binding.navFragPlayer.visibility = View.GONE
            }
        }
    }
    override fun onPause() {
        super.onPause()
        if (isFadeAnimation) {
            overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
    }
    override fun onDestroy() {
        unregisterUSBReceiver()
        super.onDestroy()
    }
    override fun onReloadLibrary() {
        songsAdapter?.swapDeleted()
        if (MusicService.isMusicPlayer()) {
            sendIntent(Constant.REFRESH_LIST)
        }
    }
    override fun onCreateActionMode(callback: ActionMode.Callback): ActionMode? {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(callback)
        }
        return mActionMode
    }
    override fun onDestroyActionMode() {
        mActionMode?.finish()
        mActionMode = null
    }
}