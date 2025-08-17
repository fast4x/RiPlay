package it.fast4x.riplay.models

import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
data class ExternalApp(
    @PrimaryKey val packageName: String,
    val activityName: String,
    @ColumnInfo(index = true) val appName: String?,
    @ColumnInfo(typeAffinity = ColumnInfo.INTEGER) val isSystemApp: Boolean
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, activityName)
}
