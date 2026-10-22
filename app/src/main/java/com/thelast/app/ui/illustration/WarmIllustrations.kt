package com.thelast.app.ui.illustration

import androidx.annotation.DrawableRes
import com.thelast.app.R

@DrawableRes
fun eventIllustrationForKey(key: String?): Int {
    return when (key) {
        "history" -> R.drawable.illus_record
        "food" -> R.drawable.illus_food
        "gas" -> R.drawable.illus_gas
        "wash" -> R.drawable.illus_wash
        "bed" -> R.drawable.illus_bed
        "car" -> R.drawable.illus_wash
        "cut", "build" -> R.drawable.illus_maintenance
        "home" -> R.drawable.nav_home
        "heart" -> R.drawable.decor_cloud
        "star" -> R.drawable.decor_star
        "check" -> R.drawable.decor_trophy
        else -> R.drawable.illus_record
    }
}

@DrawableRes
fun navIllustrationForIndex(index: Int): Int {
    return when (index) {
        0 -> R.drawable.nav_home
        1 -> R.drawable.nav_calendar
        2 -> R.drawable.nav_stats
        else -> R.drawable.nav_profile
    }
}

@DrawableRes
fun statIllustrationForLabel(label: String): Int {
    return when {
        label.contains("记录次数") || label == "事件" -> R.drawable.decor_notebook
        label.contains("最近") || label == "已记录" -> R.drawable.decor_calendar
        label.contains("平均") || label == "待开始" -> R.drawable.decor_cloud
        else -> R.drawable.decor_trophy
    }
}
