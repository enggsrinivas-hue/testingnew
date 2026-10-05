package com.smsbuddy.app

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.mail.AuthenticationFailedException

/** Sends one SMS as one email. WorkManager retries automatically if there is no internet. */
class EmailWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val sender = inputData.getString("sender") ?: "Unknown"
        val text = inputData.getString("body") ?: ""
        val time = inputData.getLong("time", System.currentTimeMillis())
        val whenText = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss", Locale.getDefault()).format(Date(time))

        return try {
            Mailer.send(
                Prefs.gmail(applicationContext), Prefs.password(applicationContext), Prefs.to(applicationContext),
                "📩 SMS from $sender",
                "From: $sender\nReceived: $whenText\n\n$text"
            )
            Prefs.increment(applicationContext)
            Result.success()
        } catch (e: AuthenticationFailedException) {
            Result.failure() // wrong app password, retrying won't help
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}
