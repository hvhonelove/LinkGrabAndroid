package com.example.linkgrab

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Only our own MediaStore URIs are stored; no broad media permission is needed. */
data class SavedVideo(
    val uri: String,
    val title: String,
    val source: String,
    val bytes: Long,
    val savedAt: Long,
    val quality: Int
)

object DownloadHistory {
    private const val KEY = "saved_videos"

    @Synchronized
    fun read(context: Context): List<SavedVideo> = runCatching {
        val array = JSONArray(context.getSharedPreferences("history", Context.MODE_PRIVATE)
            .getString(KEY, "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            SavedVideo(item.getString("uri"), item.getString("title"),
                item.optString("source"), item.optLong("bytes"),
                item.optLong("savedAt"), item.optInt("quality"))
        }
    }.getOrDefault(emptyList())

    @Synchronized
    fun add(context: Context, video: SavedVideo) {
        val items = (listOf(video) + read(context)).distinctBy { it.uri }.take(200)
        val array = JSONArray()
        items.forEach {
            array.put(JSONObject().put("uri", it.uri).put("title", it.title)
                .put("source", it.source).put("bytes", it.bytes)
                .put("savedAt", it.savedAt).put("quality", it.quality))
        }
        context.getSharedPreferences("history", Context.MODE_PRIVATE).edit()
            .putString(KEY, array.toString()).apply()
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences("history", Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
