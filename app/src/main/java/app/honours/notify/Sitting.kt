package app.honours.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.honours.MainActivity
import app.honours.R
import app.honours.container
import app.honours.domain.day
import app.honours.localized
import app.honours.ru
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * The one notification there is: at the chosen evening hour, «the commission is sitting». It isn't a
 * reminder to do anything; it doesn't come on a day that already has an award.
 */
object Sitting {
    private const val CHANNEL = "sitting"
    private const val WORK = "sitting"

    fun channel(c: Context) {
        val ch = NotificationChannel(CHANNEL, c.localized().let { if (it.ru) "Заседание комиссии" else "The commission sits" }, NotificationManager.IMPORTANCE_DEFAULT)
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** The next time the clock reaches [hour] after [now]. */
    fun next(hour: Int, now: Long, zone: ZoneId): Long {
        val today = LocalDate.ofEpochDay(now.day(zone))
        val t = today.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return if (t > now) t else today.plusDays(1).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    }

    /** Plans the next sitting; called on opening the app, on changing the hour, and after each sitting. */
    suspend fun plan(c: Context) {
        val app = c.applicationContext
        val st = app.container.settings.now()
        val wm = WorkManager.getInstance(app)
        if (!st.onboarded || st.hour < 0) { wm.cancelUniqueWork(WORK); return }
        val now = System.currentTimeMillis()
        val req = OneTimeWorkRequestBuilder<SittingWorker>().setInitialDelay(next(st.hour, now, ZoneId.systemDefault()) - now, TimeUnit.MILLISECONDS).build()
        wm.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
    }

    fun post(c: Context) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val ru = c.localized().ru
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("commission", true), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_honours)
            .setContentTitle(if (ru) "Комиссия заседает" else "The commission is sitting")
            .setContentText(if (ru) "Что сегодня было? Мелочей не бывает." else "What happened today? Nothing is too small.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(c).notify(1, n)
    }
}

class SittingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val st = c.settings.now()
        val s = c.repo.refresh()
        if (st.onboarded && st.hour >= 0 && s.on(System.currentTimeMillis().day(ZoneId.systemDefault())).isEmpty()) Sitting.post(applicationContext)
        Sitting.plan(applicationContext)
        return Result.success()
    }
}
