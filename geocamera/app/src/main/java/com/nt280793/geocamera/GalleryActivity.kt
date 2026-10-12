package com.nt280793.geocamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.nt280793.geocamera.databinding.ActivityGalleryBinding
import com.nt280793.geocamera.databinding.ItemPhotoBinding
import kotlin.concurrent.thread

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private val adapter = PhotoAdapter { uri ->
        startActivity(Intent(this, PhotoDetailActivity::class.java).setData(uri))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.grid.layoutManager = GridLayoutManager(this, 3)
        binding.grid.adapter = adapter
        binding.grid.addItemDecoration(GapDecoration(resources.displayMetrics.density.times(2).toInt()))
        binding.pdfButton.setOnClickListener { exportPdf() }
    }

    private fun exportPdf() {
        val photos = adapter.current()
        if (photos.isEmpty()) {
            Toast.makeText(this, R.string.pdf_empty, Toast.LENGTH_SHORT).show()
            return
        }
        binding.pdfButton.isEnabled = false
        thread(name = "pdf") {
            val file = PhotoAlbumPdf.write(this, photos)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                binding.pdfButton.isEnabled = true
                if (file == null) {
                    Toast.makeText(this, R.string.pdf_failed, Toast.LENGTH_LONG).show()
                } else {
                    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(share, getString(R.string.pdf_ready)))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.emptyText.setText(R.string.loading)
        binding.emptyText.visibility = View.VISIBLE
        thread(name = "gallery-load") {
            val items = PhotoStore.list(this)
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                adapter.submit(items)
                binding.emptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                binding.emptyText.setText(R.string.empty_gallery)
            }
        }
    }

    private class PhotoAdapter(
        private val onClick: (Uri) -> Unit
    ) : RecyclerView.Adapter<PhotoAdapter.Holder>() {
        private val items = mutableListOf<Uri>()

        fun submit(next: List<Uri>) {
            items.clear()
            items.addAll(next)
            notifyDataSetChanged()
        }

        fun current(): List<Uri> = items.toList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            val cell = parent.measuredWidth / 3
            if (cell > 0) binding.root.layoutParams.height = cell
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val uri = items[position]
            holder.binding.thumb.load(uri) {
                crossfade(true)
                placeholder(R.drawable.bg_surface)
                error(R.drawable.bg_surface)
            }
            holder.binding.thumb.setOnClickListener { onClick(uri) }
        }

        override fun getItemCount(): Int = items.size

        class Holder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root)
    }

    private class GapDecoration(private val gap: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.set(gap, gap, gap, gap)
        }
    }
}
