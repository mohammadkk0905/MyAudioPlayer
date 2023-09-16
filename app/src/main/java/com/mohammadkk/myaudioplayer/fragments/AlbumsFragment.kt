package com.mohammadkk.myaudioplayer.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.adapters.AlbumsAdapter
import com.mohammadkk.myaudioplayer.databinding.FragmentLibrariesBinding
import com.mohammadkk.myaudioplayer.extensions.isLandscape
import com.mohammadkk.myaudioplayer.listeners.FragmentLibraries
import com.mohammadkk.myaudioplayer.models.Album
import com.mohammadkk.myaudioplayer.viewmodels.MusicViewModel
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import kotlin.math.abs

class AlbumsFragment : Fragment(), FragmentLibraries {
    private lateinit var binding: FragmentLibrariesBinding
    private val settings = BaseSettings.getInstance()
    private lateinit var musicViewModel: MusicViewModel
    private var albumsAdapter: AlbumsAdapter? = null
    private var unchangedList = listOf<Album>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicViewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        musicViewModel.fragmentLibraries[1] = this
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLibrariesBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeList()
        musicViewModel.getAlbumsList().observe(viewLifecycleOwner) {
            unchangedList = it ?: listOf()
            albumsAdapter?.swapDataSet(unchangedList)
            if (binding.fragRefresher.isRefreshing) {
                binding.fragRefresher.isRefreshing = false
            }
        }
        binding.fragRefresher.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
                musicViewModel.updateLibraries()
            }, 200)
        }
    }
    private fun initializeList() {
        val dataSet = if (albumsAdapter != null) albumsAdapter!!.dataSet else mutableListOf()
        albumsAdapter = AlbumsAdapter(requireActivity(), dataSet)
        val spanCount = if (requireContext().isLandscape) {
            resources.getInteger(R.integer.def_grid_columns_land)
        } else {
            resources.getInteger(R.integer.def_grid_columns)
        }
        binding.listRv.setHasFixedSize(true)
        binding.listRv.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.listRv.adapter = albumsAdapter
        FastScrollerBuilder(binding.listRv).useMd2Style().setPadding(0, 0, 0, 0).build()
    }
    override fun onCreateSortMenu(sortMenu: SubMenu) {
        val sortOrder = settings.albumsSorting
        val sortName = abs(sortOrder)
        sortMenu.clear()
        sortMenu.add(
            R.id.group_one, R.id.action_sort_title,
            0, R.string.title
        ).isChecked = sortName == Constant.SORT_BY_TITLE
        sortMenu.add(
            R.id.group_one, R.id.action_sort_artist,
            1, R.string.artist
        ).isChecked = sortName == Constant.SORT_BY_ARTIST
        sortMenu.add(
            R.id.group_one, R.id.action_sort_year,
            2, R.string.year
        ).isChecked = sortName == Constant.SORT_BY_YEAR
        sortMenu.add(
            R.id.group_one, R.id.action_sort_duration,
            3, R.string.duration
        ).isChecked = sortName == Constant.SORT_BY_DURATION
        sortMenu.add(
            R.id.group_one, R.id.action_sort_songs,
            4, R.string.song_count
        ).isChecked = sortName == Constant.SORT_BY_SONGS
        sortMenu.add(
            R.id.group_two, R.id.action_sort_ascending,
            5, R.string.ascending
        ).isChecked = sortOrder > 0
        sortMenu.add(
            R.id.group_two, R.id.action_sort_descending,
            6, R.string.descending
        ).isChecked = sortOrder < 0
        sortMenu.setGroupCheckable(R.id.group_one, true, true)
        sortMenu.setGroupCheckable(R.id.group_two, true, true)
        MenuCompat.setGroupDividerEnabled(sortMenu, true)
    }
    override fun onSelectedItemMenu(item: MenuItem) {
        val sortOrder = settings.albumsSorting
        var sortName = when (item.itemId) {
            R.id.action_sort_title -> Constant.SORT_BY_TITLE
            R.id.action_sort_artist -> Constant.SORT_BY_ARTIST
            R.id.action_sort_year -> Constant.SORT_BY_YEAR
            R.id.action_sort_duration -> Constant.SORT_BY_DURATION
            R.id.action_sort_songs -> Constant.SORT_BY_SONGS
            else -> abs(sortOrder)
        }
        if (sortOrder < 0) sortName *= -1
        if (item.itemId == R.id.action_sort_ascending) {
            if (sortName < 0) sortName = abs(sortName)
            item.isChecked = true
        }
        if (item.itemId == R.id.action_sort_descending) {
            if (sortName > 0) sortName *= -1
            item.isChecked = true
        }
        if (sortName != sortOrder) {
            item.isChecked = true
            settings.albumsSorting = sortName
            musicViewModel.forceReload(1)
        }
    }
    override fun onFindItems(query: String?) {
        if (query.isNullOrEmpty()) {
            albumsAdapter?.swapDataSet(unchangedList)
        } else {
            val q = query.lowercase()
            val items = unchangedList.filter { it.title.lowercase().contains(q) }
            albumsAdapter?.swapDataSet(items)
        }
    }
}