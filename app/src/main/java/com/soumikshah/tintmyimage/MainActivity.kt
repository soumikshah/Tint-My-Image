package com.soumikshah.tintmyimage

import android.app.Activity
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {
    private var pickColor: Button? = null
    private var pickImage: Button? = null
    private var transparencySeekBar: Slider? = null
    private var imageView: ImageView? = null
    private var parentFrame: FrameLayout? = null

    private var selectedColor:Int = 0
    private var mProfileUri:Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pickImage = findViewById(R.id.pick_image)
        transparencySeekBar = findViewById(R.id.transparency_seekbar)
        imageView = findViewById(R.id.imageView)
        pickColor = findViewById(R.id.pick_color)
        parentFrame = findViewById(R.id.parentFrame)

        transparencySeekBar!!.valueTo = 255F
        transparencySeekBar!!.value = 150F
        transparencySeekBar!!.valueFrom = 0F
        imageView!!.imageAlpha = transparencySeekBar!!.value.toInt()
        parentFrame!!.background.alpha = transparencySeekBar!!.value.toInt()

        pickImage!!.setOnClickListener {
            ImagePicker.with(this)
                .compress(1024)
                .maxResultSize(1080,1080)
                .galleryOnly()
                .saveDir(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!)
                .galleryMimeTypes(  //Exclude gif images
                    mimeTypes = arrayOf(
                        "image/png",
                        "image/jpg",
                        "image/jpeg"
                    )
                )
                .createIntent { intent -> startForBackgroundImageResult.launch(intent) }
        }

        transparencySeekBar!!.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
            imageView!!.imageAlpha = slider.value.toInt()
        })

        pickColor!!.setOnClickListener {
            colorPickerDialog()
        }
    }

    private val startForBackgroundImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            if (resultCode == Activity.RESULT_OK) {
                //Image Uri will not be null for RESULT_OK
                val fileUri = data?.data!!

                mProfileUri = fileUri
                imageView!!.setImageURI(fileUri)
            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
            }
        }


    private fun colorPickerDialog(){
        ColorPickerDialogBuilder.with(this)
            .setTitle(getString(R.string.choose_color))
            .initialColor(R.color.purple_500)
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .density(12)
            .setOnColorChangedListener { }
            .setPositiveButton(getString(R.string.ok)) { d, lastSelectedColor, _ ->
                pickColor!!.setBackgroundColor(lastSelectedColor)
                pickImage!!.setBackgroundColor(lastSelectedColor)
                imageView!!.setColorFilter(lastSelectedColor)
                Toast.makeText(this,"Color has been applied!",Toast.LENGTH_SHORT).show()
                d.cancel()
                d.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialogInterface, _ ->
                dialogInterface.cancel()
                dialogInterface.dismiss()
            }
            .build()
            .show()
    }
}