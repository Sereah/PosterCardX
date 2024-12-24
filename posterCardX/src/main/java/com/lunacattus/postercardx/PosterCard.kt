package com.lunacattus.postercardx

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

/**
 * An extended view based on [androidx.cardview.widget.CardView],
 * which generates a nice background based on a poster image.
 * - The inspiration comes from the need for multimedia cards to generate immersive backgrounds
 * that match the album colors according to the albums of different songs.
 * - You can use the default configuration, or customize the background blur, gradient, crop size,
 * position, etc. through parameters.
 * - PosterCard still has all the properties of CardView, but only adds a background,
 * so it is very easy to use.
 */
class PosterCard @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = R.attr.posterCardDefaultStyle
) : CardView(context, attributeSet, defStyleAttr) {

    private var topFraction: Float = 0.5f
    private var positionBias: Float = 0.5f
    private var colorExtractFraction: Float = 0.25f
    private var colorMinLuminance: Int = 50
    private var colorMaxLuminance: Int = 190
    private var colorSaturation: Float = 0.3f
    private var gradientFraction: Float = 0.5f
    private var blurFraction: Float = 0.3f
    private var blurTransportAlpha: Int = 255
    private var blurRadius: Float = 100f
    private var useLinearGradient: Boolean = true
    private var gradientTransportArray: IntArray = IntArray(0)
    private var gradientPositionArray: FloatArray = FloatArray(0)
    private var width: Int = 0
    private var height: Int = 0
    private var posterBitmap: Bitmap? = null
    private var processedPosterBitmap: Bitmap? = null
    private var cardBgColor: Int? = null
    private var croppedBitmap: Bitmap? = null
    private var isGlideLoading: Boolean = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    companion object {
        private const val TAG = "PosterCard-debug"
    }

    init {
        Log.d(TAG, "init")
        attributeSet?.let { attrs ->
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PosterCard)
            initPosterBitmap(typedArray)
            initAttrs(typedArray)
            typedArray.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Log.d(TAG, "onSizeChanged, width: $w, height: $h")
        if (width != w || height != h) {
            width = w
            height = h
            createPosterBitmap()
        }
    }

    override fun onDraw(canvas: Canvas) {
        Log.d(
            TAG,
            "onDraw, cardBgColor: $cardBgColor, processedPosterBitmap: $processedPosterBitmap"
        )
        if (cardBgColor == null || processedPosterBitmap == null) return
        cardBgColor?.let {
            canvas.drawColor(it)
        }
        processedPosterBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, paint)
        }
        super.onDraw(canvas)
    }

    /**
     * Set the poster image resource by url that needs to extract color and process.
     *
     * @param context activity or fragment, Glide load image using.
     * @param url the url of poster image.
     * @see R.styleable.PosterCard_posterCardUrl
     * @return PosterCard self
     */
    fun setPoster(context: Context, url: String): PosterCard {
        Log.d(TAG, "setPoster url: $url")
        isGlideLoading = true
        setPosterImgByUrl(context, url)
        return this
    }

    /**
     * Set the poster image resource by drawable that needs to extract color and process.
     *
     * @param drawable the drawable of poster image.
     * @see R.styleable.PosterCard_posterCardSrc
     * @return PosterCard self
     */
    fun setPoster(drawable: Drawable): PosterCard {
        Log.d(TAG, "setPoster drawable: $drawable")
        drawableToBitmap(drawable)
        createPosterBitmap()
        return this
    }

    /**
     * Set the poster image resource by bitmap that needs to extract color and process.
     *
     * @param bitmap the bitmap of poster image.
     * @return PosterCard self
     */
    fun setPoster(bitmap: Bitmap): PosterCard {
        Log.d(TAG, "setPoster bitmap: $bitmap")
        posterBitmap = bitmap
        createPosterBitmap()
        return this
    }

    /**
     * Set the scale of the poster to cover the top of the card.
     *
     * @param value between 0 and 1, default is 0.5f.
     * @see R.styleable.PosterCard_posterCardTopFraction
     * @return PosterCard self
     */
    fun setTopFraction(value: Float): PosterCard {
        Log.d(TAG, "setTopFraction: $value")
        topFraction = value
        return this
    }

    /**
     * Set the position of the cropped poster when it covers the top of the card.
     * If the poster is wider, you can adjust the left and right positions, otherwise adjust the
     * top and bottom positions.
     * - The default is centered.
     *
     * @param value between 0 and 1, default is 0.5f.
     * @see R.styleable.PosterCard_posterCardPositionBias
     * @return PosterCard self
     */
    fun setPositionBias(value: Float): PosterCard {
        Log.d(TAG, "setPositionBias: $value")
        positionBias = value
        return this
    }

    /**
     * Sets the bottom area ratio to extract colors from the poster, based on the cropped poster.
     * You can export the cropped poster [getCroppedPoster]
     *
     * @param value between 0 and 1, default is 0.25f.
     * @see R.styleable.PosterCard_posterCardColorExtractAreaFraction
     * @return PosterCard self
     */
    fun setColorExtractFraction(value: Float): PosterCard {
        Log.d(TAG, "setColorExtractFraction: $value")
        colorExtractFraction = value
        return this
    }

    /**
     * Set the minimum luminance of the filtered color when extracting color.
     * - Black has the smallest luminance and white has the largest luminance.
     *
     * @param value between 0 and 255, default is 50.
     * @see R.styleable.PosterCard_posterCardColorMinLuminance
     * @return PosterCard self
     */
    fun setColorMinLuminance(value: Int): PosterCard {
        Log.d(TAG, "setColorMinLuminance: $value")
        colorMinLuminance = value
        return this
    }

    /**
     * Set the maximum luminance of the filtered color when extracting color.
     * - Black has the smallest luminance and white has the largest luminance.
     *
     * @param value between 0 and 255, default is 190.
     * @see R.styleable.PosterCard_posterCardColorMaxLuminance
     * @return PosterCard self
     */
    fun setColorMaxLuminance(value: Int): PosterCard {
        Log.d(TAG, "setColorMaxLuminance: $value")
        colorMaxLuminance = value
        return this
    }

    /**
     * Set the saturation of the extracted color.
     * - The higher the saturation, the brighter the color, and vice versa, the closer to gray.
     *
     * @param value between 0 and 1, default is 0.3f.
     * @see R.styleable.PosterCard_posterCardColorSaturation
     * @return PosterCard self
     */
    fun setColorSaturation(value: Float): PosterCard {
        Log.d(TAG, "setColorSaturation: $value")
        colorSaturation = value
        return this
    }

    /**
     * Set the percentage of the gradient color that is overlaid on the cropped poster, from
     * bottom to top.
     *
     * @param value between 0 and 1, default is 0.5f.
     * @see R.styleable.PosterCard_posterCardGradientFraction
     * @return PosterCard self
     */
    fun setGradientFraction(value: Float): PosterCard {
        Log.d(TAG, "setGradientFraction: $value")
        gradientFraction = value
        return this
    }

    /**
     * Set the percentage of the blur color that is overlaid on the cropped poster, from
     * bottom to top.
     *
     * @param value between 0 and 1, default is 0.3f.
     * @see R.styleable.PosterCard_posterCardBlurFraction
     * @return PosterCard self
     */
    fun setBlurFraction(value: Float): PosterCard {
        Log.d(TAG, "setBlurFraction: $value")
        blurFraction = value
        return this
    }

    /**
     * Set the transparency alpha value of the cropped poster overlay blur color.
     * - The larger the value, the lower the transparency, otherwise it is higher.
     *
     * @param value between 0 and 255, default is 255.
     * @see R.styleable.PosterCard_posterCardBlurTransportAlpha
     * @return PosterCard self
     */
    fun setBlurTransportAlpha(value: Int): PosterCard {
        Log.d(TAG, "setBlurTransportAlpha: $value")
        blurTransportAlpha = value
        return this
    }

    /**
     * Set the blur level of the poster overlay blur color.
     * - The larger the value, the lower the blur, otherwise the higher the blur.
     *
     * @param value Greater than 0, default is 100f
     * @see R.styleable.PosterCard_posterCardBlurRadius
     * @return PosterCard self
     */
    fun setBlurRadius(value: Float): PosterCard {
        Log.d(TAG, "setBlurRadius: $value")
        if (value == 0f) return this
        blurRadius = value
        return this
    }

    /**
     * Set whether to use linear gradient. If not, you can customize the gradient steps through
     * [setGradientTransportAlphaArray] and [setGradientPositionArray].
     *
     * @param value boolean, default is true.
     * @see R.styleable.PosterCard_posterCardUseLinearGradient
     * @return PosterCard self
     */
    fun setUseLinearGradient(value: Boolean): PosterCard {
        Log.d(TAG, "setUseLinearGradient: $value")
        useLinearGradient = value
        return this
    }

    /**
     * Sets the transparency alpha value array in the custom gradient ladder.
     * - The array length needs to be consistent with the length of [setGradientPositionArray].
     * And set the [setUseLinearGradient] is false.
     * - The transparency alpha of the array is from bottom to top, for example:
     * ```kotlin
     * setGradientTransportAlphaArray(intArrayOf(255, 230, 200, 170, 130))
     * ```
     * 255 represents the lowest transparency, and is the bottom part of the poster image,
     * and then the transparency increases upwards, with a total of 5 steps.
     *
     * @param value IntArray
     * @see R.styleable.PosterCard_posterCardGradientTransportAlphaIntArray
     * @return PosterCard self
     */
    fun setGradientTransportAlphaArray(value: IntArray): PosterCard {
        Log.d(TAG, "setGradientTransportAlphaArray: $value")
        gradientTransportArray = value
        return this
    }

    /**
     * Sets the transparency position value array in the custom gradient ladder.
     * - The array length needs to be consistent with the length of [setGradientTransportAlphaArray].
     * And set the [setUseLinearGradient] is false.
     * - The position of the array is from bottom to top, for example:
     * ```kotlin
     * setGradientPositionArray(floatArrayOf(0.0f, 0.3f, 0.6f, 0.8f, 1.0f))
     * ```
     * 0.0f represents the bottom part of the poster image,
     * and then the position increases upwards, with a total of 5 steps.
     *
     * @param value FloatArray
     * @see R.styleable.PosterCard_posterCardGradientPositionFloatArray
     * @return PosterCard self
     */
    fun setGradientPositionArray(value: FloatArray): PosterCard {
        Log.d(TAG, "setGradientPositionArray: $value")
        gradientPositionArray = value
        return this
    }

    /**
     * Called after all properties have been set,
     * it will trigger PosterCard to redraw with the new properties.
     */
    fun build() {
        if (isGlideLoading) return
        createPosterBitmap()
    }

    /**
     * Get the cropped poster image,
     * which will be used as the object for color extraction and
     * the object for adding blur and gradient effects.
     *
     * @return Bitmap
     */
    fun getCroppedPoster(): Bitmap? {
        return croppedBitmap
    }

    /**
     * Get the extract color.
     */
    fun getExtractColor(): Int? {
        return cardBgColor
    }

    private fun setPosterImgByUrl(
        context: Context,
        picUrl: String
    ) {
        Glide.with(context)
            .asBitmap()
            .load(picUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d(TAG, "Glide onResourceReady: $resource")
                    posterBitmap = resource
                    isGlideLoading = false
                    createPosterBitmap()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    isGlideLoading = false
                }
            })
    }

    private fun initPosterBitmap(typedArray: TypedArray) {
        val srcResId = typedArray.getResourceId(R.styleable.PosterCard_posterCardSrc, -1)
        srcResId.takeIf { it != -1 }?.let {
            val typeName = resources.getResourceTypeName(srcResId)
            Log.d(TAG, "typeName: $typeName")
            if ("drawable" == typeName || "mipmap" == typeName) {
                val posterDrawable = ContextCompat.getDrawable(context, srcResId)
                Log.d(TAG, "posterDrawable: $posterDrawable")
                posterDrawable?.let { drawableToBitmap(it) }
            } else {
                throw IllegalArgumentException("posterSrc must be a drawable or mipmap resource.")
            }
        }

        val url = typedArray.getString(R.styleable.PosterCard_posterCardUrl)
        url?.let { setPosterImgByUrl(context, it) }
    }

    private fun initAttrs(typedArray: TypedArray) {
        topFraction = typedArray.getFloat(R.styleable.PosterCard_posterCardTopFraction, 0.5f)
        positionBias = typedArray.getFloat(R.styleable.PosterCard_posterCardPositionBias, 0.5f)
        colorExtractFraction = typedArray.getFloat(
            R.styleable
                .PosterCard_posterCardColorExtractAreaFraction, 0.25f
        )
        colorMinLuminance = typedArray.getInt(
            R.styleable.PosterCard_posterCardColorMinLuminance, 50
        )
        colorMaxLuminance = typedArray.getInt(
            R.styleable.PosterCard_posterCardColorMaxLuminance, 190
        )
        colorSaturation = typedArray.getFloat(
            R.styleable.PosterCard_posterCardColorSaturation, 0.3f
        )
        gradientFraction = typedArray.getFloat(
            R.styleable.PosterCard_posterCardGradientFraction, 0.5f
        )
        blurFraction = typedArray.getFloat(
            R.styleable.PosterCard_posterCardBlurFraction, 0.3f
        )
        blurTransportAlpha = typedArray.getInt(
            R.styleable.PosterCard_posterCardBlurTransportAlpha, 255
        )
        blurRadius = typedArray.getFloat(
            R.styleable.PosterCard_posterCardBlurRadius, 100f
        )
        useLinearGradient = typedArray.getBoolean(
            R.styleable.PosterCard_posterCardUseLinearGradient, true
        )
        val gradientIntArrayId = typedArray.getResourceId(
            R.styleable.PosterCard_posterCardGradientTransportAlphaIntArray, -1
        )
        gradientIntArrayId.takeIf { it != -1 }?.let {
            val typeName = resources.getResourceTypeName(gradientIntArrayId)
            if (typeName == "array") {
                gradientTransportArray = context.resources.getIntArray(gradientIntArrayId)
            } else {
                throw IllegalArgumentException("posterCardGradientIntArray must be a array.")
            }
        }
        val gradientFloatArrayId = typedArray.getResourceId(
            R.styleable.PosterCard_posterCardGradientPositionFloatArray, -1
        )
        gradientFloatArrayId.takeIf { it != -1 }?.let {
            val typeName = resources.getResourceTypeName(gradientFloatArrayId)
            if (typeName == "array") {
                val array = resources.obtainTypedArray(gradientFloatArrayId)
                val positionArray = FloatArray(array.length())
                for (i in 0 until array.length()) {
                    positionArray[i] = array.getFloat(i, 0.0f)
                }
                gradientPositionArray = positionArray
                array.recycle()
            } else {
                throw IllegalArgumentException("posterCardGradientPositionFloatArray must be a array.")
            }
        }
    }

    private fun createPosterBitmap() {
        Log.d(
            TAG,
            "createPosterBitmap, card width:$width, height: $height, posterBitmap: $posterBitmap"
        )
        if (width == 0 || height == 0) return
        posterBitmap?.let {
            val cropBitmap = getCropPoster(it, width, height, topFraction, positionBias)
            croppedBitmap = cropBitmap
            cardBgColor = extractBackgroundColor(
                cropBitmap, colorExtractFraction,
                colorMinLuminance, colorMaxLuminance, colorSaturation
            )
            val gradientPosterBitmap = getGradientBlurBitmap(
                cardBgColor!!,
                cropBitmap,
                gradientFraction,
                blurFraction,
                blurTransportAlpha,
                blurRadius,
                useLinearGradient,
                gradientTransportArray,
                gradientPositionArray
            )
            val cornerRadius = radius
            processedPosterBitmap = getRoundedCornerBitmap(gradientPosterBitmap, cornerRadius)
            invalidate()
        }
    }

    private fun drawableToBitmap(drawable: Drawable) {
        if (drawable is BitmapDrawable) {
            posterBitmap = drawable.bitmap
        } else {
            throw IllegalArgumentException("drawableToBitmap, poster type must be BitmapDrawable.")
        }
    }

    private fun getCropPoster(
        posterBitmap: Bitmap,
        cardWidth: Int,
        cardHeight: Int,
        topFraction: Float,
        positionBias: Float,
    ): Bitmap {
        val posterWidth = posterBitmap.width
        val posterHeight = posterBitmap.height
        val cropHeight = (cardHeight * topFraction).toInt()
        Log.d(
            TAG,
            "posterWidth: $posterWidth, posterHeight: $posterHeight, cardWidth: $cardWidth, cropHeight: $cropHeight"
        )

        val scale: Float
        val dx: Float
        val dy: Float
        if (posterWidth * cropHeight > cardWidth * posterHeight) {
            scale = cropHeight / posterHeight.toFloat()
            dx = (cardWidth - posterWidth * scale) * positionBias
            dy = 0f
        } else {
            scale = cardWidth / posterWidth.toFloat()
            dx = 0f
            dy = (cropHeight - posterHeight * scale) * positionBias
        }
        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)

        val croppedBitmap = Bitmap.createBitmap(cardWidth, cropHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(croppedBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(posterBitmap, matrix, paint)

        return croppedBitmap
    }

    private fun extractBackgroundColor(
        bitmap: Bitmap,
        colorCollectBottomFraction: Float,
        colorMinLuminance: Int,
        colorMaxLuminance: Int,
        colorSaturation: Float
    ): Int {
        val height = bitmap.height
        val width = bitmap.width
        val bottomHeight = (height * colorCollectBottomFraction).toInt()

        val bottomBitmap =
            Bitmap.createBitmap(bitmap, 0, height - bottomHeight, width, bottomHeight)
        val palette = Palette.from(bottomBitmap).generate()
        val filteredSwatches = palette.swatches.filter { swatch ->
            val rgb = swatch.rgb
            val r = (rgb shr 16 and 0xFF)
            val g = (rgb shr 8 and 0xFF)
            val b = (rgb and 0xFF)
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            luminance > colorMinLuminance && luminance < colorMaxLuminance
        }
        val dominantColor = filteredSwatches.maxByOrNull { it.population }?.rgb
        if (dominantColor == null) {
            Log.e(TAG, "Failed to extract color within the luminance range, return transparent.")
            return Color.TRANSPARENT
        } else {
            val hsv = FloatArray(3)
            Color.colorToHSV(dominantColor, hsv)
            hsv[1] *= colorSaturation
            return Color.HSVToColor(Color.alpha(dominantColor), hsv)
        }
    }

    private fun getGradientBlurBitmap(
        bgColor: Int,
        bitmap: Bitmap,
        gradientFraction: Float,
        blurFraction: Float,
        blurTransportAlpha: Int,
        blurRadius: Float,
        useLinearGradient: Boolean,
        gradientTransportArray: IntArray,
        gradientPositionArray: FloatArray
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val gradientHeight = (height * gradientFraction).toInt()
        val blurHeight = (height * blurFraction).toInt()
        val gradientRect = Rect(0, height - gradientHeight, width, height)
        val blurRect = Rect(0, height - blurHeight, width, height)

        val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val blurPaint = Paint().apply {
            isAntiAlias = true
            color = addTransportColor(bgColor, blurTransportAlpha)
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRect(blurRect, blurPaint)

        if (useLinearGradient) {
            val linearGradientPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f,
                    height.toFloat(),
                    0f,
                    (height - gradientHeight).toFloat(),
                    bgColor,
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(gradientRect, linearGradientPaint)
        } else {
            val colors = IntArray(gradientTransportArray.size)
            for (i in gradientTransportArray.indices) {
                colors[i] = addTransportColor(bgColor, gradientTransportArray[i])
            }
            val gradientPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f,
                    height.toFloat(),
                    0f,
                    (height - gradientHeight).toFloat(),
                    colors,
                    gradientPositionArray,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(gradientRect, gradientPaint)
        }
        return newBitmap
    }

    private fun addTransportColor(color: Int, alpha: Int): Int {
        val validAlpha = alpha.coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (validAlpha shl 24)
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
        }
        val rect = Rect(0, 0, width, height)
        val rectF = RectF(rect)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }
}