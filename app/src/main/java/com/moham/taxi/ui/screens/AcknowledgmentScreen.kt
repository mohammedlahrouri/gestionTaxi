package com.moham.taxi.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.moham.taxi.ui.navigation.AppScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcknowledgmentScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agradecimiento") },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "¡Gracias por confiar en esta app!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
            )
            
            Text(
                text = "Esta aplicación nace de la pasión y el compromiso de un taxista que también es desarrollador. Aquí no hay grandes empresas ni intereses ocultos: solo el deseo de aportar algo útil y honesto a nuestra comunidad.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Tu confianza y apoyo hacen posible que sigamos creciendo juntos, mejorando el día a día del taxi desde dentro, con independencia y profesionalidad. Esta app es tuya tanto como mía: una herramienta hecha por y para taxistas.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "El taxi es mucho más que un trabajo: es una forma de vida, una comunidad y una familia. Sigamos avanzando, apoyándonos y defendiendo lo nuestro con las mejores herramientas.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "Gracias por ser parte de esta comunidad.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "¡Seguimos en la carretera!",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
} 
