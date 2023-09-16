package com.mohammadkk.myaudioplayer.extensions

import android.database.Cursor

fun Cursor.getLongVal(column: String): Long {
    return getLong(getColumnIndexOrThrow(column))
}
fun Cursor.getIntVal(column: String): Int {
    return getInt(getColumnIndexOrThrow(column))
}
fun Cursor.getStringVal(column: String, defValue: String = ""): String {
    return getString(getColumnIndexOrThrow(column)) ?: defValue
}