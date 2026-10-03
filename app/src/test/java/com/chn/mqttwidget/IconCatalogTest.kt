package com.chn.mqttwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class IconCatalogTest {

    private val sample = """
        10k e951
        coffee efef
        lightbulb e0f0
        power_settings_new e8ac

        broken zzzz
        garage f011
    """.trimIndent()

    private val catalog = IconCatalog.parse(sample)

    @Test
    fun `parse reads name and hex codepoint and skips junk lines`() {
        assertEquals(
            listOf(
                IconEntry("10k", 0xe951),
                IconEntry("coffee", 0xefef),
                IconEntry("lightbulb", 0xe0f0),
                IconEntry("power_settings_new", 0xe8ac),
                IconEntry("garage", 0xf011),
            ),
            catalog.entries,
        )
        assertEquals("\uEFEF", catalog.entries.first { it.name == "coffee" }.glyph)
    }

    @Test
    fun `unknown icon names fall back to the default glyph`() {
        // Legacy configs store drawable names like "ic_power".
        assertEquals(IconCatalog.DEFAULT_ICON, catalog.normalize("ic_power"))
        assertFalse(catalog.isKnown("ic_power"))
        assertEquals(
            catalog.glyphFor(IconCatalog.DEFAULT_ICON),
            catalog.glyphFor("ic_power"),
        )

        assertEquals("coffee", catalog.normalize("coffee"))
        assertTrue(catalog.isKnown("coffee"))
    }

    @Test
    fun `search matches substrings with prefix matches first`() {
        assertEquals(catalog.entries, catalog.search(""))
        assertEquals(catalog.entries, catalog.search("   "))

        val hits = catalog.search("light")
        assertEquals(listOf("lightbulb"), hits.map { it.name })

        val prefixFirst = catalog.search("coffee")
        assertEquals(listOf("coffee"), prefixFirst.map { it.name })

        assertTrue(catalog.search("xyz").isEmpty())
    }

    @Test
    fun `shipped catalog lists the default icon and over 1000 icons`() {
        val file = listOf(
            "src/main/assets/fonts/material_icons.codepoints",
            "app/src/main/assets/fonts/material_icons.codepoints",
        ).map(::File).firstOrNull { it.exists() }
        assumeTrue("material_icons.codepoints not found from ${File("").absolutePath}", file != null)

        val shipped = file!!.readText().let(IconCatalog::parse)
        assertTrue("expected >1000 icons, got ${shipped.entries.size}", shipped.entries.size > 1000)
        assertTrue(shipped.isKnown(IconCatalog.DEFAULT_ICON))
    }
}
