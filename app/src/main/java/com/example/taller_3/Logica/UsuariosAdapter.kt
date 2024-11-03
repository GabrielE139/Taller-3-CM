package com.example.taller_3.Logica

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CursorAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.taller_3.R
import com.google.firebase.storage.FirebaseStorage

class UsuariosAdapter(context: Context, cursor: Cursor) : CursorAdapter(context, cursor, 0) {
    private val storageReference = FirebaseStorage.getInstance().reference

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.layout_usuarios, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val nombreTextView = view.findViewById<TextView>(R.id.nombre)
        val apellidoTextView = view.findViewById<TextView>(R.id.apellido)
        val imagenView = view.findViewById<ImageView>(R.id.imageView)
        val boton = view.findViewById<Button>(R.id.button)

        // Obtén los datos del cursor
        val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
        val apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellido"))
        val email = cursor.getString(cursor.getColumnIndexOrThrow("email"))
        val longitud = cursor.getString(cursor.getColumnIndexOrThrow("longitud"))
        val latitud = cursor.getString(cursor.getColumnIndexOrThrow("latitud"))
        val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("imagen"))

        // Asigna los datos a las vistas de texto
        nombreTextView.text = nombre
        apellidoTextView.text = apellido

        // Configura el Intent para el botón
        val irUbicacion = Intent(context, UbicacionUsuarios::class.java)
        val bundle = Bundle()
        bundle.putString("email", email)
        bundle.putString("longitud", longitud)
        bundle.putString("latitud", latitud)
        irUbicacion.putExtra("bundle", bundle)
        boton.setOnClickListener {
            context.startActivity(irUbicacion)
        }

        // Usa Glide para cargar la imagen en el ImageView
            Glide.with(context)
                .load(imageUrl)  // Carga la URL de descarga de Firebase
               // .placeholder(R.drawable.placeholder)  // Imagen de marcador de posición mientras carga
              //  .error(R.drawable.error)  // Imagen de error si falla la carga
                .into(imagenView)  // Asigna la imagen al ImageView

    }
}