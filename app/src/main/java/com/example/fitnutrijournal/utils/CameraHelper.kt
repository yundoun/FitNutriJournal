package com.example.fitnutrijournal.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.fitnutrijournal.viewmodel.PhotoViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraHelper(
    private val fragment: Fragment,
    private val imageView: ImageView,
    private val imageSampleLayout: View,
    private val photoViewModel: PhotoViewModel,
    private val currentDate: String,
    private val mealType: String
) {

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2
    private val REQUEST_IMAGE_CROP = 3
    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    // 카메라 앱 실행해서 사진 촬영
    @SuppressLint("QueryPermissionsNeeded")
    fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(fragment.requireActivity().packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    fragment.requireContext(),
                    "com.example.fitnutrijournal.fileprovider",
                    it
                )
                photoUri = photoURI
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                fragment.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    // 임시 파일 생성
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File =
            fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    // 사진 촬영 완료되면 호출됨. photoUri를 cropImage()에 전달
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    photoUri?.let { cropImage(it) }
                }

                REQUEST_IMAGE_PICK -> {
                    data?.data?.let { uri ->
                        currentPhotoPath = uri.toString()
                        cropImage(uri)
                    }
                }

                REQUEST_IMAGE_CROP -> {
                    currentPhotoPath?.let {
                        photoViewModel.addPhoto(currentDate, mealType, it)
                        setPicWithObserver(imageView, it)
                        imageSampleLayout.visibility = View.GONE
                        imageView.visibility = View.VISIBLE
                        Toast.makeText(
                            fragment.requireContext(),
                            "사진이 저장되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // 크롭된 이미지를 저장하고 이미지뷰에 표시
    @SuppressLint("QueryPermissionsNeeded")
    private fun cropImage(uri: Uri) {
        try {
            val cropIntent = Intent("com.android.camera.action.CROP")
            cropIntent.setDataAndType(uri, "image/*")
            cropIntent.putExtra("crop", "true")
            cropIntent.putExtra("aspectX", 1)
            cropIntent.putExtra("aspectY", 1)
            cropIntent.putExtra("outputX", 512)
            cropIntent.putExtra("outputY", 512)
            cropIntent.putExtra("scale", true)
            cropIntent.putExtra("noFaceDetection", true)
            cropIntent.putExtra("return-data", false)

            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir: File =
                fragment.requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val file = File.createTempFile("CROP_${timeStamp}_", ".jpg", storageDir)
            currentPhotoPath = file.absolutePath
            val photoURI: Uri = FileProvider.getUriForFile(
                fragment.requireContext(),
                "com.example.fitnutrijournal.fileprovider",
                file
            )

            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            val resInfoList = fragment.requireContext().packageManager.queryIntentActivities(
                cropIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                fragment.requireContext().grantUriPermission(
                    packageName,
                    photoURI,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            fragment.startActivityForResult(cropIntent, REQUEST_IMAGE_CROP)
        } catch (e: Exception) {
            Toast.makeText(fragment.requireContext(), "이미지 크롭 작업을 지원하지 않습니다.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun setPicWithObserver(imageView: ImageView, photoPath: String) {
        val observer = imageView.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (observer.isAlive) {
                    observer.removeOnGlobalLayoutListener(this)
                    setPic(imageView, photoPath)
                }
            }
        })
    }

    private fun setPic(imageView: ImageView, photoPath: String) {
        // Get the dimensions of the View
        val targetW: Int = imageView.width
        val targetH: Int = imageView.height

        // Get the dimensions of the bitmap
        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(photoPath, this)
            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = if (targetW > 0 && targetH > 0) {
                (photoW / targetW).coerceAtMost(photoH / targetH)
            } else {
                1
            }

            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888 // Set preferred config to improve quality
        }

        BitmapFactory.decodeFile(photoPath, bmOptions)?.also { bitmap ->
            imageView.setImageBitmap(bitmap)
        }
    }


    fun dispatchPickPictureIntent() {
        val pickPictureIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        fragment.startActivityForResult(pickPictureIntent, REQUEST_IMAGE_PICK)
    }
}
