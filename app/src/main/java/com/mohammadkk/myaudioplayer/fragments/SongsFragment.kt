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
import com.mohammadkk.myaudioplayer.adapters.SongsAdapter
import com.mohammadkk.myaudioplayer.databinding.FragmentSongsBinding
import com.mohammadkk.myaudioplayer.extensions.isLandscape
import com.mohammadkk.myaudioplayer.listeners.FragmentLibraries
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.viewmodels.MusicViewModel
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import kotlin.math.abs
import kotlin.random.Random.Default.nextInt

class SongsFragment : Fragment(), FragmentLibraries {
    private lateinit var binding: FragmentSongsBinding
    private val settings = BaseSettings.getInstance()
    private lateinit var musicViewModel: MusicViewModel
    private var songsAdapter: SongsAdapter? = null
    private var unchangedList = listOf<Song>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        musicViewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]
        musicViewModel.fragmentLibraries[0] = this
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSongsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeList()
        musicViewModel.getSongsList().observe(viewLifecycleOwner) {
            unchangedList = it ?: listOf()
            updateAdapter(unchangedList)
            if (binding.fragRefresher.isRefreshing) {
                binding.fragRefresher.isRefreshing = false
            }
        }
        binding.fragRefresher.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
                musicViewModel.updateLibraries()
            }, 200)
        }
        binding.fabShuffle.setOnClickListener {
            songsAdapter?.let { adapter ->
                if (adapter.itemCount > 0) {
                    adapter.startPlayer(nextInt(adapter.itemCount))
                } else {
                    binding.fabShuffle.hide()
                }
            }
        }
    }
    private fun initializeList() {
        val dataSet = if (songsAdapter != null) songsAdapter!!.dataSet else mutableListOf()
        songsAdapter = SongsAdapter(requireActivity(), dataSet, false)
        val spanCount = if (requireContext().isLandscape) {
            resources.getInteger(R.integer.def_list_columns_land)
        } else {
            resources.getInteger(R.integer.def_list_columns)
        }
        binding.songsListView.setHasFixedSize(true)
        binding.songsListView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.songsListView.adapter = songsAdapter
        FastScrollerBuilder(binding.songsListView).useMd2Style().setPadding(0, 0, 0, 0).build()
    }
    override fun onCreateSortMenu(sortMenu: SubMenu) {
        val sortOrder = settings.songsSorting
        val sortName = abs(sortOrder)
        sortMenu.clear()
        sortMenu.add(
            R.id.group_one, R.id.action_sort_title,
            0, R.string.title
        ).isChecked = sortName == Constant.SORT_BY_TITLE
        sortMenu.add(
            R.id.group_one, R.id.action_sort_album,
            1, R.string.album
        ).isChecked = sortName == Constant.SORT_BY_ALBUM
        sortMenu.add(
            R.id.group_one, R.id.action_sort_artist,
            2, R.string.artist
        ).isChecked = sortName == Constant.SORT_BY_ARTIST
        sortMenu.add(
            R.id.group_one, R.id.action_sort_duration,
            3, R.string.duration
        ).isChecked = sortName == Constant.SORT_BY_DURATION
        sortMenu.add(
            R.id.group_one, R.id.action_sort_date_added,
            4, R.string.date_added
        ).isChecked = sortName == Constant.SORT_BY_DATE_ADDED
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
        val sortOrder = settings.songsSorting
        var sortName = when (item.itemId) {
            R.id.action_sort_title -> Constant.SORT_BY_TITLE
            R.id.action_sort_album -> Constant.SORT_BY_ALBUM
            R.id.action_sort_artist -> Constant.SORT_BY_ARTIST
            R.id.action_sort_duration -> Constant.SORT_BY_DURATION
            R.id.action_sort_date_added -> Constant.SORT_BY_DATE_ADDED
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
            settings.songsSorting = sortName
            musicViewModel.forceReload(0)
        }
    }
    override fun onFindItems(query: String?) {
        if (query.isNullOrEmpty()) {
            updateAdapter(unchangedList)
        } else {
            val q = query.lowercase()
            val items = unchangedList.filter { it.title.lowercase().contains(q) }
            updateAdapter(items)
        }
    }
    private fun updateAdapter(items: List<Song>) {
        songsAdapter?.swapDataSet(items)
        binding.fabShuffle.text = items.size.toString()
    }
}