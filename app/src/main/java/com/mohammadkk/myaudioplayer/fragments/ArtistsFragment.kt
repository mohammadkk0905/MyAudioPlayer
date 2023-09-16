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
import com.mohammadkk.myaudioplayer.adapters.ArtistsAdapter
import com.mohammadkk.myaudioplayer.databinding.FragmentLibrariesBinding
import com.mohammadkk.myaudioplayer.extensions.isLandscape
import com.mohammadkk.myaudioplayer.listeners.FragmentLibraries
import com.mohammadkk.myaudioplayer.models.Artist
import com.mohammadkk.myaudioplayer.viewmodels.MusicViewModel
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import kotlin.math.abs

class ArtistsFragment : Fragment(), FragmentLibraries {
    private lateinit var binding: FragmentLibrariesBinding
    private val settings = BaseSettings.getInstance()
    private lateinit var musicViewModel: MusicViewModel
    private var artistsAdapter: ArtistsAdapter? = null
    private var unchangedList = listOf<Artist>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicViewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        musicViewModel.fragmentLibraries[2] = this
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLibrariesBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeList()
        musicViewModel.getArtistsList().observe(viewLifecycleOwner) {
            unchangedList = it ?: listOf()
            artistsAdapter?.swapDataSet(unchangedList)
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
        val dataSet = if (artistsAdapter != null) artistsAdapter!!.dataSet else mutableListOf()
        artistsAdapter = ArtistsAdapter(requireActivity(), dataSet)
        val spanCount = if (requireContext().isLandscape) {
            resources.getInteger(R.integer.def_grid_columns_land)
        } else {
            resources.getInteger(R.integer.def_grid_columns)
        }
        binding.listRv.setHasFixedSize(true)
        binding.listRv.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.listRv.adapter = artistsAdapter
        FastScrollerBuilder(binding.listRv).useMd2Style().setPadding(0, 0, 0, 0).build()
    }
    override fun onCreateSortMenu(sortMenu: SubMenu) {
        val sortOrder = settings.artistsSorting
        val sortName = abs(sortOrder)
        sortMenu.clear()
        sortMenu.add(
            R.id.group_one, R.id.action_sort_title,
            0, R.string.title
        ).isChecked = sortName == Constant.SORT_BY_TITLE
        sortMenu.add(
            R.id.group_one, R.id.action_sort_duration,
            1, R.string.duration
        ).isChecked = sortName == Constant.SORT_BY_DURATION
        sortMenu.add(
            R.id.group_one, R.id.action_sort_songs,
            2, R.string.song_count
        ).isChecked = sortName == Constant.SORT_BY_SONGS
        sortMenu.add(
            R.id.group_two, R.id.action_sort_ascending,
            3, R.string.ascending
        ).isChecked = sortOrder > 0
        sortMenu.add(
            R.id.group_two, R.id.action_sort_descending,
            4, R.string.descending
        ).isChecked = sortOrder < 0
        sortMenu.setGroupCheckable(R.id.group_one, true, true)
        sortMenu.setGroupCheckable(R.id.group_two, true, true)
        MenuCompat.setGroupDividerEnabled(sortMenu, true)
    }
    override fun onSelectedItemMenu(item: MenuItem) {
        val sortOrder = settings.artistsSorting
        var sortName = when (item.itemId) {
            R.id.action_sort_title -> Constant.SORT_BY_TITLE
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
            settings.artistsSorting = sortName
            musicViewModel.forceReload(2)
        }
    }
    override fun onFindItems(query: String?) {
        if (query.isNullOrEmpty()) {
            artistsAdapter?.swapDataSet(unchangedList)
        } else {
            val q = query.lowercase()
            val items = unchangedList.filter { it.title.lowercase().contains(q) }
            artistsAdapter?.swapDataSet(items)
        }
    }
}