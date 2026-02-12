package com.example.meloch.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.meloch.data.ProfileNotificationHelper
import androidx.fragment.app.Fragment
import com.example.meloch.R
import com.example.meloch.data.UserManager
import com.example.meloch.databinding.FragmentProfileBinding
import com.example.meloch.ui.activity.LoginActivity
import com.example.meloch.util.SimplePdfGenerator
import com.example.meloch.util.JsonBackupGenerator
import com.example.meloch.util.JsonBackupImporter

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var userManager: UserManager
    private lateinit var notificationHelper: ProfileNotificationHelper
    private lateinit var pdfReportGenerator: SimplePdfGenerator
    private lateinit var jsonBackupGenerator: JsonBackupGenerator
    private lateinit var jsonBackupImporter: JsonBackupImporter

    // Permission launcher for notification permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, notifications will work
        } else {
            // Permission denied, show a message
            Toast.makeText(
                requireContext(),
                "Notification permission denied. You won't receive feedback notifications.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UserManager, NotificationHelper, PdfReportGenerator, JsonBackupGenerator, and JsonBackupImporter
        userManager = UserManager(requireContext())
        notificationHelper = ProfileNotificationHelper(requireContext())
        pdfReportGenerator = SimplePdfGenerator(requireContext())
        jsonBackupGenerator = JsonBackupGenerator(requireContext())
        jsonBackupImporter = JsonBackupImporter(requireContext())

        // Load user data
        loadUserData()

        // Check notification permission
        checkNotificationPermission()

        // Setup rating bar listener
        setupRatingBar()

        // Setup submit review button
        setupSubmitReviewButton()

        // Setup account action buttons
        setupExportPdfButton()
        setupBackupButton()
        setupImportButton()
        setupLogoutButton()
        setupDeleteAccountButton()
    }

    private fun loadUserData() {
        // Get current user
        val currentUser = userManager.getCurrentUser()

        if (currentUser != null) {
            // Set user information in the UI
            binding.usernameText.text = currentUser.username
            binding.emailText.text = currentUser.email

            binding.usernameValueText.text = currentUser.username
            binding.emailValueText.text = currentUser.email

            // Show masked password
            val maskedPassword = "•".repeat(minOf(currentUser.password.length, 8))
            binding.passwordValueText.text = maskedPassword
        }
    }

    private fun setupRatingBar() {
        binding.appRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            // Update the review input hint based on the rating
            when (rating.toInt()) {
                1, 2 -> binding.reviewInputLayout.hint = getString(R.string.review_hint) + " (What can we improve?)"
                3 -> binding.reviewInputLayout.hint = getString(R.string.review_hint) + " (What do you like and what can we improve?)"
                4, 5 -> binding.reviewInputLayout.hint = getString(R.string.review_hint) + " (What do you like most?)"
                else -> binding.reviewInputLayout.hint = getString(R.string.review_hint)
            }
        }
    }

    private fun setupSubmitReviewButton() {
        binding.submitReviewButton.setOnClickListener {
            val rating = binding.appRatingBar.rating.toInt()
            val review = binding.reviewInput.text.toString().trim()

            // Skip if no rating is provided
            if (rating == 0) {
                Toast.makeText(requireContext(), "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show appropriate message based on rating
            val message = when (rating) {
                1, 2 -> getString(R.string.review_low_rating)
                3 -> getString(R.string.review_medium_rating)
                4, 5 -> getString(R.string.review_high_rating)
                else -> getString(R.string.review_submitted)
            }

            // Show system notification instead of dialog
            notificationHelper.showReviewFeedbackNotification(
                getString(R.string.review_submitted),
                message
            )

            // Show a toast to confirm submission
            Toast.makeText(requireContext(), "Review submitted. Check notification.", Toast.LENGTH_SHORT).show()

            // Clear the form
            binding.appRatingBar.rating = 0f
            binding.reviewInput.setText("")
        }
    }

    /**
     * Sets up the export PDF button click listener
     */
    private fun setupExportPdfButton() {
        binding.exportPdfButton.setOnClickListener {
            // Always check for storage permissions regardless of Android version
            checkStoragePermissionAndExportPdf()
        }
    }

    /**
     * Enum to track the current operation requiring storage permission
     */
    private enum class StorageOperation {
        PDF_EXPORT,
        DATA_BACKUP
    }

    private var currentStorageOperation = StorageOperation.PDF_EXPORT

    // File picker launcher for importing backup files
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            importBackup(uri)
        } else {
            Toast.makeText(
                requireContext(),
                "No file selected",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Checks for storage permission and performs the appropriate operation if granted
     */
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            when (currentStorageOperation) {
                StorageOperation.PDF_EXPORT -> exportPdfReport()
                StorageOperation.DATA_BACKUP -> createBackup()
            }
        } else {
            val message = when (currentStorageOperation) {
                StorageOperation.PDF_EXPORT -> "Storage permission is required to save PDF reports"
                StorageOperation.DATA_BACKUP -> "Storage permission is required to create data backups"
            }

            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun checkStoragePermissionAndExportPdf() {
        // Set the current operation
        currentStorageOperation = StorageOperation.PDF_EXPORT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, we don't need special permissions
            // as we're using MediaStore API
            exportPdfReport()
        } else {
            // For Android 9 and below, check for storage permission
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                exportPdfReport()
            } else {
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Exports the PDF report
     */
    private fun exportPdfReport() {
        // Show progress dialog
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Generating Report")
            .setMessage("Please wait while we generate your financial report...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // Get current user
        val currentUser = userManager.getCurrentUser()
        val username = currentUser?.username ?: "User"

        // Generate PDF in a background thread
        Thread {
            val pdfUri = pdfReportGenerator.generateMonthlyReport(username)

            // Update UI on main thread
            activity?.runOnUiThread {
                progressDialog.dismiss()

                if (pdfUri != null) {
                    showPdfExportSuccessDialog(pdfUri)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to generate PDF report. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    /**
     * Shows a success dialog with options to view or share the PDF
     */
    private fun showPdfExportSuccessDialog(pdfUri: Uri) {
        // Determine the file path based on Android version
        val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Documents/Meloch/Reports/"
        } else {
            "${Environment.getExternalStorageDirectory().absolutePath}/Meloch/Reports/"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Report Generated")
            .setMessage("Your financial report has been successfully generated and saved to:\n\n$filePath")
            .setPositiveButton("View") { _, _ ->
                // Open the PDF with a PDF viewer
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(pdfUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "No PDF viewer app found. Please install one to view the report.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNeutralButton("Share") { _, _ ->
                // Share the PDF
                pdfReportGenerator.sharePdfReport(pdfUri)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Sets up the backup button click listener
     */
    private fun setupBackupButton() {
        binding.backupDataButton.setOnClickListener {
            // Always check for storage permissions regardless of Android version
            checkStoragePermissionAndBackupData()
        }
    }

    /**
     * Checks for storage permission and creates backup if granted
     */
    private fun checkStoragePermissionAndBackupData() {
        // Set the current operation
        currentStorageOperation = StorageOperation.DATA_BACKUP

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, we don't need special permissions
            // as we're using MediaStore API
            createBackup()
        } else {
            // For Android 9 and below, check for storage permission
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                createBackup()
            } else {
                // Use the same permission launcher as for PDF export
                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    /**
     * Creates a backup of all user data
     */
    private fun createBackup() {
        // Show progress dialog
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Creating Backup")
            .setMessage("Please wait while we backup your data...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // Generate backup in a background thread
        Thread {
            val backupUri = jsonBackupGenerator.generateBackup()

            // Update UI on main thread
            activity?.runOnUiThread {
                progressDialog.dismiss()

                if (backupUri != null) {
                    showBackupSuccessDialog(backupUri)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to create backup. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    /**
     * Shows a success dialog with options to share the backup
     */
    private fun showBackupSuccessDialog(backupUri: Uri) {
        // Determine the file path based on Android version
        val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "Documents/Meloch/Backup/"
        } else {
            "${Environment.getExternalStorageDirectory().absolutePath}/Meloch/Backup/"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Backup Created")
            .setMessage("Your data has been successfully backed up and saved to:\n\n$filePath")
            .setPositiveButton("Share") { _, _ ->
                // Share the backup file
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, backupUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Meloch Data Backup")
                    putExtra(Intent.EXTRA_TEXT, "Please find attached my Meloch app data backup.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share Backup File")
                startActivity(chooser)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    /**
     * Sets up the import button click listener
     */
    private fun setupImportButton() {
        binding.importBackupButton.setOnClickListener {
            // Launch file picker to select a backup file
            filePickerLauncher.launch("application/json")
        }
    }

    /**
     * Imports data from a backup file
     * @param uri URI of the backup file to import
     */
    private fun importBackup(uri: Uri) {
        // Show progress dialog
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Importing Backup")
            .setMessage("Please wait while we restore your data...")
            .setCancelable(false)
            .create()

        progressDialog.show()

        // Import backup in a background thread
        Thread {
            val success = jsonBackupImporter.importBackup(uri)

            // Update UI on main thread
            activity?.runOnUiThread {
                progressDialog.dismiss()

                if (success) {
                    showImportSuccessDialog()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to import backup. Please try again with a valid backup file.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    /**
     * Shows a success dialog after successful import
     */
    private fun showImportSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Import Successful")
            .setMessage("Your data has been successfully restored from the backup file.")
            .setPositiveButton("OK") { _, _ ->
                // Refresh the main activity to show the imported data
                (activity as? com.example.meloch.MainActivity)?.updateDashboard()

                // Refresh the current fragment
                loadUserData()
            }
            .show()
    }

    private fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    /**
     * Checks if notification permission is granted and requests it if needed
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale if needed
                    AlertDialog.Builder(requireContext())
                        .setTitle("Notification Permission")
                        .setMessage("We need notification permission to show you feedback after rating the app.")
                        .setPositiveButton("OK") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                else -> {
                    // Request permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Perform logout
                userManager.logout()

                // Navigate to login screen
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Sets up the delete account button click listener
     */
    private fun setupDeleteAccountButton() {
        binding.deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }
    }

    /**
     * Shows a confirmation dialog before deleting the account
     */
    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_account))
            .setMessage(getString(R.string.delete_account_confirmation))
            .setPositiveButton("Delete") { _, _ ->
                // Delete the account
                if (userManager.deleteCurrentUser()) {
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.delete_account_success),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to login screen
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Failed to delete account. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
