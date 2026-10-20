package com.momcare.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray

class MomCareBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val prefs = context.getSharedPreferences("momcare_schedule", 0)
        val saved = prefs.getString("medicines", "[]") ?: "[]"

        runCatching {
            val medicines = JSONArray(saved)
            for (i in 0 until medicines.length()) {
                val medicine = medicines.getJSONObject(i)
                MedicineAlarmReceiver.schedule(
                    context = context,
                    medicineName = medicine.getString("name"),
                    hour = medicine.getInt("hour"),
                    minute = medicine.getInt("minute")
                )
            }
        }
    }
}
