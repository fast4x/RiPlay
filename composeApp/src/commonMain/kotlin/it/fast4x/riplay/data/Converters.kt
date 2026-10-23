package it.fast4x.riplay.data

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import it.fast4x.riplay.data.models.Album
import it.fast4x.riplay.extensions.musicbrainz.models.ExternalLink
import it.fast4x.riplay.enums.AlbumNature
import it.fast4x.riplay.enums.ArtistNature
import it.fast4x.riplay.musicvault.MusicVaultState
import kotlinx.serialization.json.Json

object Converters {

    @TypeConverter
    @JvmStatic
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    @JvmStatic
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotBlank() }
    }

    @TypeConverter
    @JvmStatic
    fun fromString(stringListString: String): List<String> {
        return stringListString.split(",").map { it }
    }

    @TypeConverter
    @JvmStatic
    fun toString(stringList: List<String>): String {
        return stringList.joinToString(separator = ",")
    }

    @TypeConverter
    @JvmStatic
    fun fromMusicVaultState(state: MusicVaultState): String = state.name

    @TypeConverter
    @JvmStatic
    fun toMusicVaultState(value: String): MusicVaultState =
        MusicVaultState.valueOf(value)

    // Configura Json per essere più flessibile e sicuro
    val RoomJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @TypeConverter
    @JvmStatic
    fun fromAlbumList(value: List<Album>): String {
        return RoomJson.encodeToString(value) // Serializza in JSON String
    }

    @TypeConverter
    @JvmStatic
    fun toAlbumList(value: String): List<Album> {
        return RoomJson.decodeFromString(value) // Deserializza da JSON String a Lista
    }

    @TypeConverter
    @JvmStatic
    fun fromExternalLinkList(value: List<ExternalLink>?): String? {
        return value?.let { RoomJson.encodeToString(it) }
    }

    @TypeConverter
    @JvmStatic
    fun toExternalLinkList(value: String?): List<ExternalLink>? {
        return value?.let { RoomJson.decodeFromString<List<ExternalLink>>(it) }
    }

    @TypeConverter
    @JvmStatic
    fun artistNatureToString(nature: ArtistNature?): String? = nature?.name

    @TypeConverter
    @JvmStatic
    fun stringToArtistNature(value: String?): ArtistNature? =
        value?.let { try { ArtistNature.valueOf(it) } catch (e: IllegalArgumentException) { ArtistNature.UNKNOWN } }

    @TypeConverter
    @JvmStatic
    fun albumNatureToString(nature: AlbumNature?): String? = nature?.name

    @TypeConverter
    @JvmStatic
    fun stringToAlbumNature(value: String?): AlbumNature? =
        value?.let { try { AlbumNature.valueOf(it) } catch (e: IllegalArgumentException) { AlbumNature.UNKNOWN } }
}
