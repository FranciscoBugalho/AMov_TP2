package pt.isec.amovtp2.geometrygo.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import pt.isec.amovtp2.geometrygo.R

class AlertDialogNoInternetConnection : DialogFragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Defines the background drawable to the alert dialog
        dialog!!.window?.setBackgroundDrawableResource(R.drawable.alert_dialog_no_internect_connection_background)
        return inflater.inflate(R.layout.alert_dialog_no_internet_connection, container, false)
    }

    override fun onStart() {
        super.onStart()

        val window: Window? = dialog!!.window
        val wlp: WindowManager.LayoutParams = window!!.attributes

        wlp.gravity = Gravity.TOP
        wlp.flags = (wlp.flags
                // Flag that allow any events outside of the window to be sent to the windows behind it.
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                // Flag which makes this window won't ever get key input focus
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                // Everything behind this window won't be dimmed (.inv())
                and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                )
        window.attributes = wlp

        // Set the dialog layout parameters
        dialog!!.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun presentDialog(
            alertDialogNoInternetConnection: AlertDialogNoInternetConnection,
            isConnected: Boolean,
            supportFragmentManager: FragmentManager
    ) {
        // Verifies if exists any network connection
        if (!isConnected) {
            // If there isn't internet connection shows a dialog with a message to the user
            this.show(supportFragmentManager, FragmentConstants.ALERT_DIALOG)
        } else {
            if (alertDialogNoInternetConnection.isVisible)
                alertDialogNoInternetConnection.dismiss()
        }
    }
}