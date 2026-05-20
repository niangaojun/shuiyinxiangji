package com.example.watermarkcamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.watermarkcamera.databinding.ActivityPhotoHistoryBinding
import com.example.watermarkcamera.databinding.ItemPhotoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoHistoryActivity : AppCompatActivity() {

    private var _binding: ActivityPhotoHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoHistoryAdapter: PhotoHistoryAdapter
    private lateinit var photoHistoryManager: PhotoHistoryManager

    private var isLoading = false
    private var currentPage = 0
    private var hasMoreData = true

    companion object {
        private const val PAGE_SIZE = 20
        private const val GRID_SPAN_COUNT = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityPhotoHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializeComponents()
        setupEventListeners()
        loadPhotos()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.photo_history)
        }
    }

    private fun initializeComponents() {
        photoHistoryManager = PhotoHistoryManager(this)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@PhotoHistoryActivity, GRID_SPAN_COUNT)
            photoHistoryAdapter = PhotoHistoryAdapter { photoFile ->
                openPhotoFullscreen(photoFile)
            }
            adapter = photoHistoryAdapter
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.error
        )

        binding.emptyView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
    }

    private fun setupEventListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshPhotos()
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1) && !isLoading && hasMoreData) {
                    loadMorePhotos()
                }
            }
        })

        binding.retryButton.setOnClickListener {
            loadPhotos()
        }
    }

    private fun loadPhotos() {
        if (isLoading) return

        isLoading = true
        currentPage = 0
        hasMoreData = true

        showLoadingState(true)

        lifecycleScope.launch {
            try {
                val photos = withContext(Dispatchers.IO) {
                    photoHistoryManager.getPhotos(currentPage, PAGE_SIZE)
                }
                hasMoreData = photos.size == PAGE_SIZE

                photoHistoryAdapter.setPhotos(photos)
                updateEmptyState(photos.isEmpty())
                showLoadingState(false)
            } catch (e: Exception) {
                showErrorState(true, "加载照片失败: ${e.message}")
                showLoadingState(false)
            } finally {
                isLoading = false
            }
        }
    }

    private fun loadMorePhotos() {
        if (isLoading || !hasMoreData) return

        isLoading = true
        currentPage++

        lifecycleScope.launch {
            try {
                val morePhotos = withContext(Dispatchers.IO) {
                    photoHistoryManager.getPhotos(currentPage, PAGE_SIZE)
                }
                hasMoreData = morePhotos.size == PAGE_SIZE

                photoHistoryAdapter.addPhotos(morePhotos)
            } catch (e: Exception) {
                Toast.makeText(this@PhotoHistoryActivity, "加载更多失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    private fun refreshPhotos() {
        currentPage = 0
        hasMoreData = true

        lifecycleScope.launch {
            try {
                val photos = withContext(Dispatchers.IO) {
                    photoHistoryManager.getPhotos(0, PAGE_SIZE)
                }
                hasMoreData = photos.size == PAGE_SIZE

                photoHistoryAdapter.setPhotos(photos)
                updateEmptyState(photos.isEmpty())
            } catch (e: Exception) {
                Toast.makeText(this@PhotoHistoryActivity, "刷新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun openPhotoFullscreen(photoFile: File) {
        val intent = Intent(this, FullscreenImageActivity::class.java).apply {
            putExtra("IMAGE_URI", Uri.fromFile(photoFile))
        }
        startActivity(intent)
    }

    private fun showLoadingState(show: Boolean) {
        binding.loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showErrorState(show: Boolean, message: String = "") {
        binding.errorView.visibility = if (show) View.VISIBLE else View.GONE
        if (show && message.isNotEmpty()) {
            binding.errorMessage.text = message
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

class PhotoHistoryManager(private val context: android.content.Context) {

    fun getPhotos(page: Int, pageSize: Int): List<File> {
        val photosDir = getPhotosDirectory()
        if (!photosDir.exists()) {
            return emptyList()
        }

        val photoFiles = photosDir.listFiles { file ->
            file.isFile && file.name.endsWith(".jpg", ignoreCase = true)
        }?.sortedByDescending { it.lastModified() }?.toTypedArray() ?: emptyArray()

        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, photoFiles.size)

        return if (startIndex < photoFiles.size) {
            photoFiles.sliceArray(startIndex until endIndex).toList()
        } else {
            emptyList()
        }
    }

    private fun getPhotosDirectory(): File {
        return File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "WatermarkPhotos")
    }
}

class PhotoHistoryAdapter(
    private val onPhotoClick: (File) -> Unit
) : RecyclerView.Adapter<PhotoHistoryAdapter.PhotoViewHolder>() {

    private val photos = mutableListOf<File>()
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    fun setPhotos(newPhotos: List<File>) {
        photos.clear()
        photos.addAll(newPhotos)
        notifyDataSetChanged()
    }

    fun addPhotos(newPhotos: List<File>) {
        val previousSize = photos.size
        photos.addAll(newPhotos)
        notifyItemRangeInserted(previousSize, newPhotos.size)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], dateFormat)
    }

    override fun getItemCount(): Int = photos.size

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoFile: File, dateFormat: SimpleDateFormat) {
            // 使用更高效的缩略图加载
            val bitmap = android.graphics.BitmapFactory.Options().run {
                inJustDecodeBounds = true
                android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath, this)

                val scaleFactor = maxOf(1, minOf(outWidth / 200, outHeight / 200))
                inJustDecodeBounds = false
                inSampleSize = scaleFactor

                android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath, this)
            }

            binding.imageView.setImageBitmap(bitmap)

            binding.root.setOnClickListener {
                onPhotoClick(photoFile)
            }

            binding.timeText.text = dateFormat.format(Date(photoFile.lastModified()))
        }
    }
}
