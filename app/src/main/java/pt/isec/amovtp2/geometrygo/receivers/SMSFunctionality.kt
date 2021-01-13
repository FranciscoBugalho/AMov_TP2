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

        Toast.makeText(context, ": SMS received", Toast.LENGTH_LONG).show()

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        for (currentMessage in messages) {
            val origin = currentMessage!!.displayOriginatingAddress
            val msg = currentMessage.displayMessageBody

            if (msg.contains(context.getString(R.string.sms_id_token))) {
                abortBroadcast()
                val resp: SmsManager = SmsManager.getDefault()
                // todo perguntar se quer -se juntar num lobby
                // * saber em que actividade estamos, se for a main activty ou join looby então criar dialog box e fazer join
            resp.sendTextMessage(origin, null, "Obrigado pelo IP", null, null)
            }
        }
    }


/*
    override fun onReceive(context: Context, intent: Intent) {

        Toast.makeText(context, "$TAG: SMS received", Toast.LENGTH_LONG).show()

        val bundle = intent.extras
        if (bundle != null) {
            val pdusObj = bundle["pdus"] as Array<Any>?
            if (pdusObj != null) {
                val messages = arrayOfNulls<SmsMessage>(pdusObj.size)
                // API > 19 :  val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (i in pdusObj.indices) {
                    messages[i] = SmsMessage.createFromPdu(pdusObj[i] as ByteArray)
                    //API23:  ,intent.getStringExtra("format")); //3gpp ou 3gpp2
                }
                for (currentMessage in messages) {
                    val origin = currentMessage!!.displayOriginatingAddress
                    val msg = currentMessage!!.displayMessageBody
                    Log.i(TAG, "Origin: $origin")
                    Log.i(TAG, "Msg: $msg")
                    if (msg.contains("isec")) {
                        abortBroadcast()
                        Log.d(TAG, "Contains isec")
                        val resp: SmsManager = SmsManager.getDefault()
                        resp.sendTextMessage(origin, null, "thanks", null, null)
                        Log.d(TAG, "Message sent")
                    }
                }
            }
        }
    }


 */
}