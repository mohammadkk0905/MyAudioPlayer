package com.mohammadkk.myaudioplayer.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.mohammadkk.myaudioplayer.BaseSettings
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.databinding.ActivityMainBinding
import com.mohammadkk.myaudioplayer.databinding.PlayerControllerBinding
import com.mohammadkk.myaudioplayer.extensions.hasNotificationApi
import com.mohammadkk.myaudioplayer.extensions.hasPermission
import com.mohammadkk.myaudioplayer.extensions.reduceDragSensitivity
import com.mohammadkk.myaudioplayer.extensions.sendIntent
import com.mohammadkk.myaudioplayer.fragments.AlbumsFragment
import com.mohammadkk.myaudioplayer.fragments.ArtistsFragment
import com.mohammadkk.myaudioplayer.fragments.SongsFragment
import com.mohammadkk.myaudioplayer.listeners.AdapterListener
import com.mohammadkk.myaudioplayer.services.MusicService
import com.mohammadkk.myaudioplayer.viewmodels.MusicViewModel

class MainActivity : BaseActivity(), AdapterListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var binding2: PlayerControllerBinding
    private val settings get() = BaseSettings.getInstance()
    private val musicViewModel: MusicViewModel by viewModels()
    private var mActionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(mBackPressedCallback)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding2 = PlayerControllerBinding.bind(binding.root)
        setContentView(binding.root)
        setSupportActionBar(binding.mainActionBar)
        setupElementUi()
        val permission = Constant.storagePermissionApi()
        if (hasPermission(permission)) {
            setupRequires()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), Constant.PERMISSION_REQUEST_STORAGE)
        }
        binding.mainActionBar.setNavigationOnClickListener {
            if (MusicService.isMusicPlayer()) sendIntent(Constant.FINISH)
            finishAndRemoveTask()
        }
    }
    private val mBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (mActionMode != null) {
                onDestroyActionMode()
            } else {
                if (binding.mainPager.currentItem >= 1) {
                    binding.mainPager.currentItem = 0
                } else {
                    finish()
                }
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (Constant.isMarshmallowPlus()) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == Constant.PERMISSION_REQUEST_STORAGE) {
                if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                    storagePermissionManager()
                } else {
                    setupRequires()
                }
            }
        }
    }
    private fun storagePermissionManager() {
        val permission = Constant.storagePermissionApi()
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            Snackbar.make(binding.root, R.string.permission_storage_denied, Snackbar.LENGTH_SHORT).setAction(R.string.grant) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), Constant.PERMISSION_REQUEST_STORAGE)
            }.show()
        } else {
            Snackbar.make(binding.root, R.string.permission_storage_denied, Snackbar.LENGTH_SHORT).setAction(R.string.settings) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts(
                    "package",
                    packageName ?: applicationContext.packageName,
                    null
                )
                startActivity(intent)
            }.show()
        }
    }
    private fun setupElementUi() {
        val adapter = SlidePagerAdapter(supportFragmentManager, lifecycle)
        binding.mainPager.offscreenPageLimit = adapter.itemCount.minus(1)
        binding.mainPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.mainPager.adapter = adapter
        binding.mainPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                invalidateOptionsMenu()
            }
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                if (positionOffset == 0f && position != 0) {
                    if (mActionMode != null) {
                        onDestroyActionMode()
                    }
                }
            }
        })
        binding.mainPager.reduceDragSensitivity()
        TabLayoutMediator(binding2.bottomTabs, binding.mainPager) { tab, pos ->
            val mInfoTab = when (pos) {
                0 -> intArrayOf(R.drawable.ic_audiotrack, R.string.songs)
                1 -> intArrayOf(R.drawable.ic_library_music, R.string.albums)
                else -> intArrayOf(R.drawable.ic_person, R.string.artists)
            }
            tab.setCustomView(R.layout.bottom_tab_item).apply {
                customView?.findViewById<ImageView>(R.id.tab_item_icon)?.setImageResource(mInfoTab[0])
                customView?.findViewById<TextView>(R.id.tab_item_label)?.setText(mInfoTab[1])
            }
        }.attach()
    }
    private fun setupRequires() {
        musicViewModel.updateLibraries()
        BaseSettings.initialize(applicationContext)
        hasNotificationApi()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_libraries, menu)
        val library = musicViewModel.fragmentLibraries[binding.mainPager.currentItem]
        menu?.run {
            val sortMenu = findItem(R.id.action_order_by)?.subMenu!!
            library?.onCreateSortMenu(sortMenu)
            val searchView = findItem(R.id.action_search).actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    library?.onFindItems(query)
                    return false
                }
                override fun onQueryTextChange(newText: String?): Boolean {
                    library?.onFindItems(newText)
                    return false
                }
            })
            searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
                findItem(R.id.action_order_by).isVisible = !hasFocus
            }
        }
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val library = musicViewModel.fragmentLibraries[binding.mainPager.currentItem]
        library?.onSelectedItemMenu(item)
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateActionMode(callback: ActionMode.Callback): ActionMode? {
        if (mActionMode == null) {
            mActionMode = startSupportActionMode(callback)
        }
        return mActionMode
    }
    override fun onResume() {
        super.onResume()
        if (MusicService.isMusicPlayer()) {
            val visibility = binding2.nowPlayerFrag.visibility
            if (visibility != View.VISIBLE) {
                binding2.nowPlayerFrag.visibility = View.VISIBLE
            }
        } else {
            val visibility = binding2.nowPlayerFrag.visibility
            if (visibility != View.GONE) {
                binding2.nowPlayerFrag.visibility = View.GONE
            }
        }
    }
    override fun onDestroyActionMode() {
        mActionMode?.finish()
        mActionMode = null
    }
    override fun onReloadLibrary() {
        musicViewModel.updateLibraries()
    }
    private class SlidePagerAdapter(fm: FragmentManager, le: Lifecycle) : FragmentStateAdapter(fm, le) {
        private val fragments = mutableListOf<Fragment>()

        init {
            fragments.add(SongsFragment())
            fragments.add(AlbumsFragment())
            fragments.add(ArtistsFragment())
        }
        override fun getItemCount(): Int {
            return fragments.size
        }
        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
    }
}