package com.vision.wallpapers.ui

import android.Manifest
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import coil.api.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textview.MaterialTextView
import com.ramotion.circlemenu.CircleMenuView
import com.vision.wallpapers.R
import com.vision.wallpapers.WallpaperViewModel
import com.vision.wallpapers.WallpaperViewModelFactory
import com.vision.wallpapers.database.WallpaperDatabase
import com.vision.wallpapers.databinding.ActivityFullImageBinding
import com.vision.wallpapers.model.alphaCoder.AlphaPhotoResponseItem
import com.vision.wallpapers.repository.WallpaperRepo
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File


class FullImageActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {

    lateinit var binding: ActivityFullImageBinding
    lateinit var viewModel: WallpaperViewModel
    lateinit var url: String
    lateinit var type: String
    lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private val STRG_PERM = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullImageBinding.inflate(layoutInflater)
        setTheme(R.style.FullImage)
        setContentView(binding.root)

        val db = WallpaperDatabase(this)
        val repo = WallpaperRepo(db)
        val factory = WallpaperViewModelFactory(repo)

        viewModel = ViewModelProvider(this, factory).get(WallpaperViewModel::class.java)

        val bottomSheet = findViewById<LinearLayout>(R.id.bottomInfo)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        val cancel = findViewById<MaterialTextView>(R.id.cancel)

        cancel.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        val photo: AlphaPhotoResponseItem =
            intent.getSerializableExtra("photo") as AlphaPhotoResponseItem
        url = photo.url_image
        type = photo.file_type
        binding.apply {
            wallpaperIv.load(url) {
                crossfade(true)
                allowHardware(true)
            }
            scaleBtn.setOnClickListener {
                adjustZoom()
            }
            rotateBtn.setOnClickListener {
                changeScreenOrientation()
            }
            backBtn.setOnClickListener {
                onBackPressed()
            }
        }


        val menu = binding.circle
        menu.eventListener = object : CircleMenuView.EventListener() {
            override fun onMenuOpenAnimationStart(view: CircleMenuView) {

            }

            override fun onMenuOpenAnimationEnd(view: CircleMenuView) {

            }

            override fun onMenuCloseAnimationStart(view: CircleMenuView) {
            }

            override fun onMenuCloseAnimationEnd(view: CircleMenuView) {

            }

            override fun onButtonClickAnimationStart(view: CircleMenuView, index: Int) {

            }

            override fun onButtonClickAnimationEnd(view: CircleMenuView, index: Int) {

                when (index) {
                    0 -> {
                        requestPermission()
                    }
                    1 -> {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                    2 -> {
                        Toast.makeText(applicationContext, "Saved", Toast.LENGTH_SHORT).show()
                        viewModel.saveWallpaper(photo)

                    }
                    3 -> {
                        cropImage()
                    }

                }
            }
        }
    }

    private fun changeScreenOrientation() {
        val orientation: Int = this.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        if (Settings.System.getInt(
                contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0
            ) === 1
        ) {
            val handler = Handler()
            handler.postDelayed(Runnable {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }, 4000)
        }
    }

    private fun adjustZoom() {
        binding.apply {
            if (wallpaperIv.scaleType == ImageView.ScaleType.CENTER_CROP) {
                scaleBtn.setImageResource(R.drawable.ic_maximize)
                wallpaperIv.scaleType = ImageView.ScaleType.FIT_CENTER
            } else {
                scaleBtn.setImageResource(R.drawable.ic_minimize)
                wallpaperIv.scaleType = ImageView.ScaleType.CENTER_CROP
                wallpaperIv.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun downloadImageNew(filename: String = "Vision", downloadUrlOfImage: String) {
        try {
            val dm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            } else {
                TODO("VERSION.SDK_INT < M")
            }
            val downloadUri: Uri = Uri.parse(downloadUrlOfImage)
            val request = DownloadManager.Request(downloadUri)
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(filename)
                .setMimeType(type)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_PICTURES,
                    File.separator.toString() + filename + ".jpg"
                )
            dm.enqueue(request)
            Toast.makeText(this, "Image download started.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Image download failed.", Toast.LENGTH_SHORT).show()
        }
    }

    private  fun cropImage(){
        val uri = Uri.parse(url)
        val option = UCrop.Options()
        option.setAspectRatioOptions(
            2,
            AspectRatio("1:2", 1f, 2f),
            AspectRatio("3:4", 3f, 4f),
            AspectRatio("Default", 9f, 16f),
            AspectRatio("4:3", 4f, 3f),
            AspectRatio("2:1", 2f, 1f)
        )
        UCrop.of(uri, Uri.fromFile(File(cacheDir, "Vision")))
            .withOptions(option)
            .start(this)
    }

    private fun hasStoragePermission(): Boolean {
        return EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun requestPermission() {
        if (hasStoragePermission()) {
            downloadImageNew(downloadUrlOfImage = url)
        } else {
            EasyPermissions.requestPermissions(
                this,
                "This app needs access to your storage so you can download wallpapers",
                STRG_PERM,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        downloadImageNew("Vision", url)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
        Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (hasStoragePermission()) {
                downloadImageNew(downloadUrlOfImage = url)
            } else {
                Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = data?.let { UCrop.getOutput(it) }
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri)
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            wallpaperManager.setBitmap(bitmap)
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = data?.let { UCrop.getError(it) }
            Toast.makeText(applicationContext, "Something went wrong.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRationaleAccepted(requestCode: Int) {

    }

    override fun onRationaleDenied(requestCode: Int) {
        Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
    }



}