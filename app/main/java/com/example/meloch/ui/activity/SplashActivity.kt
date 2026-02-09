package com.example.meloch.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.meloch.data.UserManager
import com.example.meloch.MainActivity
import com.example.meloch.R
import com.example.meloch.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val splashTimeOut: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set version text
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        binding.versionText.text = getString(R.string.version_text_format, versionName)

        // Apply animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        binding.splashLogo.startAnimation(fadeIn)
        binding.appNameText.startAnimation(slideUp)
        binding.taglineText.startAnimation(fadeIn)

        // Navigate to appropriate activity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is logged in
            val userManager = UserManager(this)
            val intent = if (userManager.getCurrentUser() != null) {
                // User is logged in, go to MainActivity
                Intent(this, MainActivity::class.java)
            } else {
                // User is not logged in, go to LoginActivity
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, splashTimeOut)
    }
}
