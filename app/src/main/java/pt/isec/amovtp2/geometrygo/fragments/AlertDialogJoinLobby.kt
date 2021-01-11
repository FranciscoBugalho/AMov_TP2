package pt.isec.amovtp2.geometrygo.fragments

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.constants.DataConstants
import pt.isec.amovtp2.geometrygo.data.GameController

class AlertDialogJoinLobby(
    private val game: GameController,
    private var latitude: Double?,
    private var longitude: Double?
) : DialogFragment() {
    // EditText where the user will insert the server ip.
    private lateinit var editText: EditText

    // Button connect.
    private lateinit var btnConnect: Button

    // Button cancel.
    private lateinit var btnCancel: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alert_dialog_join_lobby, container, false)

        editText = view.findViewById(R.id.etDialogIp)

        btnConnect = view.findViewById(R.id.btnConnect)
        btnConnect.setOnClickListener {
            if (editText.text.isEmpty()) {
                editText.error = getString(R.string.ad_jl_et_empty_error)
                return@setOnClickListener
            } else {
                if (latitude != null && longitude != null) {
                    game.startAsClient(
                        editText.text.toString(),
                        DataConstants.SERVER_DEFAULT_PORT,
                        latitude!!,
                        longitude!!
                    )
                    dialog?.dismiss()
                } else
                    editText.error = getString(R.string.ad_cl_jl_et_no_latitude_longitude_error)
                return@setOnClickListener
            }
        }

        // If the button cancel is clicked goes to the main menu
        btnCancel = view.findViewById(R.id.btnCancel)
        btnCancel.setOnClickListener {
            dialog?.dismiss()
            activity?.finish()
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

        dialog!!.setCancelable(false)
    }

    fun presentDialog(
        supportFragmentManager: FragmentManager,
    ) {
        this.show(supportFragmentManager, FragmentConstants.ALERT_DIALOG)
    }

    fun setLatitude(latitude: Double) {
        this.latitude = latitude
    }

    fun setLongitude(longitude: Double) {
        this.longitude = longitude
    }
}