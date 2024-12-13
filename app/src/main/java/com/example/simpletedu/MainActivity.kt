package com.example.simpletedu

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val etQuestion = findViewById<EditText>(R.id.etQuestion)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val txtBalasan = findViewById<TextView>(R.id.txtBalasan)
        val txtRangkuman = findViewById<TextView>(R.id.txtRangkuman)
        val txtKecemasan = findViewById<TextView>(R.id.txtKecemasan)
        val txtDepresi = findViewById<TextView>(R.id.txtDepresi)
        val txtStress = findViewById<TextView>(R.id.txtStress)
        val txtPoin = findViewById<TextView>(R.id.txtPoin)

        btnSubmit.setOnClickListener {
            val question = etQuestion.text.toString()

            if (question.isBlank()) {
                Toast.makeText(this, "Input tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Mengirim pertanyaan...", Toast.LENGTH_SHORT).show()

            getBalasan(question) { response ->
                runOnUiThread { txtBalasan.text = response }
            }
            getRangkuman(question) { response ->
                runOnUiThread { txtRangkuman.text = response }
            }
            getKecemasan(question) { response ->
                runOnUiThread { txtKecemasan.text = response }
            }
            getDepresi(question) { response ->
                runOnUiThread { txtDepresi.text = response }
            }
            getStress(question) { response ->
                runOnUiThread { txtStress.text = response }
            }
            getPoin(question) { response ->
                runOnUiThread { txtPoin.text = response }
            }
        }
    }

    fun getResponse(apiKey: String, url: String, roleContent: String, question: String, callback: (String) -> Unit) {
        val requestBody = """
        {
            "messages": [
                {
                    "role": "system",
                    "content": "$roleContent"
                },
                {
                    "role": "user",
                    "content": "$question"
                }
            ],
            "model": "llama3-8b-8192",
            "temperature": 0.7,
            "max_tokens": 1800,
            "top_p": 1,
            "stream": true
        }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API Error", "API request failed", e)
                runOnUiThread {
                    callback("API request failed: ${e.localizedMessage}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("API Raw Response", "Body: $body")

                if (body.isNullOrEmpty()) {
                    runOnUiThread {
                        callback("Empty response from server")
                    }
                    return
                }

                try {
                    // Split respons streaming berdasarkan baris
                    val lines = body.split("\n")
                    val fullContent = StringBuilder()

                    for (line in lines) {
                        Log.d("Streaming Line", "Processing line: $line")

                        if (line.startsWith("data: ")) {
                            val jsonPart = line.removePrefix("data: ").trim()

                            if (jsonPart == "[DONE]") {
                                continue
                            }

                            try {
                                val jsonObject = JSONObject(jsonPart)
                                val choices = jsonObject.optJSONArray("choices") ?: continue

                                for (i in 0 until choices.length()) {
                                    val delta = choices.getJSONObject(i).getJSONObject("delta")
                                    val content = delta.optString("content", "")
                                    fullContent.append(content)
                                }
                            } catch (e: Exception) {
                                Log.e("JSON Parse Error", "Error parsing JSON part: ${e.message}")
                            }
                        }
                    }

                    runOnUiThread {
                        if (fullContent.isEmpty()) {
                            callback("No valid content found in server response.")
                        } else {
                            callback(fullContent.toString())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("JSON Parse Error", "Error parsing JSON: ${e.message}")
                    runOnUiThread {
                        callback("Error parsing server response: ${e.message}")
                    }
                }
            }
        })
    }

    // Function to get Balasan (Response)
    private fun getBalasan(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api.groq.com/openai/v1/chat/completions"
        val roleContent = "Kamu adalah asisten yang memberikan tanggapan terhadap input pengguna. awali jawaban dengan '1'"
        getResponse(apiKey, url, roleContent, question, callback)
    }

    // Function to get Rangkuman (Summary)
    private fun getRangkuman(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api.groq.com/openai/v1/chat/completions"
        val roleContent = "Kamu adalah asisten yang merangkum input pengguna menjadi poin-poin penting. awali jawaban dengan '2'"
        getResponse(apiKey, url, roleContent, question, callback)
    }

    // Function to get Kecemasan (Anxiety Evaluation)
    private fun getKecemasan(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api.groq.com/openai/v1/chat/completions"
        val roleContent = "Evaluasi tingkat kecemasan dari input pengguna berdasarkan konteks yang diberikan."
        getResponse(apiKey, url, roleContent, question, callback)
    }

    // Function to get Depresi (Depression Evaluation)
    private fun getDepresi(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api.groq.com/openai/v1/chat/completions"
        val roleContent = "Evaluasi tingkat depresi dari input pengguna berdasarkan konteks yang diberikan."
        getResponse(apiKey, url, roleContent, question, callback)
    }

    // Function to get Stress (Stress Evaluation)
    private fun getStress(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api.groq.com/openai/v1/chat/completions"
        val roleContent = "Evaluasi tingkat stres dari input pengguna berdasarkan konteks yang diberikan."
        getResponse(apiKey, url, roleContent, question, callback)
    }

    // Function to get Poin (Points Evaluation)
    private fun getPoin(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api.groq.com/openai/v1/chat/completions"
        val roleContent = "Hitung poin atau skor berdasarkan masukan pengguna dan konteks yang diberikan."
        getResponse(apiKey, url, roleContent, question, callback)
    }
}
