package com.example.meloch.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.meloch.MainActivity
import com.example.meloch.R
import com.example.meloch.data.UserManager
import com.example.meloch.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userManager = UserManager(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (validateInputs(username, email, password)) {
                if (userManager.register(username, email, password)) {
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    Toast.makeText(this, getString(R.string.email_exists), Toast.LENGTH_SHORT).show()
                    binding.emailInputLayout.error = getString(R.string.email_exists)
                }
            }
        }

        binding.loginLink.setOnClickListener {
            onBackPressed()
        }
    }

    private fun validateInputs(username: String, email: String, password: String): Boolean {
        var isValid = true

        // Validate username
        if (!userManager.isValidUsername(username)) {
            binding.usernameInputLayout.error = getString(R.string.invalid_username)
            isValid = false
        } else {
            binding.usernameInputLayout.error = null
        }

        // Validate email
        if (!userManager.isValidEmail(email)) {
            binding.emailInputLayout.error = getString(R.string.invalid_email)
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }

        // Validate password
        if (!userManager.isValidPassword(password)) {
            binding.passwordInputLayout.error = getString(R.string.invalid_password)
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }

        return isValid
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
