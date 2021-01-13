package pt.isec.amovtp2.geometrygo.fragments

import android.os.Bundle
import android.telephony.PhoneNumberUtils.isGlobalPhoneNumber
import android.telephony.SmsManager
import android.view.*
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import pt.isec.amovtp2.geometrygo.R

class AlertDialogChooseContact(private val ipAddress: String) : DialogFragment() {

    //val SMS_WITH__SERVER_IP = getString(R.string.sms_id_token) + ipAddress;
    // EditText where the user will insert the server ip.
    private lateinit var editTextDestinationNumber: EditText

    // Button connect.
    private lateinit var btnSendSMS: Button

    // Button cancel.
    private lateinit var btnCancel: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alert_dialog_choose_contact, container, false)

        editTextDestinationNumber = view.findViewById(R.id.etSMSDestination)
        btnSendSMS = view.findViewById(R.id.btnSendSMS)
        btnSendSMS.setOnClickListener {
            if ( !isGlobalPhoneNumber(editTextDestinationNumber.text.toString()) ||
                editTextDestinationNumber.text.isEmpty() ) {
                editTextDestinationNumber.error = "insert a valid phone number"
                return@setOnClickListener
            } else {
                val resp: SmsManager = SmsManager.getDefault()
                //todo substituir origin pelo ip e um nome fixo definifo hardcoded 6172839405 GeoAppIp ip
                resp.sendTextMessage(editTextDestinationNumber.text.toString(), null,getString(R.string.sms_id_token) + ipAddress, null, null)
                dialog?.dismiss()
            }
        }

        // If the button cancel is clicked goes to the main menu
        btnCancel = view.findViewById(R.id.btnCancel)
        btnCancel.setOnClickListener {
            dialog?.dismiss()
        }
        return view
    }

    override fun onStart() {
        super.onStart()

        val window: Window? = dialog!!.window
        val wlp: WindowManager.LayoutParams = window!!.attributes

        wlp.gravity = Gravity.CENTER
        window.attributes = wlp

        // Set the dialog layout parameters.
        dialog!!.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog!!.setCancelable(true)
    }

    fun presentDialog(
        supportFragmentManager: FragmentManager,
    ) {
        this.show(supportFragmentManager, FragmentConstants.ALERT_DIALOG)
    }

}