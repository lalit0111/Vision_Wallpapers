package com.vision.wallpapers

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.vision.wallpapers.databinding.PictureCardBinding
import com.vision.wallpapers.model.Response
import com.vision.wallpapers.model.alphaCoder.AlphaPhotoResponseItem
import com.vision.wallpapers.util.Palette
import kotlinx.coroutines.launch


class Adapter(private val viewModel: WallpaperViewModel):RecyclerView.Adapter<Adapter.ViewHolder>() {

    class ViewHolder(var binding: PictureCardBinding) : RecyclerView.ViewHolder(binding.root)

    private var onItemClickListener: ((View, String, AlphaPhotoResponseItem) -> Unit)? = null
    private var onSaveClickListener: ((AlphaPhotoResponseItem) -> Unit)? = null

    var count = 0

    fun setOnItemClickListener(listener: (View, String, AlphaPhotoResponseItem) -> Unit) {
        onItemClickListener = listener
    }

    fun setSaveOnClickListener(listener: (AlphaPhotoResponseItem) -> Unit) {
        onSaveClickListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PictureCardBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = differ.currentList[position]
        var isSaved = false
        holder.apply {

            val thumbnailRequest: RequestBuilder<Drawable> = Glide
                .with(itemView.context)
                .load(photo.urlThumb)

            Glide.with(itemView.context)
                .load(photo.urlImage)
                .placeholder(
                    Color.parseColor(Palette.LIGHT[position % Palette.LIGHT.size]).toDrawable()
                )
                .thumbnail(thumbnailRequest)
                .into(binding.wallpaperIv)
            viewModel.viewModelScope.launch {
                if (viewModel.isWallpaperSaved(photo as AlphaPhotoResponseItem)) {
                    binding.favBtn.setImageResource(R.drawable.ic_heart_filled)
                    isSaved = true
                } else {
                    binding.favBtn.setImageResource(R.drawable.heart_white)
                    isSaved = false
                }
            }

            binding.favBtn.setOnClickListener {
                if(!isSaved){
                    binding.favBtn.setImageResource(R.drawable.ic_heart_filled)
                    viewModel.saveWallpaper(photo as AlphaPhotoResponseItem)
                }else{
                    binding.favBtn.setImageResource(R.drawable.heart_white)
                    viewModel.deleteWallpaper(photo as AlphaPhotoResponseItem)
                }
            }
            binding.wallpaperIv.setOnClickListener {
                onItemClickListener?.let {
                    it(binding.wallpaperIv, photo.urlImage, photo as AlphaPhotoResponseItem)
                }
            }
        }
    }

    private val differCallback: DiffUtil.ItemCallback<Response> = object : DiffUtil.ItemCallback<Response>() {
        override fun areItemsTheSame(oldItem: Response, newItem: Response): Boolean {
            return oldItem.idR == newItem.idR
        }

        override fun areContentsTheSame(oldItem: Response, newItem: Response): Boolean {
            return oldItem.idR == newItem.idR
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun getItemCount(): Int = differ.currentList.size

}