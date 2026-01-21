package com.digitar.mintx.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import java.util.regex.Pattern

class SmsBroadcastReceiver : BroadcastReceiver() {

    private var otpListener: OtpReceiveListener? = null

    fun setOtpListener(listener: OtpReceiveListener) {
        this.otpListener = listener
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val extras = intent.extras
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    // Get SMS message contents
                    val message = extras.get(SmsRetriever.EXTRA_SMS_MESSAGE) as? String
                    message?.let {
                        val otp = extractOtp(it)
                        otp?.let { code ->
                            otpListener?.onOtpReceived(code)
                        }
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    otpListener?.onOtpTimeout()
                }
            }
        }
    }

    private fun extractOtp(message: String): String? {
        // Zomato style: "123456 is your verification code"
        // Try to find a 6 digit number
        val pattern = Pattern.compile("(|^)\\d{6}")
        val matcher = pattern.matcher(message)
        return if (matcher.find()) {
            matcher.group(0)
        } else {
            null
        }
    }

    interface OtpReceiveListener {
        fun onOtpReceived(otp: String)
        fun onOtpTimeout()
    }
}
