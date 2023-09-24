package com.mohammadkk.myaudioplayer.listeners

import androidx.appcompat.view.ActionMode

interface AdapterListener {
    fun onCreateActionMode(callback: ActionMode.Callback): ActionMode?
    fun onDestroyActionMode()
    fun onReloadLibrary() {}
    fun onDestroyService() {}
}