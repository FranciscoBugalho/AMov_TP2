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
    // EditText where the user will insert the phone number.
    private lateinit var editTextDestinationNumber: EditText

    // Button ad_cc_btn_send message.
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
            if (!isGlobalPhoneNumber(editTextDestinationNumber.text.toString()) ||
                editTextDestinationNumber.text.isEmpty()
            ) {
                editTextDestinationNumber.error =
                    getString(R.string.ad_cc_insert_a_valid_phone_number_error)
                return@setOnClickListener
            } else {
                val resp: SmsManager = SmsManager.getDefault()
                resp.sendTextMessage(
                    editTextDestinationNumber.text.toString(),
                    null,
                    getString(R.string.sms_id_token) + ipAddress,
                    null,
                    null
                )
                dialog?.dismiss()
            }
        }

        // If the button cancel is clicked cancel the dialog.
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