package com.momcare.app

import android.app.TimePickerDialog
import org.json.JSONArray
import org.json.JSONObject
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

private val RoyalBlue = Color(0xFF2457D6)
private val PageWhite = Color(0xFFF7F9FF)

private data class MedicineEntry(
    val name: String,
    val hour: Int,
    val minute: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = RoyalBlue,
                    background = PageWhite,
                    surface = Color.White
                )
            ) {
                MomCareScreen(
                    onChooseTime = { hour, minute, onSelected ->
                        TimePickerDialog(
                            this,
                            { _, selectedHour, selectedMinute ->
                                onSelected(selectedHour, selectedMinute)
                            },
                            hour,
                            minute,
                            false
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun MomCareScreen(
    onChooseTime: (
        Int,
        Int,
        (Int, Int) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current

    val medicines = remember {
        mutableStateListOf<MedicineEntry>().apply {
            val prefs = context.getSharedPreferences("momcare_schedule", 0)
            val saved = prefs.getString("medicines", "[]") ?: "[]"

            runCatching {
                val array = JSONArray(saved)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        MedicineEntry(
                            name = item.getString("name"),
                            hour = item.getInt("hour"),
                            minute = item.getInt("minute")
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(medicines.toList()) {
        val array = JSONArray()
        medicines.forEach { medicine ->
            array.put(
                JSONObject()
                    .put("name", medicine.name)
                    .put("hour", medicine.hour)
                    .put("minute", medicine.minute)
            )
        }

        context.getSharedPreferences("momcare_schedule", 0)
            .edit()
            .putString("medicines", array.toString())
            .apply()
    }

    var medicineName by remember { mutableStateOf("") }

    val now = Calendar.getInstance()
    var selectedHour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var showError by remember { mutableStateOf(false) }

    val formattedTime = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        selectedHour,
        selectedMinute
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PageWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "MomCare",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = RoyalBlue
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Your daily care, made simpler.",
                fontSize = 16.sp,
                color = Color(0xFF5E6678)
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = "Medicine schedule",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF18233A)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Add the medicine and time provided by your healthcare professional.",
                        color = Color(0xFF657087),
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = medicineName,
                        onValueChange = {
                            medicineName = it
                            showError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Medicine name") },
                        placeholder = { Text("Enter medicine name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            onChooseTime(selectedHour, selectedMinute) { h, m ->
                                selectedHour = h
                                selectedMinute = m
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Reminder time: $formattedTime",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showError) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Please enter a medicine name first.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val cleanName = medicineName.trim()
                            if (cleanName.isEmpty()) {
                                showError = true
                            } else {
                                val entry = MedicineEntry(
                                    name = cleanName,
                                    hour = selectedHour,
                                    minute = selectedMinute
                                )
                                medicines.add(entry)
                                MedicineAlarmReceiver.schedule(
                                    context = context,
                                    medicineName = entry.name,
                                    hour = entry.hour,
                                    minute = entry.minute
                                )
                                medicineName = ""
                                showError = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add medicine", fontSize = 16.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your medicines",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF18233A),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${medicines.size} added",
                    color = RoyalBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            if (medicines.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No medicines added yet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Your schedule will appear here.",
                            color = Color(0xFF657087)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(medicines) { index, medicine ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(28.dp)
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = medicine.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "%02d:%02d",
                                            medicine.hour,
                                            medicine.minute
                                        ),
                                        color = RoyalBlue,
                                        fontSize = 15.sp
                                    )
                                }

                                IconButton(
                                    onClick = { medicines.removeAt(index) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove medicine",
                                        tint = Color(0xFFB42318)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
