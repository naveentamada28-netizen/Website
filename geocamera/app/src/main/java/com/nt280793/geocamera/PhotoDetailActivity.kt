package com.nt280793.geocamera

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nt280793.geocamera.databinding.ActivityPhotoBinding

class PhotoDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoBinding
    private var photoLat: Double? = null
    private var photoLon: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }
        binding.toolbar.title = PhotoStore.displayName(this, uri)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.photo.load(uri) { crossfade(true) }
        binding.meta.text = readMeta(uri)
        binding.meta.setOnLongClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("location", binding.meta.text))
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
            true
        }
        binding.shareButton.setOnClickListener { share(uri) }
        binding.mapsButton.setOnClickListener { openMaps() }
        binding.deleteButton.setOnClickListener { confirmDelete(uri) }
        val hasGps = photoLat != null && photoLon != null
        binding.mapsButton.isEnabled = hasGps
        binding.mapsButton.alpha = if (hasGps) 1f else 0.4f
    }

    private fun readMeta(uri: Uri): String {
        val exif = try {
            contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        } catch (_: Exception) {
            null
        } ?: return getString(R.string.no_metadata)
        val latLong = exif.latLong
        if (latLong != null && latLong.size >= 2) {
            photoLat = latLong[0]
            photoLon = latLong[1]
        }
        return PhotoCaption.lines(this, uri).joinToString("\n")
    }

    private fun share(uri: Uri) {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("photo", uri)
        }
        startActivity(Intent.createChooser(share, getString(R.string.share)))
    }

    private fun openMaps() {
        val lat = photoLat ?: return
        val lon = photoLon ?: return
        val query = Uri.encode("$lat,$lon")
        val map = Uri.parse("geo:$lat,$lon?q=$query")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, map))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.no_maps, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(uri: Uri) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_photo)
            .setMessage(R.string.delete_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                PhotoStore.delete(this, uri)
                CaptureIndex(this).remove(uri)
                finish()
            }
            .show()
    }
}
