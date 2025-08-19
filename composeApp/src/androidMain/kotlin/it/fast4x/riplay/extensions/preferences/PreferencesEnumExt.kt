package it.fast4x.riplay.extensions.preferences

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcel

inline fun <reified T : Enum<T>> Bundle.getEnum(key: String, default: T) =
        getInt(key).let { if (it >= 0) enumValues<T>()[it] else default }

fun <T : Enum<T>> Bundle.putEnum(key: String, value: T?) =
        putInt(key, value?.ordinal ?: -1)

inline fun <reified T : Enum<T>> SharedPreferences.getEnumNew(key: String, default: T) =
        this.getInt(key, -1).let { if (it >= 0) enumValues<T>()[it] else default }

fun <T : Enum<T>> SharedPreferences.Editor.putEnum(key: String, value: T?) : SharedPreferences.Editor =
        this.putInt(key, value?.ordinal ?: -1)

inline fun <reified T : Enum<T>> Parcel.readEnum() =
        readInt().let { if (it >= 0) enumValues<T>()[it] else null }

fun <T : Enum<T>> Parcel.writeEnum(value: T?) =
        writeInt(value?.ordinal ?: -1)

inline fun <reified T1 : Enum<T1>, T2 : Number> fromNumber(value: T2) : T1? =
        enumValues<T1>().firstOrNull { it.ordinal == value }

inline fun <reified T1 : Enum<T1>> fromInt(value: Int) : T1? = fromNumber<T1, Int>(value)

inline fun <reified T1 : Enum<T1>> fromLong(value: Long) : T1? = fromNumber<T1, Long>(value)