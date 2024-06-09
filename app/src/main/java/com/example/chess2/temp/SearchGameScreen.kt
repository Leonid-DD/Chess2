package com.example.chess2.temp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chess2.R
import com.example.chess2.auth.google.UserData
import com.example.chess2.user.MatchingState

//@Composable
//fun SearchGame(
//    state: MatchingState,
//    searchClick: () -> Unit,
//    signOutClick: () -> Unit
//) {
//    var isPressed by remember { mutableStateOf(false) }
//
//    val context = LocalContext.current
//    LaunchedEffect(key1 = state.matchingError) {
//        state.matchingError?.let { error ->
//            Toast.makeText(
//                context,
//                error,
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Button(
//            onClick = {
//                isPressed = !isPressed
//                searchClick()
//            }
//        ) {
//            if (isPressed) {
//                Text("Stop searching")
//            } else {
//                Text("Start searching")
//            }
//        }
//        Spacer(modifier = Modifier.height(40.dp))
//        Button(
//            onClick = {
//                signOutClick()
//            }
//        ) {
//            Text(text = "Sign out")
//        }
//    }
//}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    userData: UserData?,
    state: MatchingState,
    searchClick: () -> Unit,
    signOutClick: () -> Unit,
    changeGameModeClick: (String) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }
    val searchModes = listOf("Классика", "Рандом", "Шахматы 2.0")
    var selectedMode by remember { mutableStateOf(searchModes[0]) }

    var isPressed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(key1 = state.matchingError) {
        state.matchingError?.let { error ->
            Toast.makeText(
                context,
                error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD9E8D9))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = signOutClick
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (userData?.profilePictureUrl != null) {
                        AsyncImage(
                            model = userData.profilePictureUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "Выйти из аккаунта",
                        modifier = Modifier
                            .padding(start = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4D774E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Добрый день, ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4D774E)
            )
            Text(
                text = "${userData?.username}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4D774E)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .background(Color.Transparent),
                readOnly = true,
                value = selectedMode,
                textStyle = TextStyle.Default.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                onValueChange = { changeGameModeClick(selectedMode) },
                label = {
                    Text(
                        "Выберите игровой режим",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )},
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                searchModes.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(
                            text = selectionOption
                        ) },
                        onClick = {
                            selectedMode = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(160.dp))

        OutlinedButton(
            onClick = {
                isPressed = !isPressed
                searchClick()
            }
        ) {
            Text(
                text = if (isPressed) {
                    "Прекратить поиск"
                } else {
                    "Начать поиск"
                },
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4D774E)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSearchScreen() {
    SearchScreen(null, MatchingState(), {}, {}, {})
}