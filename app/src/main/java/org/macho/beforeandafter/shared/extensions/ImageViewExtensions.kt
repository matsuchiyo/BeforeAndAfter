package org.macho.beforeandafter.shared.extensions

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.engine.DiskCacheStrategy
import org.macho.beforeandafter.R
import org.macho.beforeandafter.shared.GlideApp
import org.macho.beforeandafter.shared.util.LogUtil

fun ImageView.loadImage(fragment: Fragment, uri: Uri, useCache: Boolean = true) {
    LogUtil.i(this, "uri: $uri")
    if (useCache) {
        GlideApp.with(fragment)
                .load(uri)
                .sizeMultiplier(.4f)
                .thumbnail(.1f)
                .error(R.drawable.no_image)
                .into(this)

    } else {
        GlideApp.with(fragment)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .sizeMultiplier(.4f)
                .thumbnail(.1f)
                .error(R.drawable.no_image)
                .into(this)
    }
}

fun ImageView.loadImage(context: Context, uri: Uri, useCache: Boolean = true) {
    LogUtil.i(this, "uri: $uri")
    if (useCache) {
        GlideApp.with(context)
                .load(uri)
                .sizeMultiplier(.4f)
                .thumbnail(.1f)
                .error(R.drawable.no_image)
                .into(this)

    } else {
        GlideApp.with(context)
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .sizeMultiplier(.4f)
                .thumbnail(.1f)
                .error(R.drawable.no_image)
                .into(this)
    }
}