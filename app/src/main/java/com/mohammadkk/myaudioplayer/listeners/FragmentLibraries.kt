package com.mohammadkk.myaudioplayer.listeners

import android.view.MenuItem
import android.view.SubMenu

interface FragmentLibraries {
    fun onCreateSortMenu(sortMenu: SubMenu)
    fun onSelectedItemMenu(item: MenuItem)
    fun onFindItems(query: String?)
}