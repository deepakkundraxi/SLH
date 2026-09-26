package com.slh.app

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ResultAnalyticsExport {

    // ============================================================
    // PDF EXPORT
    // ============================================================

    fun exportPdf(
        context: Context,
        coachingId: String,
        batch: CoachingBatch,
        students: List<Student>,
        tests: List<CoachingTest>
    ): Uri? {

        return try {

            val fileName =
                "Result_Analytics_${safeFileName(batch.name)}.pdf"

            val file =
                File(
                    context.cacheDir,
                    fileName
                )

            val document =
                PdfDocument()

            val pageWidth = 595
            val pageHeight = 842

            var pageNumber = 1

            var page =
                document.startPage(
                    PdfDocument.PageInfo.Builder(
                        pageWidth,
                        pageHeight,
                        pageNumber
                    ).create()
                )

            var canvas = page.canvas

            val titlePaint =
                Paint().apply {
                    textSize = 22f
                    isFakeBoldText = true
                }

            val headingPaint =
                Paint().apply {
                    textSize = 16f
                    isFakeBoldText = true
                }

            val normalPaint =
                Paint().apply {
                    textSize = 11f
                }

            val smallPaint =
                Paint().apply {
                    textSize = 9f
                }

            var y = 40f

            fun newPageIfNeeded(
                requiredHeight: Float = 24f
            ) {

                if (
                    y + requiredHeight <
                    pageHeight - 35
                ) {
                    return
                }

                document.finishPage(page)

                pageNumber++

                page =
                    document.startPage(
                        PdfDocument.PageInfo.Builder(
                            pageWidth,
                            pageHeight,
                            pageNumber
                        ).create()
                    )

                canvas = page.canvas
                y = 40f
            }

            fun drawText(
                text: String,
                paint: Paint = normalPaint,
                x: Float = 35f
            ) {

                newPageIfNeeded(22f)

                canvas.drawText(
                    text,
                    x,
                    y,
                    paint
                )

                y += paint.textSize + 7f
            }

            fun drawDivider() {

                newPageIfNeeded(12f)

                canvas.drawLine(
                    35f,
                    y,
                    560f,
                    y,
                    smallPaint
                )

                y += 10f
            }

            fun drawHeading(
                text: String
            ) {

                newPageIfNeeded(30f)

                y += 6f

                drawText(
                    text = text,
                    paint = headingPaint
                )

                y += 4f
            }

            val batchSummaries =
                tests.mapNotNull {
                    TestStore.getTestResultSummary(it.id)
                }

            val enteredResults =
                batchSummaries.sumOf {
                    it.enteredResults
                }

            val publishedResults =
                batchSummaries.sumOf {
                    it.publishedResults
                }

            val passedResults =
                batchSummaries.sumOf {
                    it.passedResults
                }

            val failedResults =
                batchSummaries.sumOf {
                    it.failedResults
                }

            val averagePercentage =
                if (batchSummaries.isNotEmpty()) {
                    batchSummaries
                        .map {
                            it.averagePercentage
                        }
                        .average()
                } else {
                    0.0
                }

            val studentAnalytics =
                buildStudentAnalytics(
                    students = students,
                    tests = tests
                )

            drawText(
                text = "SOHAN'S LEARNING HUB",
                paint = titlePaint
            )

            drawText(
                text = "Result Analytics Report",
                paint = headingPaint
            )

            y += 4f

            drawDivider()

            drawText(
                "Batch: ${batch.name}",
                headingPaint
            )

            if (batch.code.isNotBlank()) {

                drawText(
                    "Batch Code: ${batch.code}"
                )
            }

            if (batch.course.isNotBlank()) {

                drawText(
                    "Course: ${batch.course}"
                )
            }

            if (batch.startDate.isNotBlank()) {

                drawText(
                    "Start Date: ${batch.startDate}"
                )
            }

            if (batch.endDate.isNotBlank()) {

                drawText(
                    "End Date: ${batch.endDate}"
                )
            }

            drawDivider()

            drawHeading(
                "Batch Overview"
            )

            drawText(
                "Total Students: ${students.size}"
            )

            drawText(
                "Total Tests: ${tests.size}"
            )

            drawText(
                "Entered Results: $enteredResults"
            )

            drawText(
                "Published Results: $publishedResults"
            )

            drawText(
                "Passed Results: $passedResults"
            )

            drawText(
                "Failed Results: $failedResults"
            )

            drawText(
                "Overall Average: ${formatNumber(averagePercentage)}%"
            )

            drawHeading(
                "Student Overall Performance"
            )

            if (studentAnalytics.isEmpty()) {

                drawText(
                    "No published student results available."
                )

            } else {

                studentAnalytics.forEach { row ->

                    newPageIfNeeded(90f)

                    drawText(
                        "#${row.rank}  ${row.student.name}",
                        headingPaint
                    )

                    if (
                        row.student.studentId.isNotBlank()
                    ) {

                        drawText(
                            "Student ID: ${row.student.studentId}",
                            smallPaint
                        )
                    }

                    drawText(
                        "Tests: ${row.attempted}/${tests.size}  " +
                                "PASS: ${row.passed}  " +
                                "FAIL: ${row.failed}"
                    )

                    drawText(
                        "Average: ${formatNumber(row.average)}%  " +
                                "Highest: ${formatNumber(row.highest)}%"
                    )

                    drawText(
                        "Marks: ${formatNumber(row.obtained)} / " +
                                formatNumber(row.possible)
                    )

                    drawDivider()
                }
            }

            drawHeading(
                "Test-wise Analytics"
            )

            if (tests.isEmpty()) {

                drawText(
                    "No tests available."
                )

            } else {

                tests.forEach { test ->

                    newPageIfNeeded(110f)

                    val summary =
                        TestStore.getTestResultSummary(
                            test.id
                        )

                    drawText(
                        test.title,
                        headingPaint
                    )

                    if (test.subject.isNotBlank()) {

                        drawText(
                            "Subject: ${test.subject}"
                        )
                    }

                    if (test.testDate.isNotBlank()) {

                        drawText(
                            "Date: ${test.testDate}"
                        )
                    }

                    drawText(
                        "Total Marks: " +
                                formatNumber(test.totalMarks)
                    )

                    drawText(
                        "Passing Marks: " +
                                formatNumber(test.passingMarks)
                    )

                    if (summary != null) {

                        drawText(
                            "Students: ${summary.totalStudents}"
                        )

                        drawText(
                            "Entered: ${summary.enteredResults}"
                        )

                        drawText(
                            "Published: ${summary.publishedResults}"
                        )

                        drawText(
                            "Passed: ${summary.passedResults}"
                        )

                        drawText(
                            "Failed: ${summary.failedResults}"
                        )

                        drawText(
                            "Average: " +
                                    "${formatNumber(summary.averagePercentage)}%"
                        )

                        drawText(
                            "Highest: " +
                                    formatNumber(summary.highestMarks)
                        )

                        drawText(
                            "Lowest: " +
                                    formatNumber(summary.lowestMarks)
                        )

                    } else {

                        drawText(
                            "No results entered."
                        )
                    }

                    drawDivider()
                }
            }

            drawText(
                "Generated by Sohan's Learning Hub",
                smallPaint
            )

            drawText(
                "Published results are used for student ranking.",
                smallPaint
            )

            document.finishPage(page)

            FileOutputStream(file).use {
                document.writeTo(it)
            }

            document.close()

            getFileUri(
                context = context,
                file = file
            )

        } catch (
            exception: Exception
        ) {

            null
        }
    }

    // ============================================================
    // REAL XLSX EXPORT
    // ============================================================

    fun exportExcel(
        context: Context,
        coachingId: String,
        batch: CoachingBatch,
        students: List<Student>,
        tests: List<CoachingTest>
    ): Uri? {

        return try {

            val fileName =
                "Result_Analytics_${safeFileName(batch.name)}.xlsx"

            val file =
                File(
                    context.cacheDir,
                    fileName
                )

            val summaries =
                tests.mapNotNull {
                    TestStore.getTestResultSummary(it.id)
                }

            val studentRows =
                buildStudentAnalytics(
                    students = students,
                    tests = tests
                )

            val topPerformers =
                buildTopPerformers(
                    students = students,
                    tests = tests
                )

            ZipOutputStream(
                FileOutputStream(file)
            ).use { zip ->

                // ------------------------------------------------
                // [Content_Types].xml
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "[Content_Types].xml",
                    contentTypesXml()
                )

                // ------------------------------------------------
                // _rels/.rels
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "_rels/.rels",
                    rootRelationshipsXml()
                )

                // ------------------------------------------------
                // xl/workbook.xml
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "xl/workbook.xml",
                    workbookXml()
                )

                // ------------------------------------------------
                // xl/_rels/workbook.xml.rels
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "xl/_rels/workbook.xml.rels",
                    workbookRelationshipsXml()
                )

                // ------------------------------------------------
                // xl/styles.xml
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "xl/styles.xml",
                    stylesXml()
                )

                // ------------------------------------------------
                // Sheet 1
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "xl/worksheets/sheet1.xml",
                    buildBatchOverviewSheet(
                        batch = batch,
                        students = students,
                        tests = tests,
                        summaries = summaries
                    )
                )

                // ------------------------------------------------
                // Sheet 2
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "xl/worksheets/sheet2.xml",
                    buildStudentPerformanceSheet(
                        rows = studentRows,
                        totalTests = tests.size
                    )
                )

                // ------------------------------------------------
                // Sheet 3
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "xl/worksheets/sheet3.xml",
                    buildTestAnalyticsSheet(
                        tests = tests
                    )
                )

                // ------------------------------------------------
                // Sheet 4
                // ------------------------------------------------

                addZipEntry(
                    zip,
                    "xl/worksheets/sheet4.xml",
                    buildTopPerformersSheet(
                        rows = topPerformers
                    )
                )
            }

            getFileUri(
                context = context,
                file = file
            )

        } catch (
            exception: Exception
        ) {

            null
        }
    }

    // ============================================================
    // SHARE
    // ============================================================

    fun shareFile(
        context: Context,
        uri: Uri,
        mimeType: String,
        title: String
    ) {

        val shareIntent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type = mimeType

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        context.startActivity(
            Intent.createChooser(
                shareIntent,
                title
            )
        )
    }

    // ============================================================
    // STUDENT ANALYTICS
    // ============================================================

    private fun buildStudentAnalytics(
        students: List<Student>,
        tests: List<CoachingTest>
    ): List<StudentExportRow> {

        return students
            .map { student ->

                val performances =
                    tests.mapNotNull { test ->

                        TestStore
                            .getStudentPerformance(
                                testId = test.id,
                                includeUnpublished = false
                            )
                            .firstOrNull {
                                it.studentId == student.id &&
                                        it.published
                            }
                    }

                val attempted =
                    performances.size

                val passed =
                    performances.count {
                        it.passed
                    }

                val failed =
                    performances.count {
                        !it.passed
                    }

                val average =
                    if (attempted > 0) {

                        performances
                            .map {
                                it.percentage
                            }
                            .average()

                    } else {
                        0.0
                    }

                val highest =
                    performances.maxOfOrNull {
                        it.percentage
                    } ?: 0.0

                val obtained =
                    performances.sumOf {
                        it.obtainedMarks
                    }

                val possible =
                    performances.sumOf {
                        it.totalMarks
                    }

                StudentExportRow(
                    student = student,
                    attempted = attempted,
                    passed = passed,
                    failed = failed,
                    average = average,
                    highest = highest,
                    obtained = obtained,
                    possible = possible
                )
            }
            .filter {
                it.attempted > 0
            }
            .sortedWith(
                compareByDescending<StudentExportRow> {
                    it.average
                }
                    .thenByDescending {
                        it.highest
                    }
                    .thenBy {
                        it.student.name.lowercase(
                            Locale.getDefault()
                        )
                    }
            )
            .mapIndexed { index, row ->

                row.copy(
                    rank = index + 1
                )
            }
    }

    // ============================================================
    // TOP PERFORMERS
    // ============================================================

    private fun buildTopPerformers(
        students: List<Student>,
        tests: List<CoachingTest>
    ): List<TopPerformerExportRow> {

        return tests
            .flatMap { test ->

                TestStore
                    .getTopStudentPerformances(
                        testId = test.id,
                        limit = 3,
                        publishedOnly = true
                    )
                    .map { performance ->

                        TopPerformerExportRow(
                            student =
                                students.firstOrNull {
                                    it.id ==
                                            performance.studentId
                                },
                            test = test,
                            performance = performance
                        )
                    }
            }
            .sortedByDescending {
                it.performance.percentage
            }
            .take(3)
            .mapIndexed { index, row ->

                row.copy(
                    overallPosition = index + 1
                )
            }
    }

    // ============================================================
    // XLSX SHEET 1
    // ============================================================

    private fun buildBatchOverviewSheet(
        batch: CoachingBatch,
        students: List<Student>,
        tests: List<CoachingTest>,
        summaries: List<TestResultSummary>
    ): String {

        val rows =
            mutableListOf<List<Any?>>()

        rows.add(
            listOf(
                "SOHAN'S LEARNING HUB"
            )
        )

        rows.add(
            listOf(
                "Result Analytics - Batch Overview"
            )
        )

        rows.add(
            listOf(
                ""
            )
        )

        rows.add(
            listOf(
                "Field",
                "Value"
            )
        )

        rows.add(
            listOf(
                "Batch",
                batch.name
            )
        )

        rows.add(
            listOf(
                "Batch Code",
                batch.code
            )
        )

        rows.add(
            listOf(
                "Course",
                batch.course
            )
        )

        rows.add(
            listOf(
                "Start Date",
                batch.startDate
            )
        )

        rows.add(
            listOf(
                "End Date",
                batch.endDate
            )
        )

        rows.add(
            listOf(
                "Total Students",
                students.size
            )
        )

        rows.add(
            listOf(
                "Total Tests",
                tests.size
            )
        )

        rows.add(
            listOf(
                "Entered Results",
                summaries.sumOf {
                    it.enteredResults
                }
            )
        )

        rows.add(
            listOf(
                "Published Results",
                summaries.sumOf {
                    it.publishedResults
                }
            )
        )

        rows.add(
            listOf(
                "Passed Results",
                summaries.sumOf {
                    it.passedResults
                }
            )
        )

        rows.add(
            listOf(
                "Failed Results",
                summaries.sumOf {
                    it.failedResults
                }
            )
        )

        val average =
            if (summaries.isNotEmpty()) {

                summaries
                    .map {
                        it.averagePercentage
                    }
                    .average()

            } else {
                0.0
            }

        rows.add(
            listOf(
                "Overall Average %",
                formatNumber(average)
            )
        )

        rows.add(
            listOf(
                ""
            )
        )

        rows.add(
            listOf(
                "Report Information",
                "Published results only"
            )
        )

        return buildWorksheetXml(rows)
    }

    // ============================================================
    // XLSX SHEET 2
    // ============================================================

    private fun buildStudentPerformanceSheet(
        rows: List<StudentExportRow>,
        totalTests: Int
    ): String {

        val sheetRows =
            mutableListOf<List<Any?>>()

        sheetRows.add(
            listOf(
                "Student Overall Performance"
            )
        )

        sheetRows.add(
            listOf(
                "Published results are used for ranking."
            )
        )

        sheetRows.add(
            listOf(
                ""
            )
        )

        sheetRows.add(
            listOf(
                "Rank",
                "Student ID",
                "Student Name",
                "Tests Attempted",
                "Total Tests",
                "PASS",
                "FAIL",
                "Average %",
                "Highest %",
                "Obtained Marks",
                "Total Marks"
            )
        )

        rows.forEach { row ->

            sheetRows.add(
                listOf(
                    row.rank,
                    row.student.studentId,
                    row.student.name,
                    row.attempted,
                    totalTests,
                    row.passed,
                    row.failed,
                    formatNumber(row.average),
                    formatNumber(row.highest),
                    formatNumber(row.obtained),
                    formatNumber(row.possible)
                )
            )
        }

        return buildWorksheetXml(
            sheetRows
        )
    }

    // ============================================================
    // XLSX SHEET 3
    // ============================================================

    private fun buildTestAnalyticsSheet(
        tests: List<CoachingTest>
    ): String {

        val rows =
            mutableListOf<List<Any?>>()

        rows.add(
            listOf(
                "Test-wise Analytics"
            )
        )

        rows.add(
            listOf(
                ""
            )
        )

        rows.add(
            listOf(
                "Test",
                "Subject",
                "Date",
                "Total Marks",
                "Passing Marks",
                "Students",
                "Entered",
                "Published",
                "Passed",
                "Failed",
                "Average %",
                "Highest",
                "Lowest"
            )
        )

        tests.forEach { test ->

            val summary =
                TestStore.getTestResultSummary(
                    test.id
                )

            rows.add(
                listOf(
                    test.title,
                    test.subject,
                    test.testDate,
                    formatNumber(
                        test.totalMarks
                    ),
                    formatNumber(
                        test.passingMarks
                    ),
                    summary?.totalStudents ?: 0,
                    summary?.enteredResults ?: 0,
                    summary?.publishedResults ?: 0,
                    summary?.passedResults ?: 0,
                    summary?.failedResults ?: 0,
                    summary?.averagePercentage?.let {
                        formatNumber(it)
                    } ?: "0",
                    summary?.highestMarks?.let {
                        formatNumber(it)
                    } ?: "0",
                    summary?.lowestMarks?.let {
                        formatNumber(it)
                    } ?: "0"
                )
            )
        }

        return buildWorksheetXml(
            rows
        )
    }

    // ============================================================
    // XLSX SHEET 4
    // ============================================================

    private fun buildTopPerformersSheet(
        rows: List<TopPerformerExportRow>
    ): String {

        val sheetRows =
            mutableListOf<List<Any?>>()

        sheetRows.add(
            listOf(
                "Top 3 Performers"
            )
        )

        sheetRows.add(
            listOf(
                ""
            )
        )

        sheetRows.add(
            listOf(
                "Position",
                "Student ID",
                "Student Name",
                "Test",
                "Subject",
                "Percentage",
                "Obtained Marks",
                "Total Marks"
            )
        )

        rows.forEach { row ->

            sheetRows.add(
                listOf(
                    row.overallPosition,
                    row.student?.studentId ?: "",
                    row.student?.name ?: "Student",
                    row.test.title,
                    row.test.subject,
                    formatNumber(
                        row.performance.percentage
                    ),
                    formatNumber(
                        row.performance.obtainedMarks
                    ),
                    formatNumber(
                        row.performance.totalMarks
                    )
                )
            )
        }

        if (rows.isEmpty()) {

            sheetRows.add(
                listOf(
                    "",
                    "",
                    "No published results available."
                )
            )
        }

        return buildWorksheetXml(
            sheetRows
        )
    }

    // ============================================================
    // XLSX XML GENERATOR
    // ============================================================

    private fun buildWorksheetXml(
        rows: List<List<Any?>>
    ): String {

        val builder =
            StringBuilder()

        builder.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
        )

        builder.append(
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">"""
        )

        builder.append(
            "<sheetData>"
        )

        rows.forEachIndexed { rowIndex, row ->

            val excelRow =
                rowIndex + 1

            builder.append(
                """<row r="$excelRow">"""
            )

            row.forEachIndexed { columnIndex, value ->

                val cellReference =
                    "${excelColumnName(columnIndex + 1)}$excelRow"

                if (value == null) {

                    builder.append(
                        """<c r="$cellReference" t="inlineStr"><is><t></t></is></c>"""
                    )

                } else if (value is Number) {

                    builder.append(
                        """<c r="$cellReference" t="n"><v>${escapeXml(value.toString())}</v></c>"""
                    )

                } else {

                    builder.append(
                        """<c r="$cellReference" t="inlineStr"><is><t>${escapeXml(value.toString())}</t></is></c>"""
                    )
                }
            }

            builder.append(
                "</row>"
            )
        }

        builder.append(
            "</sheetData>"
        )

        builder.append(
            "</worksheet>"
        )

        return builder.toString()
    }

    private fun excelColumnName(
        column: Int
    ): String {

        var number = column
        val result =
            StringBuilder()

        while (number > 0) {

            val remainder =
                (number - 1) % 26

            result.insert(
                0,
                ('A'.code + remainder).toChar()
            )

            number =
                (number - 1) / 26
        }

        return result.toString()
    }

    // ============================================================
    // XLSX FILE STRUCTURE
    // ============================================================

    private fun contentTypesXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            </Types>
        """.trimIndent()
    }

    private fun rootRelationshipsXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship
                    Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                    Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun workbookXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                    <sheet name="Batch Overview" sheetId="1" r:id="rId1"/>
                    <sheet name="Student Performance" sheetId="2" r:id="rId2"/>
                    <sheet name="Test Analytics" sheetId="3" r:id="rId3"/>
                    <sheet name="Top Performers" sheetId="4" r:id="rId4"/>
                </sheets>
            </workbook>
        """.trimIndent()
    }

    private fun workbookRelationshipsXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship
                    Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet1.xml"/>
                <Relationship
                    Id="rId2"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet2.xml"/>
                <Relationship
                    Id="rId3"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet3.xml"/>
                <Relationship
                    Id="rId4"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet4.xml"/>
                <Relationship
                    Id="rId5"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"
                    Target="styles.xml"/>
            </Relationships>
        """.trimIndent()
    }

    private fun stylesXml(): String {

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <fonts count="1">
                    <font>
                        <sz val="11"/>
                        <name val="Calibri"/>
                        <family val="2"/>
                    </font>
                </fonts>

                <fills count="2">
                    <fill>
                        <patternFill patternType="none"/>
                    </fill>
                    <fill>
                        <patternFill patternType="gray125"/>
                    </fill>
                </fills>

                <borders count="1">
                    <border>
                        <left/>
                        <right/>
                        <top/>
                        <bottom/>
                        <diagonal/>
                    </border>
                </borders>

                <cellStyleXfs count="1">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
                </cellStyleXfs>

                <cellXfs count="1">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"
                        xfId="0"/>
                </cellXfs>

                <cellStyles count="1">
                    <cellStyle
                        name="Normal"
                        xfId="0"
                        builtinId="0"/>
                </cellStyles>
            </styleSheet>
        """.trimIndent()
    }

    private fun addZipEntry(
        zip: ZipOutputStream,
        path: String,
        content: String
    ) {

        val entry =
            ZipEntry(path)

        zip.putNextEntry(entry)

        zip.write(
            content.toByteArray(
                Charsets.UTF_8
            )
        )

        zip.closeEntry()
    }

    // ============================================================
    // XML / FILE HELPERS
    // ============================================================

    private fun escapeXml(
        value: String
    ): String {

        return value
            .replace(
                "&",
                "&amp;"
            )
            .replace(
                "<",
                "&lt;"
            )
            .replace(
                ">",
                "&gt;"
            )
            .replace(
                "\"",
                "&quot;"
            )
            .replace(
                "'",
                "&apos;"
            )
    }

    private fun safeFileName(
        value: String
    ): String {

        return value
            .trim()
            .replace(
                Regex("[^A-Za-z0-9._-]+"),
                "_"
            )
            .ifBlank {
                "Batch"
            }
    }

    private fun getFileUri(
        context: Context,
        file: File
    ): Uri {

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun formatNumber(
        value: Double
    ): String {

        return if (value % 1.0 == 0.0) {

            value
                .toInt()
                .toString()

        } else {

            String.format(
                Locale.US,
                "%.2f",
                value
            )
        }
    }

    // ============================================================
    // EXPORT MODELS
    // ============================================================

    private data class StudentExportRow(
        val student: Student,
        val attempted: Int,
        val passed: Int,
        val failed: Int,
        val average: Double,
        val highest: Double,
        val obtained: Double,
        val possible: Double,
        val rank: Int = 0
    )

    private data class TopPerformerExportRow(
        val student: Student?,
        val test: CoachingTest,
        val performance: StudentTestPerformance,
        val overallPosition: Int = 0
    )
}