package com.pleos.familyjourney.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** 제목이 있는 카드 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            content()
        }
    }
}

/** 작은 라벨 */
@Composable
fun Chip(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** 지역 배지. 그림 대신 지역 특징 이모지 + 색 원으로 그린다. */
@Composable
fun BadgeMedal(emoji: String, name: String, color: Color, earned: Boolean, size: Dp = 88.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (earned) color else Color.Gray.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (earned) emoji else "?", fontSize = (size.value * 0.45f).sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.size(6.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, color = if (earned) Color.Unspecified else Color.Gray)
    }
}

/** ZXing으로 QR 비트맵 생성 → 차량 화면에 표시 */
@Composable
fun QrCode(content: String, modifier: Modifier = Modifier, size: Dp = 180.dp) {
    val bitmap = remember(content) { qrBitmap(content, 512) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(8.dp)
        ) {
            Image(bitmap.asImageBitmap(), contentDescription = "QR 코드", modifier = Modifier.size(size))
        }
        Spacer(Modifier.size(6.dp))
        Text("폰 카메라로 찍어 참여", style = MaterialTheme.typography.labelMedium)
    }
}

private fun qrBitmap(text: String, px: Int): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, px, px, mapOf(EncodeHintType.MARGIN to 1))
    val pixels = IntArray(px * px) { i -> if (matrix[i % px, i / px]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
    return Bitmap.createBitmap(pixels, px, px, Bitmap.Config.ARGB_8888)
}

/** 비율 막대 (투표 결과 등) */
@Composable
fun RatioBar(label: String, value: Int, total: Int, highlight: Boolean) {
    val ratio = if (total > 0) value.toFloat() / total else 0f
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(label, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
            Text("${value}표")
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                    .height(12.dp)
                    .background(if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
fun Gap(width: Dp = 16.dp) = Spacer(Modifier.width(width))
