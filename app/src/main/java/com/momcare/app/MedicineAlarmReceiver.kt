package com.momcare.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class MedicineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra(EXTRA_NAME)
            ?: "your scheduled medicine"

        when (intent.action) {
            ACTION_TAKEN -> {
                cancelRepeat(context, name)
                notificationManager(context).cancel(name.hashCode())
                return
            }

            ACTION_NOT_YET -> {
                notificationManager(context).cancel(name.hashCode())
                scheduleRepeat(context, name)
                return
            }
        }

        showReminder(context, name)

        if (intent.action != ACTION_REPEAT) {
            val hour = intent.getIntExtra(EXTRA_HOUR, -1)
            val minute = intent.getIntExtra(EXTRA_MINUTE, -1)

            if (hour >= 0 && minute >= 0) {
                scheduleNextDay(context, name, hour, minute)
            }
        }

        scheduleRepeat(context, name)
    }

    private fun showReminder(context: Context, name: String) {
        val manager = notificationManager(context)
        val channelId = "momcare_medicine_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medicine reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "MomCare medicine alarms"
                lockscreenVisibility =
                    android.app.Notification.VISIBILITY_PUBLIC
                setSound(
                    RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_ALARM
                    ),
                    android.media.AudioAttributes.Builder()
                        .setUsage(
                            android.media.AudioAttributes.USAGE_ALARM
                        )
                        .build()
                )
            }
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAME, name)
            putExtra("show_reminder", true)
        }

        val openPending = PendingIntent.getActivity(
            context,
            name.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val takenPending = actionPendingIntent(
            context, name, ACTION_TAKEN, 100
        )
        val notYetPending = actionPendingIntent(
            context, name, ACTION_NOT_YET, 200
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("MomCare — Medicine time")
            .setContentText("Please check your medicine: $name")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )
            .setContentIntent(openPending)
            .addAction(0, "YES, TAKEN", takenPending)
            .addAction(0, "NOT YET", notYetPending)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        manager.notify(name.hashCode(), notification)

        MomCareSpeech.speak(
            context.applicationContext,
            "Mom, it is time for $name. Please check your medicine reminder."
        )
    }

    private fun actionPendingIntent(
        context: Context,
        name: String,
        action: String,
        code: Int
    ): PendingIntent {
        val actionIntent = Intent(context, MedicineAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_NAME, name)
        }

        return PendingIntent.getBroadcast(
            context,
            name.hashCode() xor code,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val EXTRA_NAME = "medicine_name"
        private const val EXTRA_HOUR = "medicine_hour"
        private const val EXTRA_MINUTE = "medicine_minute"
        private const val ACTION_TAKEN = "com.momcare.app.MEDICINE_TAKEN"
        private const val ACTION_NOT_YET = "com.momcare.app.MEDICINE_NOT_YET"
        private const val ACTION_REPEAT = "com.momcare.app.MEDICINE_REPEAT"
        private const val REPEAT_CODE = 0x4D43

        fun schedule(
            context: Context,
            medicineName: String,
            hour: Int,
            minute: Int
        ) {
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                putExtra(EXTRA_NAME, medicineName)
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
            }

            val pending = PendingIntent.getBroadcast(
                context,
                medicineName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAt(context, pending, nextTime(hour, minute))
        }

        private fun scheduleNextDay(
            context: Context,
            name: String,
            hour: Int,
            minute: Int
        ) {
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_HOUR, hour)
                putExtra(EXTRA_MINUTE, minute)
            }

            val pending = PendingIntent.getBroadcast(
                context,
                name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAt(context, pending, nextTime(hour, minute))
        }

        private fun nextTime(hour: Int, minute: Int): Long {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }.timeInMillis
        }

        private fun scheduleRepeat(context: Context, name: String) {
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = ACTION_REPEAT
                putExtra(EXTRA_NAME, name)
            }

            val pending = PendingIntent.getBroadcast(
                context,
                name.hashCode() xor REPEAT_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAt(
                context,
                pending,
                System.currentTimeMillis() + 5 * 60 * 1000L
            )
        }

        private fun cancelRepeat(context: Context, name: String) {
            val intent = Intent(context, MedicineAlarmReceiver::class.java).apply {
                action = ACTION_REPEAT
                putExtra(EXTRA_NAME, name)
            }

            val pending = PendingIntent.getBroadcast(
                context,
                name.hashCode() xor REPEAT_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            val alarms = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

            alarms.cancel(pending)
            pending.cancel()
        }

        private fun scheduleAt(
            context: Context,
            pending: PendingIntent,
            time: Long
        ) {
            val alarms = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarms.canScheduleExactAlarms()
            ) {
                return
            }

            alarms.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time,
                pending
            )
        }

        private fun notificationManager(
            context: Context
        ): NotificationManager {
            return context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
        }
    }
}
