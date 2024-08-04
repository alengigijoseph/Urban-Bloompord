package com.example.weed

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.weed.databinding.FragmentHomeBinding
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weed.databinding.ActivityMainBinding
import com.example.weed.databinding.DialogRgbBinding
import com.google.firebase.database.*
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import kotlin.math.roundToInt


class HomeFragment : Fragment() {

    lateinit var binding: FragmentHomeBinding
    private lateinit var temperatureTextView: TextView
    private lateinit var temperatureCircularProgressBar: CircularProgressBar
    private lateinit var setTemperatureButton: Button
    private lateinit var powerToggleButton: Switch
    lateinit var ledtxt: TextView
    private lateinit var databaseReference: DatabaseReference
    private lateinit var databaseReferencePower: DatabaseReference
    lateinit var peltier_ref: DatabaseReference
    lateinit var pump_ref:DatabaseReference
    lateinit var led_ref:DatabaseReference
    private lateinit var databaseReferenceLed: DatabaseReference
    lateinit var humidity:DatabaseReference
    var r = 0
    var g = 0
    var b = 0
    var br = 0

    private val rgbLayoutDialogBinding : DialogRgbBinding by lazy {
        DialogRgbBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().reference.child("temperature")
        humidity = FirebaseDatabase.getInstance().reference.child("humidity")
        databaseReferencePower = FirebaseDatabase.getInstance().reference.child("power_on")
        peltier_ref = FirebaseDatabase.getInstance().reference.child("peltier_pow")
        pump_ref = FirebaseDatabase.getInstance().reference.child("pump_pow")
        led_ref = FirebaseDatabase.getInstance().reference.child("led_pow")
        databaseReferenceLed = FirebaseDatabase.getInstance().reference.child("ledParameters")

        // Initialize UI components
        temperatureTextView = binding.temperatureTextView
        temperatureCircularProgressBar = binding.temperatureProgressBar
        setTemperatureButton = binding.setTemperatureButton
        powerToggleButton = binding.powerToggleButton
        val pickColorBtn = binding.pickColorBtn

        ledtxt= binding.textView2


        // Fetch initial temperature value on app launch
        fetchPowerStateFromDatabase()
        fetchPeltier()
        fetchPump()
        fetchLed()
        fetchTemperatureFromDatabase()
        fetchHumidityFromDatabase()
        fetchLedFromDatabase()

        // Set listener for the SeekBar to update the temperature TextView
        temperatureCircularProgressBar.onProgressChangeListener= { progress ->
            val temperatureValue = progress.toInt()
            temperatureTextView.text = "$temperatureValue°C"
        }
        binding.humprogress.onProgressChangeListener= { progress ->
            val humValue = progress.toInt()
            binding.humTextView.text = "$humValue%"
        }

        // Set listener for the "Set Temperature" button to update the temperature in the database
        setTemperatureButton.setOnClickListener {
            showSeekBarDialog()
        }
        powerToggleButton.setOnCheckedChangeListener { _, isChecked ->
            val powerState = if (isChecked) "on" else "off"
            setPowerStateInDatabase(powerState)
        }
        binding.peltiertoggle.setOnCheckedChangeListener{_, isChecked ->
            val powerState = if (isChecked) "on" else "off"
            setPeltier(powerState)
        }
        binding.pumptoggle.setOnCheckedChangeListener{_, isChecked ->
            val powerState = if (isChecked) "on" else "off"
            setPump(powerState)
        }
        binding.ledtoggle.setOnCheckedChangeListener{_, isChecked ->
            val powerState = if (isChecked) "on" else "off"
            setLed(powerState)
        }


        // Monitor the database for changes
        monitorDatabaseForChanges()

        val rgbDialog = Dialog(requireContext()).apply {
            setContentView(rgbLayoutDialogBinding.root)
            window!!.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setCancelable(false)
        }

        setOnSeekbar(
            "R",
            rgbLayoutDialogBinding.redLayout.typeTxt,
            rgbLayoutDialogBinding.redLayout.seekBar,
            rgbLayoutDialogBinding.redLayout.colorValueTxt
        )
        setOnSeekbar(
            "G",
            rgbLayoutDialogBinding.greenLayout.typeTxt,
            rgbLayoutDialogBinding.greenLayout.seekBar,
            rgbLayoutDialogBinding.greenLayout.colorValueTxt
        )
        setOnSeekbar(
            "B",
            rgbLayoutDialogBinding.blueLayout.typeTxt,
            rgbLayoutDialogBinding.blueLayout.seekBar,
            rgbLayoutDialogBinding.blueLayout.colorValueTxt
        )
        setOnSeekbar(
            "BR",
            rgbLayoutDialogBinding.brightLayout.typeTxt,
            rgbLayoutDialogBinding.brightLayout.seekBar,
            rgbLayoutDialogBinding.brightLayout.colorValueTxt
        )
        rgbLayoutDialogBinding.cancelBtn.setOnClickListener {
            rgbDialog.dismiss()
        }
        rgbLayoutDialogBinding.pickBtn.setOnClickListener {
            rgbDialog.dismiss()
            setLedStateInDatabase(r,g,b,br)
        }


        pickColorBtn.setOnClickListener {
            rgbDialog.show()
        }
        binding.redButton.setOnClickListener{
            setLedStateInDatabase(255,0,0,255)
        }
        binding.blueButton.setOnClickListener{
            setLedStateInDatabase(0,0,255,255)
        }
        binding.purpleButton.setOnClickListener{
            setLedStateInDatabase(255,0,255,255)
        }
        binding.whiteButton.setOnClickListener{
            setLedStateInDatabase(255,255,255,255)
        }

        return binding.root
    }

    fun fetchPeltier(){
        peltier_ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val powerState2 = snapshot.getValue(String::class.java)
                powerState2?.let {
                    if(it =="on"){
                        binding.peltiertoggle.isChecked = true
                        binding.peltiertxt.text  = "Peltier: ON"
                    }
                    else{
                        binding.peltiertoggle.isChecked = false
                        binding.peltiertxt.text  = "Pelier: OFF"
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
    }
    fun fetchPump(){
        pump_ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val powerState2 = snapshot.getValue(String::class.java)
                powerState2?.let {
                    if(it =="on"){
                        binding.pumptoggle.isChecked = true
                        binding.pumptxt.text  = "Pump: ON"
                    }
                    else{
                        binding.pumptoggle.isChecked = false
                        binding.pumptxt.text  = "Pump: OFF"
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
    }
    fun fetchLed(){
        led_ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val powerState2 = snapshot.getValue(String::class.java)
                powerState2?.let {
                    if(it =="on"){
                        binding.ledtoggle.isChecked = true
                        binding.ledtxt.text  = "LED: ON"
                    }
                    else{
                        binding.ledtoggle.isChecked = false
                        binding.ledtxt.text  = "LED: OFF"
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
    }

    private fun fetchPowerStateFromDatabase() {
        // Fetch initial power state from the database
        databaseReferencePower.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val powerState = snapshot.getValue(String::class.java)
                powerState?.let {
                    if(it =="on"){
                        powerToggleButton.isChecked = true
                        binding.mainstxt.text  = "Mains: ON"
                    }
                    else{
                        powerToggleButton.isChecked = false
                        binding.mainstxt.text  = "Mains: OFF"
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })

    }
    private fun fetchLedFromDatabase() {
        // Fetch initial temperature value from the database
        databaseReferenceLed.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get the values from the dataSnapshot
                val red = dataSnapshot.child("red").value as Long
                val green = dataSnapshot.child("green").value as Long
                val blue = dataSnapshot.child("blue").value as Long
                val brightness = dataSnapshot.child("brightness").value as Long
                val hexColor = String.format("#%02X%02X%02X", red*brightness/255, green*brightness/255, blue*brightness/255)
                ledtxt.setBackgroundColor(Color.parseColor(hexColor))
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors here
            }
        })
    }
    private fun fetchHumidityFromDatabase() {
        // Fetch initial temperature value from the database
        humidity.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperatureValue = snapshot.getValue(Int::class.java)
                temperatureValue?.let {
                    binding.humTextView.text = "$it%"
                    binding.humprogress.apply {
                        setProgressWithAnimation(it.toFloat(),1000)
                        progressBarWidth = 5f
                        backgroundProgressBarWidth = 2f
                        progressBarColor = Color.YELLOW
                        startAngle=270f
                        progressDirection=CircularProgressBar.ProgressDirection.TO_RIGHT
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
    }

    private fun fetchTemperatureFromDatabase() {
        // Fetch initial temperature value from the database
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperatureValue = snapshot.getValue(Int::class.java)
                temperatureValue?.let {
                    temperatureTextView.text = "$it°C"
                    if(it.toFloat()>=0){
                        temperatureCircularProgressBar.apply {
                            setProgressWithAnimation(it.toFloat(),1000)
                            progressBarWidth = 5f
                            backgroundProgressBarWidth = 2f
                            progressBarColor = Color.RED
                            startAngle=0f
                            progressDirection=CircularProgressBar.ProgressDirection.TO_RIGHT
                        }
                    }
                    else{
                        temperatureCircularProgressBar.apply {
                            setProgressWithAnimation(it.toFloat(),1000)
                            progressBarWidth = 5f
                            backgroundProgressBarWidth = 2f
                            progressBarColor = Color.BLUE
                            startAngle=0f
                            progressDirection=CircularProgressBar.ProgressDirection.TO_LEFT
                        }
                    }
                    //temperatureCircularProgressBar.progress = it.toFloat()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
    }

    private fun setPowerStateInDatabase(powerState: String) {
        // Set the power state in the database
        databaseReferencePower.setValue(powerState)
    }
    fun setPeltier(powerState2: String) {

        peltier_ref.setValue(powerState2)
    }
    fun setPump(powerState2: String) {

        pump_ref.setValue(powerState2)
    }
    fun setLed(powerState2: String) {

        led_ref.setValue(powerState2)
    }

    private fun setLedStateInDatabase(r:Int,g:Int,b:Int,br:Int) {
        // Set the power state in the database
        databaseReferenceLed.child("red").setValue(r)
        databaseReferenceLed.child("blue").setValue(b)
        databaseReferenceLed.child("green").setValue(g)
        databaseReferenceLed.child("brightness").setValue(br)
    }
    fun setTemperatureInDatabase(temperatureValue: Int) {
        // Set the temperature value in the database
        databaseReference.setValue(temperatureValue)
    }

    private fun monitorDatabaseForChanges() {
        // Monitor the database for changes in the temperature value
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperatureValue = snapshot.getValue(Int::class.java)
                temperatureValue?.let {
                    // Update UI with the new temperature value
                    temperatureCircularProgressBar.progress = it.toFloat()
                    temperatureTextView.text = "$it°C"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
        humidity.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperatureValue = snapshot.getValue(Int::class.java)
                temperatureValue?.let {
                    binding.humTextView.text = "$it%"
                    binding.humprogress.progress= it.toFloat()

                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
        databaseReferencePower.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val powerState = snapshot.getValue(String::class.java)
                powerState?.let {
                    if(it =="on"){
                        powerToggleButton.isChecked = true
                        binding.mainstxt.text  = "Mains: ON"
                    }
                    else{
                        powerToggleButton.isChecked = false
                        binding.mainstxt.text  = "Mains: OFF"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
        led_ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val powerState2 = snapshot.getValue(String::class.java)
                powerState2?.let {
                    if(it =="on"){
                        binding.ledtoggle.isChecked = true
                        binding.ledtxt.text  = "LED: ON"
                    }
                    else{
                        binding.ledtoggle.isChecked = false
                        binding.ledtxt.text  = "LED: OFF"
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
        pump_ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val powerState2 = snapshot.getValue(String::class.java)
                powerState2?.let {
                    if(it =="on"){
                        binding.pumptoggle.isChecked = true
                        binding.pumptxt.text  = "Pump: ON"
                    }
                    else{
                        binding.pumptoggle.isChecked = false
                        binding.pumptxt.text  = "Pump: OFF"
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
        peltier_ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val powerState2 = snapshot.getValue(String::class.java)
                powerState2?.let {
                    if(it =="on"){
                        binding.peltiertoggle.isChecked = true
                        binding.peltiertxt.text  = "Peltier: ON"
                    }
                    else{
                        binding.peltiertoggle.isChecked = false
                        binding.peltiertxt.text  = "Peltier: OFF"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })

        databaseReferenceLed.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val red = snapshot.child("red").value as Long
                val green = snapshot.child("green").value as Long
                val blue = snapshot.child("blue").value as Long
                val brightness = snapshot.child("brightness").value as Long
                val hexColor = String.format("#%02X%02X%02X", red*brightness/255, green*brightness/255, blue*brightness/255)
                ledtxt.setBackgroundColor(Color.parseColor(hexColor))
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle an error
            }
        })
    }
    private fun showSeekBarDialog() {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_seekbar, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBar)
        val selectedValueTextView = dialogView.findViewById<TextView>(R.id.selectedValueTextView)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setTitle("Select Temperature")

        // Adjust the max value of the SeekBar to match the range
        seekBar.max = 200
        seekBar.progress = 100 // Set the initial progress to the middle of the range

        val alertDialog = alertDialogBuilder.create()

        // Update the TextView dynamically as the SeekBar progresses
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val selectedValue = progress - 100 // Adjust for the range
                selectedValueTextView.text = "Selected Value: $selectedValue°C"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        okButton.setOnClickListener {
            val selectedValue = seekBar.progress - 100
            setTemperatureInDatabase(selectedValue)
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun setRGBColor(){
        r = rgbLayoutDialogBinding.redLayout.seekBar.progress
        g = rgbLayoutDialogBinding.greenLayout.seekBar.progress
        b = rgbLayoutDialogBinding.blueLayout.seekBar.progress
        br = rgbLayoutDialogBinding.brightLayout.seekBar.progress
        val hex = String.format("#%02x%02x%02x",r*br/255,g*br/255,b*br/255)
        rgbLayoutDialogBinding.colorView.setBackgroundColor(Color.parseColor(hex))
    }
    private fun setOnSeekbar(type:String, typeTxt:TextView,seekBar: SeekBar,colorTxt:TextView) {
        typeTxt.text = type
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                colorTxt.text = seekBar.progress.toString()
                setRGBColor()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })
        colorTxt.text = seekBar.progress.toString()
    }


}
