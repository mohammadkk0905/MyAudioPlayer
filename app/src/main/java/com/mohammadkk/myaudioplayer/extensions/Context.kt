package com.mohammadkk.myaudioplayer.extensions

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.TransactionTooLargeException
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.documentfile.provider.DocumentFile
import com.bumptech.glide.util.Util.isOnMainThread
import com.google.android.material.color.MaterialColors
import com.mohammadkk.myaudioplayer.Constant
import com.mohammadkk.myaudioplayer.R
import com.mohammadkk.myaudioplayer.models.Song
import com.mohammadkk.myaudioplayer.services.MusicService

val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
val Context.isLandscape: Boolean get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

fun Context.hasPermission(permission: String?): Boolean {
    if (permission == null) return false
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
fun Context.sendIntent(actionName: String) {
    Intent(this, MusicService::class.java).apply {
        action = actionName
        try {
            if (Constant.isOreoPlus()) {
                startForegroundService(this)
            } else {
                startService(this)
            }
        } catch (ignored: Exception) {
        }
    }
}
fun Activity.hasNotificationApi(): Boolean {
    if (!Constant.isTiramisuPlus()) return true
    var result = true
    if (Constant.isTiramisuPlus()) {
        if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                Constant.PERMISSION_REQUEST_NOTIFICATION
            )
            result = false
        }
    }
    return result
}
fun Context.getColorCompat(@ColorRes id: Int): Int {
    return ResourcesCompat.getColor(resources, id, theme)
}
fun Context.getDrawableCompat(@DrawableRes id: Int): Drawable? {
    return ResourcesCompat.getDrawable(resources, id, theme)
}
fun Context.getPrimaryColor(): Int {
    var primary = MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary, Color.TRANSPARENT)
    if (primary == Color.TRANSPARENT) primary = getColorCompat(R.color.light_blue_500)
    return primary
}
fun Context.isMassUsbDeviceConnected(): Boolean {
    val usbManager = getSystemService(Context.USB_SERVICE) as? UsbManager
    val devices = usbManager?.deviceList ?: return false
    for (mDeviceName in devices) {
        val dc = mDeviceName.value
        for (i in 0 until dc.interfaceCount) {
            if (dc.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE) {
                return true
            }
        }
    }
    return false
}
fun Context.fromTreeUri(fileUri: Uri) = try {
    DocumentFile.fromTreeUri(this, fileUri)
} catch (e: Exception) {
    null
}
fun Activity.shareSongsIntent(songs: List<Song>) {
    if (songs.size == 1) {
        shareSongIntent(songs.first())
    } else {
        Constant.ensureBackgroundThread {
            val uriPaths = arrayListOf<Uri>()
            songs.forEach { uriPaths.add(it.toProviderUri(this)) }
            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "audio/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriPaths)
                try {
                    startActivity(Intent.createChooser(this, getString(R.string.share)))
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.no_app_found)
                } catch (e: RuntimeException) {
                    if (e.cause is TransactionTooLargeException) {
                        toast(R.string.maximum_share_reached)
                    } else {
                        errorToast(e)
                    }
                } catch (e: Exception) {
                    errorToast(e)
                }
            }
        }
    }
}
fun Activity.shareSongIntent(song: Song) {
    Constant.ensureBackgroundThread {
        val newUri = song.toProviderUri(this)
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, newUri)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            grantUriPermission("android", newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                startActivity(Intent.createChooser(this, getString(R.string.share)))
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                } else {
                    errorToast(e)
                }
            } catch (e: Exception) {
                errorToast(e)
            }
        }
    }
}
fun Context.errorToast(exception: Exception, length: Int = Toast.LENGTH_LONG) {
    errorToast(exception.toString(), length)
}
fun Context.errorToast(message: String?, length: Int = Toast.LENGTH_LONG) {
    if (message == null) return
    toast(String.format(getString(R.string.error_symbol), message), length)
}
fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    toast(getString(id), length)
}
fun Context.toast(message: String?, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (isOnMainThread()) {
            doToast(this, message ?: "null", length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, message ?: "null", length)
            }
        }
    } catch (ignored: Exception) {
    }
}
private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else {
        Toast.makeText(context, message, length).show()
    }
}