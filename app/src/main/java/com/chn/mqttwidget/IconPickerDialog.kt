package com.chn.mqttwidget

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Dialog with a searchable grid of every glyph in the Material Icons font.
 *
 * It is a dialog rather than an inline grid because the ~2200 icons need their
 * own scroll container: a grid inside the config screen's ScrollView cannot
 * scroll to the icons at the end of the list.
 */
object IconPickerDialog {

    private const val SPAN_COUNT = 5

    fun show(
        activity: AppCompatActivity,
        current: String,
        onSelected: (String) -> Unit,
    ) {
        val catalog = MaterialIconFont.catalog(activity)
        val content = LayoutInflater.from(activity).inflate(R.layout.dialog_icon_picker, null)
        val grid = content.findViewById<RecyclerView>(R.id.icon_grid)
        val search = content.findViewById<EditText>(R.id.input_icon_search)
        val empty = content.findViewById<TextView>(R.id.icon_empty)

        var dialog: AlertDialog? = null
        val adapter = IconAdapter(catalog, current) { name ->
            onSelected(name)
            dialog?.dismiss()
        }

        grid.layoutManager = GridLayoutManager(activity, SPAN_COUNT)
        grid.adapter = adapter

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                adapter.submit(s?.toString() ?: "")
                empty.visibility =
                    if (adapter.itemCount == 0) View.VISIBLE else View.GONE
            }
        })

        dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.icon_picker_title)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also {
                it.show()
                adapter.scrollToSelected(grid)
            }
    }

    private class IconAdapter(
        private val catalog: IconCatalog,
        private val selected: String,
        private val onSelected: (String) -> Unit,
    ) : RecyclerView.Adapter<IconAdapter.Holder>() {

        private var items: List<IconEntry> = catalog.entries

        fun submit(query: String) {
            items = catalog.search(query)
            notifyDataSetChanged()
        }

        /** Opens on the icon the widget is currently using. */
        fun scrollToSelected(grid: RecyclerView) {
            val index = items.indexOfFirst { it.name == selected }
            if (index > 0) grid.scrollToPosition(index)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_icon_choice, parent, false),
        )

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = items[position]
            MaterialIconFont.bind(holder.glyph, entry.name)
            holder.glyph.contentDescription = entry.name
            holder.glyph.setBackgroundResource(
                if (entry.name == selected) R.drawable.icon_selected_bg else R.drawable.icon_bg,
            )
            holder.glyph.setOnClickListener { onSelected(entry.name) }
        }

        class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val glyph: TextView = view.findViewById(R.id.icon_glyph)
        }
    }
}
