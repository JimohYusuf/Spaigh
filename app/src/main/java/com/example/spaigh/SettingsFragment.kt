package com.example.spaigh

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.spaigh.network.DbConstants

class SettingsFragment : Fragment(), AdapterView.OnItemSelectedListener, View.OnClickListener {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val saveSettings = view.findViewById<Button>(R.id.save_settings)

        saveSettings.setOnClickListener(this)

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val spinner: Spinner = view.findViewById(R.id.sensitivity)

        // Create an ArrayAdapter using the string array and a default spinner layout//////////////
        activity?.let {
            ArrayAdapter.createFromResource(
                it,
                R.array.sensitivity,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                // Apply the adapter to the spinner
                spinner.adapter = adapter
            }
        }
        spinner.onItemSelectedListener = this


        val website_address = activity?.findViewById<TextView>(R.id.website_address)
        val stream_address = activity?.findViewById<TextView>(R.id.stream_address)

        val sharedPref: SharedPreferences? = activity?.applicationContext?.getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )

        if (sharedPref != null) {
            website_address?.text = sharedPref.getString(getString(R.string.website_address), "")
        }

        if (sharedPref != null) {
            stream_address?.text = sharedPref.getString(getString(R.string.stream_address), "")
        }

    }


    override fun onNothingSelected(parent: AdapterView<*>?) {
        moveThreshold = 0.01F
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        moveThreshold = when (position) {
            0 -> 0.005F
            1 -> 0.01F
            2 -> 0.1F
            else -> 0.01F
        }
        if (parent != null) {
            Toast.makeText(
                activity,
                "Sensitivity: ${parent.selectedItem.toString()}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    override fun onClick(v: View?) {

        val website_address = activity?.findViewById<TextView>(R.id.website_address)
        val stream_address = activity?.findViewById<TextView>(R.id.stream_address)

        if (website_address != null) {
            DbConstants.WEBSITE_ADDRESS = website_address.text.toString()
        }
        if (stream_address != null) {
            DbConstants.STREAM_ADDRESS = stream_address.text.toString()
        }

        val sharedPref: SharedPreferences? = activity?.applicationContext?.getSharedPreferences(
            getString(R.string.shared_pref),
            Context.MODE_PRIVATE
        )
        if (sharedPref != null) {
            with(sharedPref.edit()) {
                if (website_address != null) {
                    putString(getString(R.string.website_address), website_address.text.toString())
                }
                if (stream_address != null) {
                    putString(getString(R.string.stream_address), stream_address.text.toString())
                }
                commit()
            }
        }

        Toast.makeText(
            activity,
            "Settings saved",
            Toast.LENGTH_LONG
        ).show()

    }


}