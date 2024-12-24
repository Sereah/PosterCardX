package com.lunacattus.postercardx

import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lunacattus.postercarddemo.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val posterCard = PosterCard(this).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
        }
        val drawable = ContextCompat.getDrawable(this, com.lunacattus.postercarddemo.R.drawable.jay5)
        if (drawable != null) {
            posterCard.setPoster(drawable).setTopFraction(0.4f).build()
        }
        posterCard.radius = 30f
        posterCard.cardElevation = 50f
        binding.cardContainer.addView(posterCard)
    }
}