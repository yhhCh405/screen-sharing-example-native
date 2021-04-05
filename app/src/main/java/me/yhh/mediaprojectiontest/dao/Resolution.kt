package me.yhh.mediaprojectiontest.dao

import android.os.Parcelable
import android.util.DisplayMetrics
import androidx.versionedparcelable.VersionedParcelize
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Resolution(
    val width: Int, val height: Int
) : Parcelable {
    companion object{
        val getDefaultResolutionsList: List<Resolution> = listOf(
            Resolution(540,960),
            Resolution(1440,2960),
            Resolution(1080, 1920),
            Resolution(1200,1920)
        )
    }
}