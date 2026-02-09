package com.example.meloch.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.meloch.R
import com.example.meloch.data.PreferencesManager
import com.example.meloch.data.model.Category
import com.example.meloch.data.model.Transaction
import com.example.meloch.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale


class SimplePdfGenerator(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("LKR")
    }
    private val dateFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    // Colors
    private val colorBackground = ContextCompat.getColor(context, R.color.background)
    private val colorPrimary = ContextCompat.getColor(context, R.color.primary)
    private val colorWhite = Color.WHITE
    private val colorIncome = Color.GREEN
    private val colorExpense = Color.RED

    // Page dimensions (A4 size in points)
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 50

    // Text sizes
    private val titleTextSize = 24f
    private val subtitleTextSize = 16f
    private val sectionTitleTextSize = 18f
    private val normalTextSize = 12f
    private val smallTextSize = 10f

    fun generateMonthlyReport(username: String): Uri? {
        try {
            // Get the current month and create filename
            val currentMonth = dateFormatter.format(Date())
            val fileName = "Meloch_Report_${currentMonth.replace(" ", "_")}.pdf"

            // Create the file
            val file: File
            val outputStream: FileOutputStream
            var uri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Meloch/Reports")
                }

                uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri == null) {
                    throw IOException("Failed to create new MediaStore record.")
                }

                // Create a temporary file to write the PDF
                file = File(context.cacheDir, fileName)
                outputStream = FileOutputStream(file)
            } else {
                // For Android 9 and below, use direct file access
                val externalDir = Environment.getExternalStorageDirectory()
                val melochDir = File(externalDir, "Meloch")
                val reportsDir = File(melochDir, "Reports")

                // Create directory structure if it doesn't exist
                if (!melochDir.exists()) {
                    val mkdirsSuccess = melochDir.mkdirs()
                    if (!mkdirsSuccess && !melochDir.exists()) {
                        throw IOException("Failed to create directory: ${melochDir.absolutePath}")
                    }
                }
                if (!reportsDir.exists()) {
                    val mkdirsSuccess = reportsDir.mkdirs()
                    if (!mkdirsSuccess && !reportsDir.exists()) {
                        throw IOException("Failed to create directory: ${reportsDir.absolutePath}")
                    }
                }

                // Create the file in external storage
                file = File(reportsDir, fileName)
                outputStream = FileOutputStream(file)
            }

            // Create PDF document
            val pdfDocument = PdfDocument()

            // Create pages and add content
            createPdfContent(pdfDocument, username)

            // Write the PDF document to the output stream
            pdfDocument.writeTo(outputStream)

            // Close the document and output stream
            pdfDocument.close()
            outputStream.close()

            // For Android 10+, copy the temp file to MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        file.inputStream().use { it.copyTo(os) }
                    }

                    // Success message will be shown by the caller on the main thread
                } catch (e: Exception) {
                    Log.e("SimplePdfGenerator", "Error copying PDF to MediaStore", e)
                    return null
                }

                return uri
            } else {
                // For Android 9 and below, return FileProvider URI
                // Success message will be shown by the caller on the main thread

                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
        } catch (e: Exception) {
            Log.e("SimplePdfGenerator", "Error generating PDF: ${e.message}", e)
            // Error message will be shown by the caller on the main thread
            return null
        }
    }

    private fun createPdfContent(pdfDocument: PdfDocument, username: String) {
        val currentMonth = dateFormatter.format(Date())
        var currentY = margin.toFloat()
        var pageNum = 1

        // Create the first page
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Set background color
        canvas.drawColor(colorBackground)

        // Add title and subtitle
        currentY = drawTitle(canvas, "MELOCH FINANCIAL REPORT", currentY)
        currentY = drawSubtitle(canvas, "Monthly Summary - $currentMonth", currentY)

        // Add financial summary section
        currentY = drawSectionTitle(canvas, "Financial Summary", currentY)
        currentY = drawFinancialSummary(canvas, currentY)

        // Add transactions section
        currentY = drawSectionTitle(canvas, "Recent Transactions", currentY)
        currentY = drawTransactions(canvas, currentY)

        // Check if we need a new page for category breakdown
        if (currentY > pageHeight - margin * 2) {
            // Finish current page
            pdfDocument.finishPage(page)

            // Create a new page
            pageNum++
            val newPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val newPage = pdfDocument.startPage(newPageInfo)
            val newCanvas = newPage.canvas

            // Set background color for new page
            newCanvas.drawColor(colorBackground)

            // Reset Y position
            currentY = margin.toFloat()

            // Add category breakdown section on new page
            currentY = drawSectionTitle(newCanvas, "Expense Breakdown by Category", currentY)
            currentY = drawCategoryBreakdown(newCanvas, currentY)

            // Check if we need another page for the doughnut chart
            if (currentY > pageHeight - 450) { // Estimate space needed for chart + legends + footer
                // Finish current page
                pdfDocument.finishPage(newPage)

                // Create a new page for the chart
                pageNum++
                val chartPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val chartPage = pdfDocument.startPage(chartPageInfo)
                val chartCanvas = chartPage.canvas

                // Set background color for chart page
                chartCanvas.drawColor(colorBackground)

                // Add chart title
                val chartTitleY = margin.toFloat() + 30f
                val chartTitlePaint = Paint().apply {
                    color = colorWhite
                    textSize = sectionTitleTextSize
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                chartCanvas.drawText("Expense Distribution Chart", pageWidth / 2f, chartTitleY, chartTitlePaint)

                // Draw the doughnut chart
                drawDoughnutChart(chartCanvas, chartTitleY + 20f)

                // Add footer
                drawFooter(chartCanvas, username, currentMonth, pageNum)

                // Finish the chart page
                pdfDocument.finishPage(chartPage)
            } else {
                // Add chart title
                currentY += 30f
                val chartTitlePaint = Paint().apply {
                    color = colorWhite
                    textSize = sectionTitleTextSize
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                newCanvas.drawText("Expense Distribution Chart", pageWidth / 2f, currentY, chartTitlePaint)

                // Draw the doughnut chart
                drawDoughnutChart(newCanvas, currentY + 20f)

                // Add footer
                drawFooter(newCanvas, username, currentMonth, pageNum)

                // Finish the page
                pdfDocument.finishPage(newPage)
            }
        } else {
            // Add category breakdown section on same page
            currentY = drawSectionTitle(canvas, "Expense Breakdown by Category", currentY)
            currentY = drawCategoryBreakdown(canvas, currentY)

            // Check if we need a new page for the doughnut chart
            if (currentY > pageHeight - 450) { // Estimate space needed for chart + legends + footer
                // Finish current page
                pdfDocument.finishPage(page)

                // Create a new page for the chart
                pageNum++
                val chartPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                val chartPage = pdfDocument.startPage(chartPageInfo)
                val chartCanvas = chartPage.canvas

                // Set background color for chart page
                chartCanvas.drawColor(colorBackground)

                // Add chart title
                val chartTitleY = margin.toFloat() + 30f
                val chartTitlePaint = Paint().apply {
                    color = colorWhite
                    textSize = sectionTitleTextSize
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                chartCanvas.drawText("Expense Distribution Chart", pageWidth / 2f, chartTitleY, chartTitlePaint)

                // Draw the doughnut chart
                drawDoughnutChart(chartCanvas, chartTitleY + 20f)

                // Add footer
                drawFooter(chartCanvas, username, currentMonth, pageNum)

                // Finish the chart page
                pdfDocument.finishPage(chartPage)
            } else {
                // Add chart title
                currentY += 30f
                val chartTitlePaint = Paint().apply {
                    color = colorWhite
                    textSize = sectionTitleTextSize
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("Expense Distribution Chart", pageWidth / 2f, currentY, chartTitlePaint)

                // Draw the doughnut chart
                drawDoughnutChart(canvas, currentY + 20f)

                // Add footer
                drawFooter(canvas, username, currentMonth, pageNum)

                // Finish the page
                pdfDocument.finishPage(page)
            }
        }
    }

    private fun drawTitle(canvas: Canvas, title: String, y: Float): Float {
        val paint = Paint().apply {
            color = colorWhite
            textSize = titleTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(title, pageWidth / 2f, y + titleTextSize, paint)
        return y + titleTextSize + 10f
    }

    /**
     * Draws the subtitle on the canvas
     * @return The new Y position after drawing
     */
    private fun drawSubtitle(canvas: Canvas, subtitle: String, y: Float): Float {
        val paint = Paint().apply {
            color = colorWhite
            textSize = subtitleTextSize
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(subtitle, pageWidth / 2f, y + subtitleTextSize, paint)
        return y + subtitleTextSize + 30f
    }

    private fun drawSectionTitle(canvas: Canvas, title: String, y: Float): Float {
        val paint = Paint().apply {
            color = colorWhite
            textSize = sectionTitleTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw the title
        canvas.drawText(title, margin.toFloat(), y + sectionTitleTextSize, paint)

        // Draw a line under the title
        val linePaint = Paint().apply {
            color = colorWhite
            strokeWidth = 2f
        }

        canvas.drawLine(
            margin.toFloat(),
            y + sectionTitleTextSize + 5f,
            pageWidth - margin.toFloat(),
            y + sectionTitleTextSize + 5f,
            linePaint
        )

        return y + sectionTitleTextSize + 20f
    }

    private fun drawFinancialSummary(canvas: Canvas, y: Float): Float {
        // Get financial data
        val monthlyIncome = preferencesManager.getMonthlyIncome()
        val monthlyExpenses = preferencesManager.getMonthlyExpenses()
        val balance = monthlyIncome - monthlyExpenses
        val budgetLeft = preferencesManager.getBudgetLeft()

        // Set up paints
        val labelPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            textAlign = Paint.Align.LEFT
        }

        val valuePaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            textAlign = Paint.Align.RIGHT
        }

        val rowHeight = 40f
        val tableWidth = pageWidth - 2 * margin
        val labelWidth = tableWidth * 0.6f
        val valueWidth = tableWidth * 0.4f

        // Draw table rows
        var currentY = y

        // Row 1: Total Income
        drawTableRow(canvas, "Total Income:", currencyFormatter.format(monthlyIncome),
            margin.toFloat(), currentY, labelWidth, valueWidth, labelPaint, valuePaint, false)
        currentY += rowHeight

        // Row 2: Total Expenses
        drawTableRow(canvas, "Total Expenses:", currencyFormatter.format(monthlyExpenses),
            margin.toFloat(), currentY, labelWidth, valueWidth, labelPaint, valuePaint, true)
        currentY += rowHeight

        // Row 3: Balance
        drawTableRow(canvas, "Balance:", currencyFormatter.format(balance),
            margin.toFloat(), currentY, labelWidth, valueWidth, labelPaint, valuePaint, false)
        currentY += rowHeight

        // Row 4: Budget Left
        drawTableRow(canvas, "Budget Left:", currencyFormatter.format(budgetLeft),
            margin.toFloat(), currentY, labelWidth, valueWidth, labelPaint, valuePaint, true)
        currentY += rowHeight

        return currentY + 20f // Add some extra space after the table
    }

    private fun drawTableRow(canvas: Canvas, label: String, value: String, x: Float, y: Float,
                            labelWidth: Float, valueWidth: Float, labelPaint: Paint, valuePaint: Paint, isEvenRow: Boolean) {
        // Draw row background
        val rowPaint = Paint().apply {
            color = if (isEvenRow) Color.rgb(51, 51, 51) else Color.rgb(34, 34, 34)
            style = Paint.Style.FILL
        }

        canvas.drawRect(x, y, x + labelWidth + valueWidth, y + 40f, rowPaint)

        // Draw label and value
        canvas.drawText(label, x + 10f, y + 25f, labelPaint)
        canvas.drawText(value, x + labelWidth + valueWidth - 10f, y + 25f, valuePaint)
    }

    private fun drawTransactions(canvas: Canvas, y: Float): Float {
        // Get transactions
        val transactions = preferencesManager.getTransactions()

        if (transactions.isEmpty()) {
            // Draw no data message
            val paint = Paint().apply {
                color = colorWhite
                textSize = normalTextSize
                textAlign = Paint.Align.LEFT
                textSkewX = -0.25f // Italic
            }

            canvas.drawText("No transactions recorded for this month.", margin.toFloat(), y + 20f, paint)
            return y + 40f
        }

        // Sort transactions by date (newest first)
        val sortedTransactions = transactions.sortedByDescending { it.date.time }

        // Limit to 10 most recent transactions
        val recentTransactions = if (sortedTransactions.size > 10) {
            sortedTransactions.take(10)
        } else {
            sortedTransactions
        }

        // Set up table dimensions
        val tableWidth = pageWidth - 2 * margin
        val dateWidth = tableWidth * 0.35f
        val categoryWidth = tableWidth * 0.25f
        val typeWidth = tableWidth * 0.15f
        val amountWidth = tableWidth * 0.25f
        val rowHeight = 40f

        // Set up paints
        val headerPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val headerBgPaint = Paint().apply {
            color = colorPrimary
            style = Paint.Style.FILL
        }

        val cellPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            textAlign = Paint.Align.LEFT
        }

        val amountIncomePaint = Paint().apply {
            color = colorIncome
            textSize = normalTextSize
            textAlign = Paint.Align.RIGHT
        }

        val amountExpensePaint = Paint().apply {
            color = colorExpense
            textSize = normalTextSize
            textAlign = Paint.Align.RIGHT
        }

        // Draw header row
        var currentX = margin.toFloat()
        var currentY = y

        // Header background
        canvas.drawRect(currentX, currentY, currentX + tableWidth, currentY + rowHeight, headerBgPaint)

        // Header cells
        currentX += 10f // Padding
        canvas.drawText("Date & Time", currentX, currentY + 25f, headerPaint)
        currentX += dateWidth
        canvas.drawText("Category", currentX, currentY + 25f, headerPaint)
        currentX += categoryWidth
        canvas.drawText("Type", currentX, currentY + 25f, headerPaint)
        currentX += typeWidth

        // Amount header is right-aligned
        val amountHeaderPaint = Paint(headerPaint)
        amountHeaderPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", currentX + amountWidth - 10f, currentY + 25f, amountHeaderPaint)

        currentY += rowHeight

        // Draw transaction rows
        recentTransactions.forEachIndexed { index, transaction ->
            val isEvenRow = index % 2 == 0
            val rowBgPaint = Paint().apply {
                color = if (isEvenRow) Color.rgb(51, 51, 51) else Color.rgb(34, 34, 34)
                style = Paint.Style.FILL
            }

            // Row background
            canvas.drawRect(margin.toFloat(), currentY, margin.toFloat() + tableWidth, currentY + rowHeight, rowBgPaint)

            // Cells
            currentX = margin.toFloat() + 10f // Padding

            // Date cell
            canvas.drawText(dateTimeFormatter.format(transaction.date), currentX, currentY + 25f, cellPaint)
            currentX += dateWidth

            // Category cell
            canvas.drawText(transaction.category.displayName, currentX, currentY + 25f, cellPaint)
            currentX += categoryWidth

            // Type cell
            canvas.drawText(transaction.type.name, currentX, currentY + 25f, cellPaint)
            currentX += typeWidth

            // Amount cell
            val amountText = if (transaction.type == TransactionType.INCOME)
                "+${currencyFormatter.format(transaction.amount)}"
            else
                "-${currencyFormatter.format(transaction.amount)}"

            val amountPaint = if (transaction.type == TransactionType.INCOME) amountIncomePaint else amountExpensePaint
            canvas.drawText(amountText, currentX + amountWidth - 10f, currentY + 25f, amountPaint)

            currentY += rowHeight
        }

        // Add note if showing limited transactions
        if (sortedTransactions.size > 10) {
            val notePaint = Paint().apply {
                color = colorWhite
                textSize = smallTextSize
                textAlign = Paint.Align.LEFT
                textSkewX = -0.25f // Italic
            }

            canvas.drawText(
                "* Showing 10 most recent transactions out of ${sortedTransactions.size} total.",
                margin.toFloat(),
                currentY + 20f,
                notePaint
            )

            currentY += 40f
        } else {
            currentY += 20f
        }

        return currentY
    }

    private fun drawCategoryBreakdown(canvas: Canvas, y: Float): Float {
        // Get category expenses
        val categoryExpenses = if (preferencesManager.isBudgetReset()) {
            preferencesManager.getExpensesSinceLastReset()
        } else {
            preferencesManager.getCategoryExpenses()
        }

        // Filter out income categories
        val expenseCategories = categoryExpenses.filter { (category, _) ->
            category != Category.SALARY && category != Category.SIDE_BUSINESS
        }

        if (expenseCategories.isEmpty()) {
            // Draw no data message
            val paint = Paint().apply {
                color = colorWhite
                textSize = normalTextSize
                textAlign = Paint.Align.LEFT
                textSkewX = -0.25f // Italic
            }

            canvas.drawText("No expense data recorded for this month.", margin.toFloat(), y + 20f, paint)
            return y + 40f
        }

        // Calculate total expenses
        val totalExpenses = expenseCategories.values.sum()

        // Set up table dimensions
        val tableWidth = pageWidth - 2 * margin
        val categoryWidth = tableWidth * 0.5f
        val amountWidth = tableWidth * 0.3f
        val percentWidth = tableWidth * 0.2f
        val rowHeight = 40f

        // Set up paints
        val headerPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val headerBgPaint = Paint().apply {
            color = colorPrimary
            style = Paint.Style.FILL
        }

        val cellPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            textAlign = Paint.Align.LEFT
        }

        val amountPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            textAlign = Paint.Align.RIGHT
        }

        val percentPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            textAlign = Paint.Align.CENTER
        }

        // Draw header row
        var currentX = margin.toFloat()
        var currentY = y

        // Header background
        canvas.drawRect(currentX, currentY, currentX + tableWidth, currentY + rowHeight, headerBgPaint)

        // Header cells
        currentX += 10f // Padding
        canvas.drawText("Category", currentX, currentY + 25f, headerPaint)
        currentX += categoryWidth

        // Amount header is right-aligned
        val amountHeaderPaint = Paint(headerPaint)
        amountHeaderPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Amount", currentX + amountWidth - 10f, currentY + 25f, amountHeaderPaint)
        currentX += amountWidth

        // Percentage header is center-aligned
        val percentHeaderPaint = Paint(headerPaint)
        percentHeaderPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("% of Total", currentX + percentWidth / 2, currentY + 25f, percentHeaderPaint)

        currentY += rowHeight

        // Draw category rows
        expenseCategories.entries.forEachIndexed { index, (category, amount) ->
            val isEvenRow = index % 2 == 0
            val rowBgPaint = Paint().apply {
                color = if (isEvenRow) Color.rgb(51, 51, 51) else Color.rgb(34, 34, 34)
                style = Paint.Style.FILL
            }

            val percentage = if (totalExpenses > 0) (amount / totalExpenses * 100) else 0.0

            // Row background
            canvas.drawRect(margin.toFloat(), currentY, margin.toFloat() + tableWidth, currentY + rowHeight, rowBgPaint)

            // Cells
            currentX = margin.toFloat() + 10f // Padding

            // Category cell
            canvas.drawText(category.displayName, currentX, currentY + 25f, cellPaint)
            currentX += categoryWidth

            // Amount cell
            canvas.drawText(currencyFormatter.format(amount), currentX + amountWidth - 10f, currentY + 25f, amountPaint)
            currentX += amountWidth

            // Percentage cell
            canvas.drawText(String.format("%.1f%%", percentage), currentX + percentWidth / 2, currentY + 25f, percentPaint)

            currentY += rowHeight
        }

        // Draw total row
        val totalBgPaint = Paint().apply {
            color = colorPrimary
            style = Paint.Style.FILL
        }

        // Total row background
        canvas.drawRect(margin.toFloat(), currentY, margin.toFloat() + tableWidth, currentY + rowHeight, totalBgPaint)

        // Total cells
        currentX = margin.toFloat() + 10f // Padding

        // Total label cell
        val totalLabelPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("TOTAL", currentX, currentY + 25f, totalLabelPaint)
        currentX += categoryWidth

        // Total amount cell
        val totalAmountPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(currencyFormatter.format(totalExpenses), currentX + amountWidth - 10f, currentY + 25f, totalAmountPaint)
        currentX += amountWidth

        // Total percentage cell
        val totalPercentPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("100%", currentX + percentWidth / 2, currentY + 25f, totalPercentPaint)

        currentY += rowHeight + 20f // Add some extra space after the table

        return currentY
    }

    private fun drawDoughnutChart(canvas: Canvas, y: Float): Float {
        // Get category expenses
        val categoryExpenses = if (preferencesManager.isBudgetReset()) {
            preferencesManager.getExpensesSinceLastReset()
        } else {
            preferencesManager.getCategoryExpenses()
        }

        // Filter out income categories
        val expenseCategories = categoryExpenses.filter { (category, _) ->
            category != Category.SALARY && category != Category.SIDE_BUSINESS
        }

        if (expenseCategories.isEmpty()) {
            // Draw no data message
            val paint = Paint().apply {
                color = colorWhite
                textSize = normalTextSize
                textAlign = Paint.Align.CENTER
                textSkewX = -0.25f // Italic
            }

            canvas.drawText("No expense data to display in chart.", pageWidth / 2f, y + 20f, paint)
            return y + 40f
        }

        // Calculate total expenses
        val totalExpenses = expenseCategories.values.sum()

        // Set up chart dimensions
        val chartCenterX = pageWidth / 2f
        val chartCenterY = y + 150f
        val outerRadius = 130f  // Slightly larger outer radius
        val innerRadius = 50f   // Slightly smaller inner radius for better proportions

        // Set up colors for categories (using a predefined set of colors)
        val categoryColors = mapOf(
            Category.FOOD to Color.rgb(255, 152, 0),         // Orange
            Category.ENTERTAINMENT to Color.rgb(76, 175, 80),  // Green
            Category.TRANSPORT to Color.rgb(33, 150, 243),     // Blue
            Category.HEALTH to Color.rgb(244, 67, 54),         // Red
            Category.SHOPPING to Color.rgb(156, 39, 176),      // Purple
            Category.VACATION to Color.rgb(63, 81, 181)        // Indigo
        )

        // Draw the doughnut chart
        var startAngle = 0f
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Draw each category slice
        expenseCategories.forEach { (category, amount) ->
            val sweepAngle = (amount / totalExpenses * 360).toFloat()

            // Set the color for this category
            paint.color = categoryColors[category] ?: Color.GRAY

            // Draw the arc with rounded corners
            val rectF = RectF(chartCenterX - outerRadius, chartCenterY - outerRadius,
                             chartCenterX + outerRadius, chartCenterY + outerRadius)
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)

            // Move to next angle
            startAngle += sweepAngle
        }

        // Draw the inner circle (hole) to create the doughnut effect
        paint.color = colorBackground
        canvas.drawCircle(chartCenterX, chartCenterY, innerRadius, paint)

        // Draw the total in the center
        val textPaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Total", chartCenterX, chartCenterY - 10f, textPaint)
        canvas.drawText(currencyFormatter.format(totalExpenses), chartCenterX, chartCenterY + 15f, textPaint)

        // Draw legends
        val legendStartY = chartCenterY + outerRadius + 40f
        val legendItemHeight = 30f
        val legendTextPaint = Paint().apply {
            color = colorWhite
            textSize = smallTextSize
            textAlign = Paint.Align.LEFT
        }

        // Draw a title for the legend
        val legendTitlePaint = Paint().apply {
            color = colorWhite
            textSize = normalTextSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Expense Categories", chartCenterX, legendStartY - 10f, legendTitlePaint)

        // Calculate legend layout (2 columns if more than 3 categories)
        val useColumns = expenseCategories.size > 3
        val columnsCount = if (useColumns) 2 else 1
        val itemsPerColumn = Math.ceil(expenseCategories.size.toDouble() / columnsCount).toInt()
        val columnWidth = pageWidth / (columnsCount + 1)

        // Draw each legend item
        expenseCategories.entries.forEachIndexed { index, (category, amount) ->
            val columnIndex = if (useColumns) index / itemsPerColumn else 0
            val rowIndex = index % itemsPerColumn

            val legendX = chartCenterX - (columnWidth / 2) + (columnIndex * columnWidth) - 40f
            val legendY = legendStartY + (rowIndex * legendItemHeight) + 20f

            // Draw color box
            val colorBoxPaint = Paint().apply {
                color = categoryColors[category] ?: Color.GRAY
                style = Paint.Style.FILL
            }
            canvas.drawRect(legendX, legendY - 10f, legendX + 15f, legendY + 5f, colorBoxPaint)

            // Draw category name and percentage
            val percentage = (amount / totalExpenses * 100).toInt()
            canvas.drawText(
                "${category.displayName} (${percentage}%)",
                legendX + 25f,
                legendY,
                legendTextPaint
            )
        }

        // Return the new Y position after the legend with fixed spacing
        // Ensure there's at least 100 points of space between the end of the legend and the bottom of the page
        return legendStartY + (Math.min(itemsPerColumn, expenseCategories.size) * legendItemHeight) + 100f
    }

    private fun drawFooter(canvas: Canvas, username: String, currentMonth: String, pageNum: Int) {
        // Always position the footer at a fixed distance from the bottom of the page
        val footerY = pageHeight - 20f  // 20 points from the bottom

        // Draw a separator line above the footer
        val linePaint = Paint().apply {
            color = colorWhite
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(
            pageWidth / 4f,
            footerY - 45f,
            pageWidth * 3f / 4f,
            footerY - 45f,
            linePaint
        )

        // Footer text
        val footerText1 = "Meloch Financial Report - $currentMonth - $username"
        val footerText2 = "Generated on ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"
        val footerText3 = "Page $pageNum"

        // Footer paint
        val footerPaint = Paint().apply {
            color = colorWhite
            textSize = smallTextSize
            textAlign = Paint.Align.CENTER
        }

        // Draw footer text
        canvas.drawText(footerText1, pageWidth / 2f, footerY - 30f, footerPaint)
        canvas.drawText(footerText2, pageWidth / 2f, footerY - 15f, footerPaint)
        canvas.drawText(footerText3, pageWidth / 2f, footerY, footerPaint)
    }

    fun sharePdfReport(pdfUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "Meloch Financial Report - ${dateFormatter.format(Date())}")
            putExtra(Intent.EXTRA_TEXT, "Please find attached my financial report generated by Meloch app.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Financial Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
