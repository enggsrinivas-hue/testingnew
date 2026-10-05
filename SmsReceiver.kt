package com.smsbuddy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs.isEnabled(context) || !Prefs.isConfigured(context)) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Long texts arrive in pieces: glue them back together per sender.
        for ((sender, pieces) in parts.groupBy { it.originatingAddress ?: "Unknown" }) {
            val body = pieces.joinToString("") { it.messageBody ?: "" }
            val work = OneTimeWorkRequestBuilder<EmailWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setInputData(workDataOf("sender" to sender, "body" to body, "time" to pieces.first().timestampMillis))
                .build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
