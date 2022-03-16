package com.example.flutterimagecompress.handle

import android.content.Context
import java.io.OutputStream

interface FormatHandler {

  val type: Int

  val typeName: String

  fun handleByteArray(context: Context, byteArray: ByteArray, outputStream: OutputStream, width: Int?, height: Int?, quality: Int, rotate: Int, keepExif: Boolean, inSampleSize: Int)

  fun handleFile(context: Context, path: String, outputStream: OutputStream, width: Int?, height: Int?, quality: Int, rotate: Int, keepExif: Boolean, inSampleSize: Int,numberOfRetries:Int)
}