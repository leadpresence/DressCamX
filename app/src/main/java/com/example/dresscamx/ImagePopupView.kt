package com.example.dresscamx


import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import kotlinx.android.synthetic.main.image_popup.view.*

class ImagePopupView : FrameLayout {

    companion object {

        const val FADING_ANIMATION_DURATION = 200L

        const val ALPHA_TRANSPARENT = 0.0f
        private const val ALPHA_OPAQUE = 1.0f

        @LayoutRes
        private const val LAYOUT_RESOURCE = R.layout.image_popup

        fun builder(context: Context): ImagePopupBuilder = ImagePopupBuilder(context)
    }

    private var imageDrawable: Drawable? = null
    private var onBackgroundClickAction: () -> Unit = {}

    constructor(context: Context, builder: ImagePopupBuilder) : super(context, null) {
        imageDrawable = builder.imageDrawable
        onBackgroundClickAction = builder.onBackgroundClickAction

        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        alpha = ALPHA_TRANSPARENT

        inflateLayout(context)
        fillContent()
        setClickListeners()
    }

    private fun inflateLayout(context: Context) =
        LayoutInflater.from(context).inflate(LAYOUT_RESOURCE, this, true)

    private fun fillContent() {
        imagePopup.setImageDrawable(imageDrawable)
        fadeInView()
    }

    private fun setClickListeners() {
        imagePopupRoot.setOnClickListener { onBackgroundClickAction.invoke() }
    }

    private fun fadeInView() {
        imagePopup.visibility = View.VISIBLE
        animate().alpha(ALPHA_OPAQUE)
            .setDuration(FADING_ANIMATION_DURATION)
            .start()
    }

    class ImagePopupBuilder(private val context: Context) {

        var imageDrawable: Drawable? = null
            private set

        var onBackgroundClickAction: () -> Unit = {}
            private set


        fun imageDrawable(imageDrawable: Drawable) = apply { this.imageDrawable = imageDrawable }

        fun onBackgroundClickAction(onBackgroundClickAction: () -> Unit) =
            apply { this.onBackgroundClickAction = onBackgroundClickAction }

        fun build(): ImagePopupView = ImagePopupView(context, this)
    }
}