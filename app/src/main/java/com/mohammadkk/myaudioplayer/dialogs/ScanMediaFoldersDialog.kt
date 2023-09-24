package com.mohammadkk.myaudioplayer.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.activities.BaseActivity
import com.mohammadkk.myaudioplayer.databinding.DialogRecyclerBinding
import com.mohammadkk.myaudioplayer.extensions.errorToast
import com.mohammadkk.myaudioplayer.extensions.hasNotificationApi
import com.mohammadkk.myaudioplayer.extensions.hasPermission
import com.mohammadkk.myaudioplayer.extensions.notifyOnDataChanged
import com.mohammadkk.myaudioplayer.services.ScannerService
import java.io.File

class ScanMediaFoldersDialog : DialogFragment() {
    private var initialPath: String = ""
    private var parentFolder: File? = null
    private var parentContent: Array<File>? = null
    private var rootAdapter: ScanAdapter? = null
    private var canGoUp = false
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (!requireContext().hasPermission(Constant.STORAGE_PERMISSION)) {
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error_label)
                .setMessage(R.string.permission_storage_denied)
                .setPositiveButton(android.R.string.ok, null)
                .create()
        }
        initialPath = Environment.getExternalStorageDirectory().absolutePath
        var mSavedInstanceState = savedInstanceState
        if (mSavedInstanceState == null) {
            mSavedInstanceState = Bundle()
        }
        if (!mSavedInstanceState.containsKey("current_path")) {
            mSavedInstanceState.putString("current_path", initialPath)
        }
        parentFolder = File(mSavedInstanceState.getString("current_path", File.pathSeparator))
        checkIfCanGoUp()
        parentContent = listFiles()
        rootAdapter = ScanAdapter(getContentsArray())
        val binding = DialogRecyclerBinding.inflate(layoutInflater)
        binding.dialogRecycler.adapter = rootAdapter
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(parentFolder?.absolutePath)
            .setCancelable(false)
            .setView(binding.root)
            .setPositiveButton(R.string.directory_scan) { _, _ ->
                val absolutePath = parentFolder?.absolutePath
                if (requireActivity().hasNotificationApi() && absolutePath != null) {
                    (activity as? BaseActivity)?.apply {
                        val serviceIntent = Intent(this, ScannerService::class.java)
                        serviceIntent.putExtra(Constant.SONG, absolutePath)
                        serviceIntent.action = Constant.SCANNER
                        try {
                            onBindService()
                            startService(serviceIntent)
                        } catch (e: Exception) {
                            errorToast(e)
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                dismiss()
            }

        return builder.create()
    }
    private fun selectionDirectory(i: Int) {
        if (canGoUp && i == 0) {
            if (parentFolder?.absolutePath == initialPath) {
                checkIfCanGoUp()
                return
            }
            parentFolder = parentFolder?.parentFile
            checkIfCanGoUp()
        } else {
            parentFolder = parentContent?.get(if (canGoUp) i - 1 else i)
            canGoUp = true
        }
        reloadDialog()
    }
    private fun reloadDialog() {
        parentContent = listFiles()
        val baseDialog = dialog as AlertDialog
        baseDialog.setTitle(parentFolder?.absolutePath)
        rootAdapter?.reload()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_path", parentFolder?.absolutePath ?: initialPath)
    }
    private fun checkIfCanGoUp() {
        canGoUp = parentFolder?.parent != null
    }
    private fun listFiles(): Array<File>? {
        val results = arrayListOf<File>()
        parentFolder?.listFiles()?.forEach { fi ->
            if (fi.isDirectory && !fi.isHidden) {
                results.add(fi)
            }
        }
        if (results.isNotEmpty()) {
            results.sortBy { it.name.lowercase() }
            return results.toTypedArray()
        }
        return null
    }
    private fun getContentsArray(): Array<String?> {
        if (parentContent == null) {
            if (canGoUp) return arrayOf("..")
            return arrayOf()
        }
        val baseSize = if (canGoUp) 1 else 0
        val results = arrayOfNulls<String>(parentContent!!.size + baseSize)
        if (canGoUp) results[0] = ".."
        for (i in 0 until parentContent!!.size) {
            results[i + baseSize] = parentContent!![i].name
        }
        return results
    }
    private inner class ScanAdapter(private var items: Array<String?>) : RecyclerView.Adapter<ScanAdapter.ScanHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanHolder {
            val inflater = LayoutInflater.from(parent.context)
            return ScanHolder(inflater.inflate(R.layout.list_scan_dialog, parent, false))
        }
        override fun onBindViewHolder(holder: ScanHolder, position: Int) {
            holder.title.text = items[position]
            holder.itemView.setOnClickListener { selectionDirectory(position) }
        }
        fun reload() {
            items = getContentsArray()
            notifyOnDataChanged()
        }
        override fun getItemCount(): Int {
            return items.size
        }
        private inner class ScanHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.title)
        }
    }
}