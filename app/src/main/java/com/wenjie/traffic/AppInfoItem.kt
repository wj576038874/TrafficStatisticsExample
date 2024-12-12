package com.wenjie.traffic

import android.graphics.drawable.Drawable

/**
 * Created by wenjie on 2024/12/11.
 */
data class AppInfoItem(
    val name: String? = null,
    val icon: Drawable,
    val uid: Int,
    var use: Long = 0,
    var time: Long = 0
)