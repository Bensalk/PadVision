package com.bensal.tvlauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val name = i.getStringExtra("name") ?: "Channel"
        val url = i.getStringExtra("url") ?: return
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("alarm", "Channel alarms", NotificationManager.IMPORTANCE_HIGH))
        val open = Intent(c, MainActivity::class.java).putExtra("play_url", url)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(c, name.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(name.hashCode(), Notification.Builder(c, "alarm")
            .setContentTitle("Time to watch")
            .setContentText(name)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build())
    }
}
