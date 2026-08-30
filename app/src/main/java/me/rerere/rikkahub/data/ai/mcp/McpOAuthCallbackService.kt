/*
 * 橘瓣 OrangeChat
 * Keeps the MCP OAuth loopback listener alive while the browser is open.
 */
package me.rerere.rikkahub.data.ai.mcp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

internal class McpOAuthCallbackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "MCP 授权", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("正在等待 MCP 授权")
            .setContentText("授权完成后会自动结束")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "mcp_oauth_callback"
        private const val NOTIFICATION_ID = 24134

        fun start(context: android.content.Context) {
            ContextCompat.startForegroundService(
                context.applicationContext,
                Intent(context, McpOAuthCallbackService::class.java),
            )
        }

        fun stop(context: android.content.Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, McpOAuthCallbackService::class.java)
            )
        }
    }
}
