package com.example.simpletedu

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class  MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val etQuestion=findViewById<EditText>(R.id.etQuestion)
        val btnSubmit=findViewById<Button> (R.id.btnSubmit)
        val txtResponse=findViewById<TextView> (R. id. txtResponse)


        btnSubmit.setOnClickListener {
            val question = etQuestion.text.toString()
            Toast.makeText(this, question, Toast.LENGTH_SHORT).show()
            getResponse(question) { response ->
                runOnUiThread{
                        txtResponse.text = response
                }
            }
        }
    }
    fun getResponse(question: String, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api.groq.com/openai/v1/chat/completions"

        val requestBody = """
        {
            "messages": [
                {
                    "role": "system",
                    "content": "kamu adalah asisten bernama tedu, tanggapi semua yang orang ceritakan dan katakan kepadamu. beri mereka solusi"
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
                Log.e("error", "API Failed", e)
                callback("API request failed: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body != null) {
                    // Parsing streaming data
                    val lines = body.split("\n")
                    val fullContent = StringBuilder()

                    lines.forEach { line ->
                        if (line.startsWith("data: ")) {
                            val json = line.removePrefix("data: ").trim()
                            if (json != "[DONE]") {
                                try {
                                    val jsonObject = JSONObject(json)
                                    val choices = jsonObject.getJSONArray("choices")
                                    val content = choices.getJSONObject(0)
                                        .getJSONObject("delta")
                                        .optString("content", "")
                                    fullContent.append(content)
                                } catch (e: Exception) {
                                    Log.e("ParseError", "Error parsing JSON: ${e.message}")
                                }
                            }
                        }
                    }

                    // Callback dengan teks lengkap
                    callback(fullContent.toString())
                } else {
                    callback("Empty response")
                }
            }
        })
    }


}