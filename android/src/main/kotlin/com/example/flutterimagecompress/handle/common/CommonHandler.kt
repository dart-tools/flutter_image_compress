package com.example.flutterimagecompress.handle.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.flutterimagecompress.exif.ExifKeeper
import com.example.flutterimagecompress.ext.calcScale
import com.example.flutterimagecompress.ext.compress
import com.example.flutterimagecompress.ext.rotate
import com.example.flutterimagecompress.handle.FormatHandler
import com.example.flutterimagecompress.logger.log
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class CommonHandler(override val type: Int) : FormatHandler {

  override val typeName: String
    get() {
      return when (type) {
        1 -> "png"
        3 -> "webp"
        else -> "jpeg"
      }
    }

  private val bitmapFormat: Bitmap.CompressFormat
    get() {
      return when (type) {
        1 -> Bitmap.CompressFormat.PNG
        3 -> Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.JPEG
      }
    }

  override fun handleByteArray(context: Context, byteArray: ByteArray, outputStream: OutputStream, width: Int?, height: Int?, quality: Int, rotate: Int, keepExif: Boolean, inSampleSize: Int) {
    val result = compress(byteArray, width, height, quality, rotate, inSampleSize)

    if (keepExif && bitmapFormat == Bitmap.CompressFormat.JPEG) {
      val byteArrayOutputStream = ByteArrayOutputStream()
      byteArrayOutputStream.write(result)
      val resultStream = ExifKeeper(byteArray).writeToOutputStream(
              context,
              byteArrayOutputStream
      )
      outputStream.write(resultStream.toByteArray())
    } else {
      outputStream.write(result)
    }

  }

  private fun compress(arr: ByteArray, width: Int?, height: Int?, quality: Int, rotate: Int = 0, inSampleSize: Int): ByteArray {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = false
    options.inPreferredConfig = Bitmap.Config.RGB_565
    options.inSampleSize = inSampleSize
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
      @Suppress("DEPRECATION")
      options.inDither = true
    }

    val bitmap = BitmapFactory.decodeByteArray(arr, 0, arr.count(), options)
    val outputStream = ByteArrayOutputStream()

    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()

    log("src width = $w")
    log("src height = $h")

    log("input width = $width")
    log("input height = $height")

    val newHeight = height ?: ((h / w) * width!!).toInt()
    val newWidth = width ?: ((w / h) * newHeight).toInt()

    var scale = bitmap.calcScale(newWidth, newHeight)

    log("scale = $scale")

    var destW = w / scale
    var destH = h / scale


    log("dst width = $destW")
    log("dst height = $destH")

    var result = Bitmap.createScaledBitmap(bitmap, destW.toInt(), destH.toInt(), true)
            .rotate(rotate)
    if(newWidth.toFloat() / newHeight.toFloat() != w / h) {
      var targetX = ((destW - newWidth) / 2).toInt();
      var targetY = ((destH - newHeight) / 2).toInt();
      result = Bitmap.createBitmap(result, targetX, targetY, newWidth, newHeight)
    }
    result.compress(bitmapFormat, quality, outputStream)

    return outputStream.toByteArray()
  }


  override fun handleFile(context: Context, path: String, outputStream: OutputStream, width: Int?, height: Int?, quality: Int, rotate: Int, keepExif: Boolean, inSampleSize: Int,numberOfRetries:Int) {
    try{
      if(numberOfRetries <= 0)return;
      val options = BitmapFactory.Options()
      options.inJustDecodeBounds = false
      options.inPreferredConfig = Bitmap.Config.RGB_565
      options.inSampleSize = inSampleSize
      if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
        @Suppress("DEPRECATION")
        options.inDither = true
      }
      var bitmap = BitmapFactory.decodeFile(path, options)

      val w = bitmap.width.toFloat()
      val h = bitmap.height.toFloat()

      
      log("src width = $w")
      log("src height = $h")

      log("input width = $width")
      log("input height = $height")

      var intendedWidth = width?.toInt()
      var intendedHeight = height?.toInt()
      if(intendedWidth != null && intendedWidth > w) {
        intendedWidth = w.toInt()
        if(intendedHeight != null) {
          intendedHeight = ((height!!.toFloat() / width!!.toFloat()) * intendedWidth!!).toInt()
        }
      }
      if(intendedHeight != null && intendedHeight > h) {
        intendedHeight = h.toInt()
        if(intendedWidth != null) {
          intendedWidth = ((width!!.toFloat() / height!!.toFloat()) * intendedHeight!!).toInt()
        }
      }

      log("intendedWidth = $intendedWidth")
      log("intendedHeight = $intendedHeight")

      val newHeight = intendedHeight ?: ((h / w) * intendedWidth!!).toInt()
      val newWidth = intendedWidth ?: ((w / h) * newHeight).toInt()

      var scale = bitmap.calcScale(newWidth, newHeight)

      var destW = w / scale
      var destH = h / scale

      log("dst width = $destW")
      log("dst height = $destH")

      if(newWidth.toFloat() / newHeight.toFloat() != w / h) {
        var targetX = (scale * (destW - newWidth) / 2).toInt();
        var targetY = (scale * (destH - newHeight) / 2).toInt();

        log("targetX = $targetX")
        log("targetY = $targetY")
        
        bitmap = Bitmap.createBitmap(bitmap, targetX, targetY, (scale * newWidth).toInt(), (scale * newHeight).toInt())
      }

      val array = bitmap.compress(newWidth, newHeight, quality, rotate, type)

      if (keepExif && bitmapFormat == Bitmap.CompressFormat.JPEG) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.write(array)
        val tmpOutputStream = ExifKeeper(path).writeToOutputStream(
                context,
                byteArrayOutputStream
        )
        outputStream.write(tmpOutputStream.toByteArray())
      } else {
        outputStream.write(array)
      }
    }catch (e:OutOfMemoryError){//handling out of memory error and increase samples size
      System.gc();
      handleFile(context, path, outputStream, width, height, quality, rotate, keepExif, inSampleSize *2,numberOfRetries-1);
    }
  }
}
