package com.example.flutterimagecompress.handle.heif

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.heifwriter.HeifWriter
import com.example.flutterimagecompress.ext.calcScale
import com.example.flutterimagecompress.ext.rotate
import com.example.flutterimagecompress.handle.FormatHandler
import com.example.flutterimagecompress.logger.log
import com.example.flutterimagecompress.util.TmpFileUtil
import java.io.OutputStream

class HeifHandler : FormatHandler {

  override val type: Int
    get() = 2

  override val typeName: String
    get() = "heif"

  override fun handleByteArray(context: Context, byteArray: ByteArray, outputStream: OutputStream, width: Int?, height: Int?, quality: Int, rotate: Int, keepExif: Boolean, inSampleSize: Int) {
    val tmpFile = TmpFileUtil.createTmpFile(context)
    compress(byteArray, width, height, quality, rotate, inSampleSize, tmpFile.absolutePath)
    outputStream.write(tmpFile.readBytes())
  }

  private fun compress(arr: ByteArray, width: Int?, height: Int?, quality: Int, rotate: Int = 0, inSampleSize: Int, targetPath: String) {
    val options = makeOption(inSampleSize)
    val bitmap = BitmapFactory.decodeByteArray(arr, 0, arr.count(), options)
    convertToHeif(bitmap, width, height, rotate, targetPath, quality)
  }

  private fun compress(path: String, width: Int?, height: Int?, quality: Int, rotate: Int = 0, inSampleSize: Int, targetPath: String) {
    val options = makeOption(inSampleSize)
    val bitmap = BitmapFactory.decodeFile(path, options)
    convertToHeif(bitmap, width, height, rotate, targetPath, quality)
  }

  private fun makeOption(inSampleSize: Int): BitmapFactory.Options {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = false
    options.inPreferredConfig = Bitmap.Config.RGB_565
    options.inSampleSize = inSampleSize
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      @Suppress("DEPRECATION")
      options.inDither = true
    }
    return options
  }

  private fun convertToHeif(bitmap: Bitmap, width: Int?, height: Int?, rotate: Int, targetPath: String, quality: Int) {
    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()

    log("src width = $w")
    log("src height = $h")

    val newHeight = height ?: ((h / w) * width!!).toInt()
    val newWidth = width ?: ((w / h) * newHeight).toInt()

    val scale = bitmap.calcScale(newWidth, newHeight)

    val destW = w / scale
    val destH = h / scale

    log("dst width = $destW")
    log("dst height = $destH")

    var result = Bitmap.createScaledBitmap(bitmap, destW.toInt(), destH.toInt(), true)
            .rotate(rotate)

    if(newWidth.toFloat() / newHeight.toFloat() != w / h) {
      var targetY = ((destH - newHeight) / 2).toInt();
      var targetX = ((destW - newWidth) / 2).toInt();
      result = Bitmap.createBitmap(result, targetX, targetY, newWidth, newHeight)
    }

    val heifWriter = HeifWriter.Builder(targetPath, result.width, result.height, HeifWriter.INPUT_MODE_BITMAP)
            .setQuality(quality)
            .setMaxImages(1)
            .build()

    heifWriter.start()
    heifWriter.addBitmap(result)
    heifWriter.stop(5000)

    heifWriter.close()
  }

  override fun handleFile(context: Context, path: String, outputStream: OutputStream, width: Int?, height: Int?, quality: Int, rotate: Int, keepExif: Boolean, inSampleSize: Int,numberOfRetries:Int) {
    val tmpFile = TmpFileUtil.createTmpFile(context)
    compress(path, width, height, quality, rotate, inSampleSize, tmpFile.absolutePath)
    outputStream.write(tmpFile.readBytes())
  }
}