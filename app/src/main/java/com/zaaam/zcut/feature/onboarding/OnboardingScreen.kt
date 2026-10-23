package com.zaaam.zcut.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zaaam.zcut.ui.components.ZcutGhostButton
import com.zaaam.zcut.ui.components.ZcutPrimaryButton
import com.zaaam.zcut.ui.theme.ZcutCoral
import com.zaaam.zcut.ui.theme.ZcutCyan
import com.zaaam.zcut.ui.theme.ZcutLime
import com.zaaam.zcut.ui.theme.ZcutSpacing
import com.zaaam.zcut.ui.theme.ZcutViolet
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val title: String,
    val message: String,
    val colors: List<Color>
)

private val SLIDES = listOf(
    OnboardingSlide(
        title = "Semua fitur terbuka",
        message = "Tidak ada langganan, kredit, iklan, atau watermark. Semua tool aktif sejak awal.",
        colors = listOf(ZcutLime, ZcutCyan)
    ),
    OnboardingSlide(
        title = "Diproses di perangkat",
        message = "Edit, render, dan simpan berjalan lokal. Draft tidak pernah dikirim ke server mana pun.",
        colors = listOf(ZcutCyan, ZcutViolet)
    ),
    OnboardingSlide(
        title = "Siap dibagikan",
        message = "Ekspor resolusi penuh lalu bagikan langsung ke aplikasi favoritmu.",
        colors = listOf(ZcutCoral, ZcutViolet)
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { SLIDES.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == SLIDES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(PaddingValues(ZcutSpacing.screen))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            ZcutGhostButton(text = "Lewati", onClick = onFinish)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = ZcutSpacing.lg)
        ) { page ->
            val slide = SLIDES[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(slide.colors.map { it.copy(alpha = 0.75f) }))
                )
                Spacer(Modifier.height(ZcutSpacing.xxl))
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(ZcutSpacing.sm))
                Text(
                    text = slide.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ZcutSpacing.md),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SLIDES.indices.forEach { index ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == pagerState.currentPage) 20.dp else 8.dp, 8.dp),
                    shape = CircleShape,
                    color = if (index == pagerState.currentPage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                ) {}
            }
        }

        ZcutPrimaryButton(
            text = if (isLast) "Mulai" else "Lanjut",
            onClick = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
