package pt.isec.amovtp2.geometrygo.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.widget.Toast
import pt.isec.amovtp2.geometrygo.R

class SMSFunctionality : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        for (currentMessage in messages) {
            val origin = currentMessage!!.displayOriginatingAddress
            val msg = currentMessage.displayMessageBody

            if (msg.contains(context.getString(R.string.sms_id_token))) {
                Toast.makeText(
                    context,
                    context.getString(R.string.sms_received_text),
                    Toast.LENGTH_LONG
                ).show()

                abortBroadcast()
                val resp: SmsManager = SmsManager.getDefault()
                resp.sendTextMessage(
                    origin,
                    null,
                    context.getString(R.string.sms_response_thanks_for_the_ip),
                    null,
                    null
                )
            }
        }
    }
}