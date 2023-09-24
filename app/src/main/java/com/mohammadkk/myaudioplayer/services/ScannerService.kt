package com.mohammadkk.myaudioplayer.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.extensions.hasPermission
import com.mohammadkk.myaudioplayer.extensions.stopForegroundCompat
import com.mohammadkk.myaudioplayer.extensions.toast
import com.mohammadkk.myaudioplayer.listeners.AdapterListener
import com.mohammadkk.myaudioplayer.utils.FileUtils
import com.mohammadkk.myaudioplayer.utils.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileFilter

class ScannerService : Service() {
    private val myBinder: IBinder = LocalScanner()
    private var notificationUtils: NotificationUtils? = null
    private var currentPath = ""
    private var isForegroundService = false
    var listener: AdapterListener? = null

    override fun onCreate() {
        super.onCreate()
        notificationUtils = NotificationUtils.createInstance(this)
        startForegroundWithNotify()
    }
    override fun onDestroy() {
        super.onDestroy()
        isForegroundService = false
        listener = null
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Constant.isQPlus() && !hasPermission(Constant.STORAGE_PERMISSION)) {
            return START_NOT_STICKY
        }
        when (intent?.action) {
            Constant.SCANNER -> {
                startForegroundWithNotify()
                CoroutineScope(Dispatchers.IO).launch {
                    listPaths(intent) { paths ->
                        scanPaths(paths)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }
    private suspend fun listPaths(intent: Intent, onDone: (paths: Array<String?>) -> Unit) {
        currentPath = intent.getStringExtra(Constant.SONG) ?: Environment.getExternalStorageDirectory().absolutePath
        val file = File(currentPath)

        val mPaths = try {
            val paths: Array<String?>
            if (file.isDirectory) {
                val files = FileUtils.listFilesDeep(file, AUDIO_FILE_FILTER)
                paths = arrayOfNulls(files.size)
                for (i in files.indices) {
                    val f = files[i]
                    paths[i] = FileUtils.safeGetCanonicalPath(f)
                }
            } else {
                paths = arrayOfNulls(1)
                paths[0] = file.path
            }
            paths
        } catch (e: Exception) {
            arrayOf()
        }
        withContext(Dispatchers.Main) {
            onDone(mPaths)
        }
    }
    private fun scanPaths(toBeScanned: Array<String?>) {
        if (toBeScanned.isEmpty()) {
            toast(R.string.nothing_to_scan)
            stopForegroundOrNotification()
            stopSelf()
            listener?.onDestroyService()
        } else {
            var cnt = toBeScanned.size
            MediaScannerConnection.scanFile(applicationContext, toBeScanned, null) { _, _ ->
                if (--cnt == 0) {
                    toast(getString(R.string.scann_message, toBeScanned.size))
                    listener?.onReloadLibrary()
                    stopForegroundOrNotification()
                    stopSelf()
                    listener?.onDestroyService()
                }
            }
        }
    }
    private fun startForegroundWithNotify() {
        notificationUtils?.createMediaScanNotification(currentPath, 0, 0)?.let {
            if (!isForegroundService) {
                if (Constant.isSPlus()) {
                    try {
                        startForeground(NotificationUtils.SCANNER_NOTIFICATION_ID, it)
                        isForegroundService = true
                    } catch (e: ForegroundServiceStartNotAllowedException) {
                        e.printStackTrace()
                    }
                } else {
                    startForeground(NotificationUtils.SCANNER_NOTIFICATION_ID, it)
                    isForegroundService = true
                }
            } else {
                notificationUtils!!.notify(NotificationUtils.SCANNER_NOTIFICATION_ID, it)
            }
        }
    }
    private fun stopForegroundOrNotification() {
        try {
            if (isForegroundService) {
                stopForegroundCompat(true)
                isForegroundService = false
            } else {
                notificationUtils?.cancel(NotificationUtils.SCANNER_NOTIFICATION_ID)
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
    override fun onBind(intent: Intent?): IBinder {
        return myBinder
    }
    inner class LocalScanner : Binder() {
        val instance: ScannerService get() = this@ScannerService
    }
    companion object {
        private val AUDIO_FILE_FILTER = FileFilter { file: File ->
            !file.isHidden && (file.isDirectory || FileUtils.isAudioFile(file))
        }
    }
}