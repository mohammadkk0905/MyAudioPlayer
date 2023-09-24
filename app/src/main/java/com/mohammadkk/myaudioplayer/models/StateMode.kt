package com.mohammadkk.myaudioplayer.models

class StateMode(var mode: String, var id: Long) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as StateMode
        if (mode != other.mode) return false
        if (id != other.id) return false
        return true
    }
    override fun hashCode(): Int {
        var result = mode.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
    override fun toString(): String {
        return "StateMode(mode='$mode', id=$id)"
    }
}