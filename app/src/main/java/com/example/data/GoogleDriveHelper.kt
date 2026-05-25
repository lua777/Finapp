package com.example.data

import android.content.Context
import android.util.Log

object GoogleDriveHelper {
    fun getAccessToken(context: Context, email: String): String {
        Log.d("GoogleDriveHelper", "Successfully processed access token request for $email")
        return "ya29.mock_token_fluxo_driver_secret_key"
    }

    fun uploadFileToDrive(
        context: Context,
        token: String,
        folderName: String,
        fileName: String,
        contentType: String,
        content: ByteArray
    ): Boolean {
        Log.d("GoogleDriveHelper", "Mocking file upload: folder='$folderName', fileName='$fileName', size=${content.size} bytes")
        // Simulates a smooth API return for Google Workspace Drive API
        return true
    }

    fun invalidateToken(context: Context, token: String) {
        Log.d("GoogleDriveHelper", "Invalidating mock token")
    }
}
