package pt.isec.amovtp2.geometrygo.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import pt.isec.amovtp2.geometrygo.R
import pt.isec.amovtp2.geometrygo.data.GameController
import pt.isec.amovtp2.geometrygo.data.constants.DataConstants

class AlertDialogCreateLobby(
    private val game: GameController,
    private var latitude: Double?,
    private var longitude: Double?,
    private val tvTeamName: TextView
) : DialogFragment() {
    // EditText where the user will insert the team name.
    private lateinit var editText: EditText

    // TextView to display the number of characters.
    private lateinit var textView: TextView

    // Button create.
    private lateinit var btnCreate: Button

    // Button cancel.
    private lateinit var btnCancel: Button

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alert_dialog_create_lobby, container, false)

        textView = view.findViewById(R.id.tvNCharacters)
        textView.text = "0/${DataConstants.TEAM_NAME_SIZE}"

        editText = view.findViewById(R.id.etDialogTeamName)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > DataConstants.TEAM_NAME_SIZE) {
                    textView.setTextColor(ContextCompat.getColor(view.context, R.color.red_warnings))
                } else {
                    textView.setTextColor(ContextCompat.getColor(view.context, R.color.black))
                }
                textView.text = "$count/${DataConstants.TEAM_NAME_SIZE}"
            }

            override fun afterTextChanged(s: Editable?) { }

        })

        btnCreate = view.findViewById(R.id.btnCreate)
        btnCreate.setOnClickListener {
            if (editText.text.isEmpty()) {
                editText.error = getString(R.string.ad_cl_et_empty_error)
                return@setOnClickListener
            } else if (editText.text.toString().length > DataConstants.TEAM_NAME_SIZE) {
                editText.error = getString(R.string.ad_et_more_than_25_characters_error)
                return@setOnClickListener
            }
            else {
                if (latitude != null && longitude != null) {
                    game.createTeam(editText.text.toString())
                    tvTeamName.text = game.getTeamName()
                    game.startAsServer(latitude!!, longitude!!)
                    dialog?.dismiss()
                } else
                    editText.error = getString(R.string.ad_cl_jl_et_no_latitude_longitude_error)
                return@setOnClickListener
            }
        }

        // If the button cancel is clicked goes to the main menu.
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
        supportFragmentManager: FragmentManager
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