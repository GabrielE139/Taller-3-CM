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
import com.example.taller_3.R

class UsuariosAdapter (context: Context, cursor: Cursor) : CursorAdapter(context, cursor, 0) {
    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.layout_usuarios, parent, false)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val nombreTextView = view.findViewById<TextView>(R.id.nombre)
        val apellidoTextView = view.findViewById<TextView>(R.id.apellido)
        val imagenView = view.findViewById<ImageView>(R.id.imageView)
        val boton = view.findViewById<Button>(R.id.button)

        val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
        val apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellido"))
        val email = cursor.getString(cursor.getColumnIndexOrThrow("email"))
        val longitud = cursor.getString(cursor.getColumnIndexOrThrow("longitud"))
        val latitud = cursor.getString(cursor.getColumnIndexOrThrow("latitud"))

        nombreTextView.text = nombre
        apellidoTextView.text = apellido

        val irUbicacion = Intent(context, UbicacionUsuarios::class.java)
        val bundle = Bundle()
        bundle.putString("email", email)
        bundle.putString("longitud", longitud)
        bundle.putString("latitud", latitud)
        irUbicacion.putExtra("bundle", bundle)
        boton.setOnClickListener{
            context.startActivity(irUbicacion)
        }
    }
}