package com.example.auth2type

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.lang.Exception
import java.util.concurrent.Executors



fun ImageView.loadImage(url: String) {
    val executor = Executors.newSingleThreadExecutor()
    val handler = Handler(Looper.getMainLooper())

    executor.execute {
        try {
            val imageStream = java.net.URL(url).openStream()
            val imageBitmap = BitmapFactory.decodeStream(imageStream)
            handler.post {
                this.setImageBitmap(imageBitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}