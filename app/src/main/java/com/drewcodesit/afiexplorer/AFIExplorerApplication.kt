package com.drewcodesit.afiexplorer

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.drewcodesit.afiexplorer.database.favorites.FavoriteDatabase
import com.drewcodesit.afiexplorer.database.preloaded.AFITopics
import com.drewcodesit.afiexplorer.database.preloaded.VectorDAO
import com.drewcodesit.afiexplorer.database.preloaded.VectorEntity
import com.drewcodesit.afiexplorer.utils.Config.generateFakeEmbedding
import com.drewcodesit.afiexplorer.utils.objects.DelegatesExt
import com.drewcodesit.afiexplorer.utils.objects.PreloadedAFITopics
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class AFIExplorerApplication : Application() {

    companion object {
        var instance: AFIExplorerApplication by DelegatesExt.notNullSingleValue()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        applyTheme()

        CoroutineScope(Dispatchers.IO).launch {
            val database = FavoriteDatabase.getDatabase(this@AFIExplorerApplication)
            val topicDAO = database.afiTopicDAO()
            val vectorDAO = database.vectorDAO()
            val topics = PreloadedAFITopics.preloadedAFITopics

            topics.forEach { topic ->
                topicDAO?.insertAll(topics)

                val existingVectors = vectorDAO?.getVectorsByPubId(topic.pubId)
                if (existingVectors.isNullOrEmpty()) {
                    extractAndStorePDF(topic, vectorDAO)
                } else {
                    Log.i(":::INSERT VECTORS:::", "Vectors already exist for:: ${topic.title}")
                }
            }
        }
    }

    private suspend fun extractAndStorePDF(publications: AFITopics, vectorDAO: VectorDAO?) {
        try {
            val tempFile = withContext(Dispatchers.IO) {
                File.createTempFile(publications.number, ".pdf")
            }
            Log.e("EXTRACT AND STORE", "${tempFile.name} created")
            withContext(Dispatchers.IO) {
                URL(publications.url).openStream()
            }.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
            extractTextAndStoreVectors(tempFile, publications, vectorDAO!!) // Pass VectorDAO
        } catch (e: Exception) {
            // Handle error appropriately (e.g., log, show a notification)
            Log.e("PDF_DOWNLOAD", "Error downloading ${publications.url}: ${e.message}")
        }
    }

    private suspend fun extractTextAndStoreVectors(
        pdfFile: File, publications: AFITopics, vectorDAO: VectorDAO
    ) {
        try {
            val document = PDDocument.load(pdfFile)
            val text = PDFTextStripper().getText(document)
            document.close() // Make sure to close the document!
            pdfFile.delete()

            Log.d("PDF_TEXT_DEBUG", "Text length for ${publications.title}: ${text.length}")

            storeVectors(text, publications, vectorDAO)
            Log.i("PDF_PROCESSING", "Processed and stored vectors for ${publications.title}")
        } catch (e: Exception) {
            Log.e("PDF_PROCESSING", "Error processing ${publications.title}: ${e.message}")
        }
    }

    private suspend fun storeVectors(text: String, publications: AFITopics, vectorDAO: VectorDAO) {
        val paragraphs =
            text.split("\n\n") // You could also split on single newlines or regex if needed
                .map { it.trim() }.filter {
                    it.length > 50 && // <-- ADDED '&&' HERE
                            // Skip figure captions like "Figure 1: ..."
                            !it.matches(Regex("^Figure\\s+\\d+.*"))
                           // !it.contains("Image")
                    //it.split(" ").size > 5
                }

        Log.d(
            "STORE_VECTORS_DEBUG",
            "Found ${paragraphs.size} paragraphs for ${publications.title} after filtering."
        )

        paragraphs.forEachIndexed {index, paragraph ->
            val embedding = generateFakeEmbedding(paragraph) // or call to Gemini API

            val vectorEntity = VectorEntity(
                pubId = publications.pubId,
                originalText = paragraph,
                embedding = embedding  // <-- Store embedding here
            )

            vectorDAO.addData(vectorEntity)
            Log.d(
                "STORE VECTORS", "Stored paragraph $index with embedding for ${publications.title}"
            )
        }
    }

    // Applies theme from SharedPrefs
    private fun applyTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val modeNight = sharedPreferences.getInt(
            getString(R.string.pref_key_mode_night), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        AppCompatDelegate.setDefaultNightMode(modeNight)
    }
}