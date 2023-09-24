package com.mohammadkk.myaudioplayer.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.activities.BaseActivity
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.listeners.AdapterListener
import com.mohammadkk.myaudioplayer.models.Song

class DeleteSongsDialog : DialogFragment() {
    private var baseActivity: BaseActivity? = null
    private var listener: AdapterListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            baseActivity = activity as BaseActivity
            listener = activity as AdapterListener
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alert = MaterialAlertDialogBuilder(requireContext())
        if (dataset.isEmpty()) {
            context?.applicationContext?.toast("123")
            dismiss()
            return alert.create()
        }
        if (dataset.size == 1) {
            alert.setTitle(R.string.delete_song)
        } else {
            alert.setTitle(R.string.delete_songs)
        }
        val typedArray = arrayListOf<String>()
        var isBreak = false
        for (i in dataset.indices) {
            if (i >= 9) {
                isBreak = true
                break
            }
            typedArray.add("${i + 1}-${dataset[i].title}")
        }
        if (isBreak) typedArray.add("${dataset.size}-${dataset.last().title}")
        val dialogAdapter = object : ArrayAdapter<String>(requireContext(), R.layout.list_item_dialog, typedArray) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflater = LayoutInflater.from(context)
                var mView = convertView
                if (mView == null) {
                    mView = inflater.inflate(R.layout.list_item_dialog, parent, false)
                }
                val title: TextView = mView!!.findViewById(R.id.title)
                title.text = typedArray[position]
                return mView
            }
        }
        alert.setAdapter(dialogAdapter, null)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                baseActivity?.deleteSongs(dataset) {
                    listener?.onReloadLibrary()
                }
                dismiss()
            }
            .setNegativeButton(android.R.string.cancel) {  _, _ ->
                destroyDataset()
                dismiss()
            }
        return alert.create()
    }
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        destroyDataset()
    }
    companion object {
        private val dataset = mutableListOf<Song>()

        fun create(songs: Collection<Song>): DeleteSongsDialog {
            dataset.clear()
            if (songs.isNotEmpty()) {
                dataset.addAll(songs)
            }
            return DeleteSongsDialog()
        }
        fun getDataset(): MutableList<Song> {
            return dataset
        }
        fun destroyDataset() {
            dataset.clear()
        }
    }
}