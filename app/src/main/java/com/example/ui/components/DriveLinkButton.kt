package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DriveLinkButton(
    url: String,
    label: String,
    modifier: Modifier = Modifier,
    isFolder: Boolean = false
) {
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            openExternalLink(context, url)
        },
        modifier = modifier
            .height(36.dp)
            .testTag("open_drive_button"),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF0F766E)
        ),
        border = BorderStroke(1.dp, Color(0xFF99F6E4))
    ) {
        Icon(
            imageVector = if (isFolder) Icons.Default.FolderShared else Icons.Default.OpenInNew,
            contentDescription = "Open Google Drive Resource",
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF0D9488)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (label.isNotBlank()) label else if (isFolder) "Drive Folder" else "Drive Doc",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun openExternalLink(context: Context, rawUrl: String) {
    if (rawUrl.isBlank()) {
        Toast.makeText(context, "No Google Drive URL provided", Toast.LENGTH_SHORT).show()
        return
    }
    val cleanUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
        "https://$rawUrl"
    } else {
        rawUrl
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
