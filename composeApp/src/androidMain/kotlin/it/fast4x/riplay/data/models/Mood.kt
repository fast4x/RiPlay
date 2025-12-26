package it.fast4x.riplay.data.models

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import it.fast4x.environment.Environment
import it.fast4x.environment.models.NavigationEndpoint

data class Mood(
    val name: String,
    val color: Color,
    val browseId: String?,
    val params: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        name = parcel.readString()!!,
        color = Color(parcel.readLong()),
        browseId = parcel.readString()!!,
        params = parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) = with(parcel) {
        writeString(name)
        writeLong(color.value.toLong())
        writeString(browseId)
        writeString(params)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Mood> {
        override fun createFromParcel(parcel: Parcel) = Mood(parcel)
        override fun newArray(size: Int) = arrayOfNulls<Mood>(size)
    }
}

fun Environment.Mood.Item.toUiMood() = Mood(
    name = title,
    color = Color(stripeColor),
    browseId = endpoint.browseId,
    params = endpoint.params
)

data class Chip(
    val name: String,
    val browseId: String?,
    val params: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        name = parcel.readString()!!,
        browseId = parcel.readString()!!,
        params = parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) = with(parcel) {
        writeString(name)
        writeString(browseId)
        writeString(params)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Chip> {
        override fun createFromParcel(parcel: Parcel): Chip {
            return Chip(parcel)
        }
        override fun newArray(size: Int) = arrayOfNulls<Chip>(size)
    }
}

fun Environment.Chip.toUiChip() = Chip(
    name = this.title,
    browseId = this.endpoint?.browseId,
    params = this.endpoint?.params

)