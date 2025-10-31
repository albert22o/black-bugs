package com.example.bugs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment

class AuthorsTab : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tab_authors, container, false)

        val listViewAuthors: ListView = view.findViewById(R.id.listViewAuthors)
        val authors = getAuthorsList()

        val adapter = object : ArrayAdapter<Author>(
            requireContext(),
            R.layout.item_author,
            R.id.textViewAuthorName,
            authors
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_author, parent, false)

                val author = getItem(position)
                val imageViewAuthor = view.findViewById<android.widget.ImageView>(R.id.imageViewAuthor)
                val textViewAuthorName = view.findViewById<android.widget.TextView>(R.id.textViewAuthorName)

                textViewAuthorName.text = author?.name
                author?.photoResId?.let {
                    imageViewAuthor.setImageResource(it)
                }

                return view
            }
        }

        listViewAuthors.adapter = adapter

        return view
    }

    private fun getAuthorsList(): List<Author> {
        return listOf(
            Author("Оганесян Альберт Самвелович", R.drawable.ic_person),
            Author("Лацук Андрей Юрич", R.drawable.ic_person),
        )
    }
}