package com.stripe.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.stripe.android.R
import com.stripe.android.databinding.CardBrandViewBinding
import com.stripe.android.model.CardBrand

internal class CardBrandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val viewBinding: CardBrandViewBinding = CardBrandViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private val iconView = viewBinding.icon

    @ColorInt
    internal var tintColorInt: Int = 0

    init {
        isClickable = false
        isFocusable = false
    }

    private val rotationAnimation: Animation = RotateAnimation(
        0f,
        360f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f
    ).also {
        it.duration = 1500
        it.repeatCount = Animation.INFINITE
        it.interpolator = LinearInterpolator()
    }

    internal fun showBrandIcon(brand: CardBrand, shouldShowErrorIcon: Boolean) {
        iconView.animation = null

        if (shouldShowErrorIcon) {
            iconView.setImageResource(brand.errorIcon)
        } else {
            iconView.setImageResource(brand.icon)

            if (brand == CardBrand.Loading) {
                iconView.startAnimation(rotationAnimation)
                applyTint()
            } else if (brand == CardBrand.Unknown) {
                applyTint()
            }
        }
    }

    internal fun showCvcIcon(brand: CardBrand) {
        iconView.animation = null
        iconView.setImageResource(brand.cvcIcon)
        applyTint()
    }

    internal fun applyTint() {
        val icon = iconView.drawable
        val compatIcon = DrawableCompat.wrap(icon)
        DrawableCompat.setTint(compatIcon.mutate(), tintColorInt)
        iconView.setImageDrawable(DrawableCompat.unwrap(compatIcon))
    }
}
