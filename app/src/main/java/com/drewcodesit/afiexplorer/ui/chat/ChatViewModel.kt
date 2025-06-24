package com.drewcodesit.afiexplorer.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drewcodesit.afiexplorer.BuildConfig
import com.drewcodesit.afiexplorer.database.preloaded.VectorDAO
import com.drewcodesit.afiexplorer.utils.Config.cosineSimilarity
import com.drewcodesit.afiexplorer.utils.Config.generateFakeEmbedding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(private val vectorDAO: VectorDAO) : ViewModel() {

    private val generativeModel: GenerativeModel

    init {
        // Configure Gemini model with parameters
        val configBuilder = GenerationConfig.builder()
        configBuilder.topP = 0.4f
        configBuilder.temperature = 0.3f

        /**
         * TODO: Change API Key to AFI Explorer (actual)
         */
        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = configBuilder.build(),
            systemInstruction = content {
                text("You are an AI chatbot named ChatAFI,an expert in Air Force publications. Your primary function is to provide accurate, concise, and relevant answers to user questions based *solely* on provided text from official United States Air Force (USAF) publications.")
            })
    }

    // Public function to ask a question about a specific publication ID and get an AI-generated answer
    fun askGemini(pubId: Int, question: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            val answer = withContext(Dispatchers.IO) {
                generateGeminiAnswer(pubId, question)
            }
            callback(answer)
        }
    }


    // Core logic to generate an answer using the Gemini model and RAG-like context retrieval
    private suspend fun generateGeminiAnswer(pubId: Int, question: String): String? {
        try {
            val vectors = vectorDAO.getVectorsByPubId(pubId)
            Log.e("VECTORS VECTORS VECTORS", "Vectors:: ${vectors.size}")

            // Get embedding of the user's question
            val queryEmbedding = generateFakeEmbedding(question) // Replace with real when available

            // Rank paragraphs by cosine similarity
            val topMatches = vectors.filter { it.embedding != null }
                .sortedByDescending { cosineSimilarity(queryEmbedding, it.embedding!!) }
                .take(3)  // You can adjust

            // Concatenate the most relevant chunks to form the context for the prompt
            val context = topMatches.joinToString("\n") { it.originalText }

            // Create a strict prompt emphasizing reliance only on the provided context
            val prompt =
                """
                    **Answering Rules:**
                    1. Use only the provided context. Do not rely on outside knowledge.
                    2. If the answer is not explicitly in the context, respond: "The answer to your question is not present in the provided context."
                    3. Be concise and directly answer the question.
                    
                    **Context (from Air Force Instruction):**
                    $context
                    
                    **Question:**
                    $question
                    
                    **Answer Instructions:**
                    Respond solely based on the above context. If relevant, cite paragraph, table, or page numbers (e.g., "See Paragraph 14" or "Refer to Table 3-1").
                """
                .trimIndent()

            // Call Gemini to generate a response
            val response = generativeModel.generateContent(prompt)
            Log.e("GEMINI RESPONSE", "Response is: ${response.text}")
            return response.text
        } catch (e: Exception) {
            // Log and return error message if something goes wrong
            Log.e("ChatViewModel", "Gemini error", e)
            return "Error: ${e.localizedMessage ?: "Something went wrong."}"

        }
    }
}