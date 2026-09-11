package com.imagingutils.data

import java.io.File
import java.util.prefs.Preferences

/** Lightweight, cross-restart storage for user preferences (currently the last opened folder). */
private val prefs: Preferences = Preferences.userNodeForPackage(AppSettings::class.java)

private const val KEY_LAST_FOLDER = "lastFolder"

/** Marker class used to scope the [Preferences] node. */
private class AppSettings

/** Returns the last opened folder if it was saved and still exists as a directory, otherwise null. */
fun loadLastFolder(): File? {
    val path = prefs.get(KEY_LAST_FOLDER, null) ?: return null
    val dir = File(path)
    return if (dir.isDirectory) dir else null
}

/** Persists the last opened folder path so it can be restored on the next launch. */
fun saveLastFolder(folder: File) {
    prefs.put(KEY_LAST_FOLDER, folder.absolutePath)
}
