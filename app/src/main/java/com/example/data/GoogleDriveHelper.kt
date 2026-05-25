package com.example.data

import android.accounts.Account
import android.content.Context
import androidx.annotation.Keep
import com.google.android.gms.auth.GoogleAuthUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleDriveHelper {

    suspend fun getAccessToken(context: Context, email: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val scope = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile"
                val account = Account(email, "com.google")
                GoogleAuthUtil.getToken(context, account, scope)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun invalidateToken(context: Context, token: String) {
        withContext(Dispatchers.IO) {
            try {
                GoogleAuthUtil.clearToken(context, token)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun findFolder(token: String, folderName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
                val url = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&spaces=drive&fields=files(id)"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bodyStr = response.body?.string() ?: return@withContext null
                    val json = JSONObject(bodyStr)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        files.getJSONObject(0).getString("id")
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun createFolder(token: String, folderName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val jsonBody = JSONObject().apply {
                    put("name", folderName)
                    put("mimeType", "application/vnd.google-apps.folder")
                }
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = RequestBody.create(mediaType, jsonBody.toString())
                
                val request = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files")
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bodyStr = response.body?.string() ?: return@withContext null
                    JSONObject(bodyStr).getString("id")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun findFileInFolder(token: String, fileName: String, parentId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val query = "name = '$fileName' and '$parentId' in parents and trashed = false"
                val url = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}&spaces=drive&fields=files(id)"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bodyStr = response.body?.string() ?: return@withContext null
                    val json = JSONObject(bodyStr)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        files.getJSONObject(0).getString("id")
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun uploadNewFile(token: String, fileName: String, mimeType: String, content: String, parentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val boundary = "upload_boundary_" + System.currentTimeMillis()
                
                val metadata = JSONObject().apply {
                    put("name", fileName)
                    put("parents", JSONArray().put(parentId))
                }
                
                val bodyStr = buildString {
                    append("--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadata.toString())
                    append("\r\n--$boundary\r\n")
                    append("Content-Type: $mimeType; charset=UTF-8\r\n\r\n")
                    append(content)
                    append("\r\n--$boundary--\r\n")
                }
                
                val mediaType = "multipart/related; boundary=$boundary".toMediaTypeOrNull()
                val requestBody = RequestBody.create(mediaType, bodyStr)
                
                val request = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun updateExistingFile(token: String, fileId: String, mimeType: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val mediaType = mimeType.toMediaTypeOrNull()
                val requestBody = RequestBody.create(mediaType, content)
                
                val request = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
                    .addHeader("Authorization", "Bearer $token")
                    .patch(requestBody)
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun uploadFileToDrive(token: String, folderName: String, fileName: String, mimeType: String, content: String): Boolean {
        var folderId = findFolder(token, folderName)
        if (folderId == null) {
            folderId = createFolder(token, folderName)
        }
        val targetFolderId = folderId ?: return false
        
        val existingFileId = findFileInFolder(token, fileName, targetFolderId)
        return if (existingFileId != null) {
            updateExistingFile(token, existingFileId, mimeType, content)
        } else {
            uploadNewFile(token, fileName, mimeType, content, targetFolderId)
        }
    }
}
