package com.odom.motivationlocker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface

// 설정화면에 배경 사진을 "작게" 미리보여주기 위한 썸네일 로더.
// 원본 해상도를 그대로 디코딩하면 큰 사진 선택 시 메모리를 낭비하므로
// inSampleSize로 먼저 다운샘플링한 뒤 EXIF 방향을 보정한다.
object PhotoThumbnailLoader {

    fun loadThumbnail(context: Context, uri: Uri, maxSizePx: Int): Bitmap? {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSizePx)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            applyOrientation(sampledBitmap, orientation)
        } catch (e: Exception) {
            Log.w("PhotoThumbnailLoader", "썸네일을 불러오지 못함", e)
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSizePx: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxSizePx && height / (sampleSize * 2) >= maxSizePx) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
