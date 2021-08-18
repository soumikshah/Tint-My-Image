package com.soumikshah.tintmyimage

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.drawToBitmap
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*


class MainActivity : AppCompatActivity() {
    private var pickColor: Button? = null
    private var pickImage: Button? = null
    private var transparencySeekBar: Slider? = null
    private var imageView: ImageView? = null
    private var parentFrame: FrameLayout? = null
    private var mProfileUri:Uri? = null
    private var outFile:File? = null
    private val FILE_PROVIDER = "com.soumikshah.tintmyimage.provider"

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

        transparencySeekBar!!.addOnChangeListener(Slider.OnChangeListener { slider, _, _ ->
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
                Log.d("Tint",fileUri.toString())
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
            .setPositiveButton(getString(android.R.string.ok)) { d, lastSelectedColor, _ ->
                pickColor!!.setBackgroundColor(lastSelectedColor)
                pickImage!!.setBackgroundColor(lastSelectedColor)
                imageView!!.setColorFilter(lastSelectedColor)
                transparencySeekBar!!.thumbTintList = ColorStateList.valueOf(lastSelectedColor)
                transparencySeekBar!!.trackActiveTintList = ColorStateList.valueOf(lastSelectedColor)
                val builder: AlertDialog.Builder = AlertDialog.Builder(ContextThemeWrapper
                    (this, R.style.AlertDialogTheme))
                builder.setTitle("Press save button on the top!!!")
                builder.setMessage("If you want to save this tinted image press save button and " +
                        "you'll need to accept permission to save the image to your phone.")
                builder.setPositiveButton("Ok") { dialogInterface, _ ->
                    dialogInterface.cancel()
                    dialogInterface.dismiss()
                }
                builder.setNegativeButton("Not Interested") { dialogInterface, _ ->
                    dialogInterface.cancel()
                    dialogInterface.dismiss()
                }
                builder.show()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu,menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_item_share -> {
                shareImage()
                return true
            }
/*            R.id.action_save -> {
                return true
            }*/
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

   private fun shareImage(){
       val bitmap = BitmapDrawable(resources,imageView!!.drawToBitmap())
       val fileDir = getExternalFilesDirs(null).first()
       val dir = File( fileDir,"TintedImage")
       dir.mkdirs()
       val fileName = String.format("%d.jpg", System.currentTimeMillis())
       outFile = File(dir, fileName)
       Log.d("Tint",outFile!!.absolutePath.toString())
       if(outFile!!.exists()){
           outFile!!.delete()
       }
       isStoragePermissionGranted()
       try{
           val outStream = FileOutputStream(outFile)
           bitmap.toBitmap().compress(Bitmap.CompressFormat.JPEG,100,outStream)
           outStream.flush()
           outStream.close()
       }catch (ex:Exception){
           ex.printStackTrace()
       }
       val fos: OutputStream? =if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
           val resolver = contentResolver
           val contentValues = ContentValues()
           contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, outFile!!.absolutePath.toString() + ".jpg")
           contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
           contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
           val imageUri =
               resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            Toast.makeText(this@MainActivity,"Image Saved!!",Toast.LENGTH_LONG).show()
            shareImageLogic(imageUri!!)
           resolver.openOutputStream(Objects.requireNonNull(imageUri)!!)
       } else {
           shareImageLogic(Uri.fromFile(outFile))
           FileOutputStream(outFile)

       }
        bitmap.toBitmap().compress(Bitmap.CompressFormat.JPEG, 100, fos)
        Objects.requireNonNull(fos)!!.close()
   }

    private fun shareImageLogic(imageUri:Uri){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q){
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TITLE,"Tinted Image")
                putExtra(Intent.EXTRA_STREAM,imageUri)
                clipData = ClipData.newUri(contentResolver,
                    getString(R.string.app_name),
                    FileProvider.getUriForFile(this@MainActivity, FILE_PROVIDER, File(outFile!!.absolutePath.toString())))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(intent,null))
        }else{
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TITLE,"Tinted Image")
                putExtra(Intent.EXTRA_STREAM, imageUri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(intent, "null"))
        }
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.v("Tint", "Permission is granted")
                true
            } else {
                Log.v("Tint", "Permission is revoked")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("Tint", "Permission is granted")
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("Tint", "Permission: " + permissions[0] + "was " + grantResults[0])
        }
    }
}