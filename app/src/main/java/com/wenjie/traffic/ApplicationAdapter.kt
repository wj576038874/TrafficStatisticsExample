package com.wenjie.traffic

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import java.util.Locale

/**
 * Created by wenjie on 2024/12/11.
 */
class ApplicationAdapter(private val type: Int) : RecyclerView.Adapter<ApplicationAdapter.MyViewHolder>() {

    var max: Long = 0

    var data = mutableListOf<AppInfoItem>()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class MyViewHolder(view: View) : ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val name: TextView = view.findViewById(R.id.tv_name)
        val use: TextView = view.findViewById(R.id.tv_use)
        val progress: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_adapter, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = data[position]
        holder.icon.setImageDrawable(item.icon)
        holder.name.text = item.name
        holder.progress.max = max.toInt()
        holder.progress.progress = item.use.toInt()
        holder.use.text = if (type == 1) formatSizeFromKB(item.use) else formatTime(item.time)
    }

    private fun formatSizeFromByte(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B" // 小于1KB，显示字节
            bytes in 1024..<1024 * 1024 -> "%.2f KB".format(bytes / 1024.0) // 大于1KB，小于1MB，显示KB，保留两位小数
            bytes in (1024 * 1024)..<1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024)) // 大于1MB，小于1GB，显示MB，保留两位小数
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024)) // 大于1GB，显示GB，保留两位小数
        }
    }

    fun formatSizeFromKB(kb: Long): String {
        return when {
            kb < 1024 -> "$kb KB"  // 小于1KB，直接显示KB
            kb in 1024..<1024 * 1024 -> "%.2f MB".format(kb / 1024.0)  // 大于1KB，小于1MB，显示MB，保留两位小数
            else -> "%.2f GB".format(kb / (1024.0 * 1024))  // 大于1MB，显示GB，保留两位小数
        }
    }

    fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        val seconds = (milliseconds % 60000) / 1000
        return String.format(Locale.getDefault(),"%02d小时%02d分钟", hours, minutes)
    }
}