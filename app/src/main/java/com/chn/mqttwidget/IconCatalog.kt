package com.chn.mqttwidget

/**
 * One glyph of the Material Icons font: its ligature name ("garage") and the
 * unicode codepoint (0xf011) the font maps it to.
 */
data class IconEntry(val name: String, val codepoint: Int) {

    /** The single character string to draw with the icon font. */
    val glyph: String = String(Character.toChars(codepoint))
}

/**
 * The searchable icon catalog, parsed from the codepoint list that ships next
 * to the font (assets/fonts/material_icons.codepoints, one "name hex" pair per
 * line). Pure Kotlin on purpose so it can be unit tested on the JVM.
 */
class IconCatalog(val entries: List<IconEntry>) {

    private val byName: Map<String, IconEntry> = entries.associateBy { it.name }

    /** Known icon names, or the default when the stored one is unknown. */
    fun normalize(name: String): String =
        if (byName.containsKey(name)) name else DEFAULT_ICON

    fun isKnown(name: String): Boolean = byName.containsKey(name)

    /** Falls back to [DEFAULT_ICON] for unknown names (e.g. legacy "ic_power"). */
    fun glyphFor(name: String): String = (byName[name] ?: byName[DEFAULT_ICON])?.glyph ?: ""

    /**
     * Substring search over the icon names, prefix matches first:
     * "light" finds "lightbulb", "highlight", "light_mode".
     */
    fun search(query: String): List<IconEntry> {
        val needle = query.trim()
        if (needle.isEmpty()) return entries
        val prefix = mutableListOf<IconEntry>()
        val infix = mutableListOf<IconEntry>()
        for (entry in entries) {
            val index = entry.name.indexOf(needle, ignoreCase = true)
            when {
                index == 0 -> prefix.add(entry)
                index > 0 -> infix.add(entry)
            }
        }
        return prefix + infix
    }

    companion object {
        const val DEFAULT_ICON = "power_settings_new"

        fun parse(text: String): IconCatalog = IconCatalog(
            text.lineSequence()
                .mapNotNull { line ->
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size != 2) return@mapNotNull null
                    val codepoint = parts[1].toIntOrNull(16) ?: return@mapNotNull null
                    IconEntry(parts[0], codepoint)
                }
                .toList(),
        )
    }
}
