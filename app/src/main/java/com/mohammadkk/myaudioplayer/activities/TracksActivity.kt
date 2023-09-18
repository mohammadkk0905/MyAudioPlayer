package com.mohammadkk.myaudioplayer.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.GridLayoutManager
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.adapters.SongsAdapter
import com.mohammadkk.myaudioplayer.databinding.ActivityTracksBinding
import com.mohammadkk.myaudioplayer.extensions.isLandscape
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(mBackPressedCallback)
        binding = ActivityTracksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainActionBar)
        getInputMethod()
        subViewModel.getListData().observe(this) {
            songsAdapter?.swapDataSet(Libraries.getSortedSongs(it))
            if (binding.tracksRefresh.isRefreshing) {
                binding.tracksRefresh.isRefreshing = false
            }
        }
        binding.tracksRefresh.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
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
    private fun getInputMethod() {
        if (intent.hasExtra(Constant.ALBUM_ID)) {
            supportActionBar?.title = getString(R.string.album)
            val albumId = intent.getLongExtra(Constant.ALBUM_ID, -1L)
            if (albumId == -1L) {
                toast(R.string.unknown_error_occurred)
                finish()
                return
            }
            initializeList()
            albumManager(albumId)
        } else if (intent.hasExtra(Constant.ARTIST_ID)) {
            supportActionBar?.title = getString(R.string.artist)
            val artistId = intent.getLongExtra(Constant.ARTIST_ID, -1L)
            if (artistId == -1L) {
                toast(R.string.unknown_error_occurred)
                finish()
                return
            }
            initializeList()
            artistManager(artistId)
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
    private fun initializeList() {
        val dataSet = if (songsAdapter != null) songsAdapter!!.dataSet else mutableListOf()
        songsAdapter = SongsAdapter(this, dataSet, true)
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