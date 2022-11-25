package com.adilkhann.mydrawingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class Drawing : AppCompatActivity() {
    private var flView : FrameLayout? = null
    private var drawingView : DrawingView? = null
    private var bgImage : ImageView? = null
    private var verySmallButton : ImageButton? = null
    private var smallButton : ImageButton? = null
    private var mediumButton : ImageButton? = null
    private var largeButton : ImageButton? = null
    private var ibGallery : ImageButton? = null
    private var ibUndo : ImageButton? = null
    private var ibPaint : ImageButton? = null
    private var bitmap : Bitmap? = null
    private var ibSave : ImageButton? = null
    private var isReadPermission = false
    private var isWritePermission = false
    var customProgressDialog : Dialog? = null
    private var resultGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            activityResult ->
            if(activityResult.resultCode == RESULT_OK && activityResult.data != null)
            {
                bgImage = findViewById(R.id.iv_background)
                bgImage?.setImageURI(activityResult.data?.data)
            }
        }
    private  var permissionLauncher : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
                permissions->
            permissions.entries.forEach()
            {
                val permission = it.key
                val isGranted = it.value
                if(permission == Manifest.permission.READ_EXTERNAL_STORAGE)
                    isReadPermission = isGranted
                if(permission == Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    isWritePermission = isGranted
            }

        }


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing)
        // check for that all permission granted or not
        requestPermission()
        drawingView = findViewById(R.id.drawing_view)
        // create objects all size selecting button
        verySmallButton = findViewById(R.id.ib_very_small)
        smallButton = findViewById(R.id.ib_small)
        mediumButton = findViewById(R.id.ib_medium)
        largeButton = findViewById(R.id.ib_large)
        // do operation one by one
        verySmallButton?.setOnClickListener{
            verySmallButton?.setImageResource(R.drawable.ic_selected_size)
            smallButton?.setImageResource(R.drawable.ic_size)
            mediumButton?.setImageResource(R.drawable.ic_size)
            largeButton?.setImageResource(R.drawable.ic_size)
            drawingView?.setSizeForBrush(10.toFloat())

        }
        smallButton?.setOnClickListener{
            smallButton?.setImageResource(R.drawable.ic_selected_size)
            verySmallButton?.setImageResource(R.drawable.ic_size)
            mediumButton?.setImageResource(R.drawable.ic_size)
            largeButton?.setImageResource(R.drawable.ic_size)
            drawingView?.setSizeForBrush(15.toFloat())

        }
        mediumButton?.setOnClickListener{
            mediumButton?.setImageResource(R.drawable.ic_selected_size)
            verySmallButton?.setImageResource(R.drawable.ic_size)
            smallButton?.setImageResource(R.drawable.ic_size)
            largeButton?.setImageResource(R.drawable.ic_size)
            drawingView?.setSizeForBrush(20.toFloat())

        }
        largeButton?.setOnClickListener{
            largeButton?.setImageResource(R.drawable.ic_selected_size)
            verySmallButton?.setImageResource(R.drawable.ic_size)
            mediumButton?.setImageResource(R.drawable.ic_size)
            smallButton?.setImageResource(R.drawable.ic_size)
            drawingView?.setSizeForBrush(25.toFloat())

        }
        // now create object of undo button
        ibUndo = findViewById(R.id.ib_undo)
        // do operation
        ibUndo?.setOnClickListener {
            drawingView?.undoView()
        }
        // Now main working going to start
        // i Have to create ibPaint object and using this ib paint object i have select
        // color pallet
        // i am going use open source color pallet from gfg so i will copy it's dependencies
        // in my project
        ibPaint = findViewById(R.id.ib_brush)
        ibPaint?.setOnClickListener {
            showDialogFun()
        }
        // let's select the image from gallery
        ibGallery = findViewById(R.id.ib_gallery)
        ibGallery?.setOnClickListener {
          if(!isReadPermission && Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
          {
              showRationaleDialog("Required Permission", "We can't access your media storage be" +
                      "cause you have denied storage permission request")
          }
            else
          {
              val intent = Intent(Intent.ACTION_PICK,
                  MediaStore.Images.Media.EXTERNAL_CONTENT_URI
              )
              resultGalleryLauncher.launch(intent)
          }
        }
        // let's save image in my storage
        ibSave = findViewById(R.id.ib_save)
        ibSave?.setOnClickListener {
            if(isReadPermission)
            {
                showCustomDialog()
                lifecycleScope.launch {
                    flView = findViewById(R.id.fl_view)
                    saveBitmapToMedia(getBitmapFromView(flView!!))
                }
            }
        }
    }
    // when dialog will pick the color and paint
    @SuppressLint("ClickableViewAccessibility")
    private fun showDialogFun()
    {
        val customDialog = Dialog(this)
        customDialog.setContentView(R.layout.dialog_color_pallete)
        val paletteView = customDialog.findViewById<ImageView>(R.id.color_picker)
        paletteView.isDrawingCacheEnabled = true
        paletteView.buildDrawingCache(true)
        paletteView.setOnTouchListener { _, motionEvent ->
            if(motionEvent.action == MotionEvent.ACTION_DOWN || motionEvent.action == MotionEvent.ACTION_MOVE)
            {
                bitmap = paletteView.drawingCache
                val pixel = bitmap?.getPixel(motionEvent.x.toInt(), motionEvent.y.toInt())
                val r = Color.red(pixel!!)
                val g = Color.green(pixel!!)
                val b = Color.blue(pixel!!)
                val hex = "#"+ Integer.toHexString(pixel!!)
                val tvResult = customDialog.findViewById<TextView>(R.id.result_tv)
                val colorView = customDialog.findViewById<View>(R.id.color_view)
                colorView.setBackgroundColor(Color.rgb(r,g,b))
                tvResult.text = "RGB: $r,$g,$b\nHEX: $hex"
                drawingView?.setColorBrush(hex)


            }
            if(motionEvent.action == MotionEvent.ACTION_UP)
            {
                customDialog.dismiss()
            }
            true}

        customDialog.show()
    }
    //Request permission
    private fun requestPermission()
    {
        isReadPermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        isWritePermission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val permissionReq : MutableList<String> = ArrayList()
        if(!isReadPermission) permissionReq.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if(!isWritePermission) permissionReq.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if(permissionReq.isNotEmpty())
            permissionLauncher.launch(permissionReq.toTypedArray())

    }
    // show rationale dialog
    private fun showRationaleDialog(title:String,message: String)
    {
        val alertDialog : AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton("cancel")
        {
            dialog,_ ->
            dialog.dismiss()
        }
        alertDialog.create().show()
    }
    // Now convert my drawing view into bitmap
    private fun getBitmapFromView(view:View):Bitmap
    {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        bgImage?.draw(canvas)
        view.draw(canvas)
        return bitmap
    }
    private suspend fun saveBitmapToMedia(bitmap:Bitmap?)
    :String{
        var result = ""
        withContext(Dispatchers.IO)
        {
            try {
                val bytes = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.PNG,90,bytes)
                val file = File(externalCacheDir?.absoluteFile.toString()+File.separator+
                        "MyDrawingApp"+"${System.currentTimeMillis()/1000}"+".png")
                val fileOutput = FileOutputStream(file)
                fileOutput.write(bytes.toByteArray())
                fileOutput.close()
                result = file.absolutePath
                runOnUiThread{
                    cancelCustomProgressDialog()
                    if(result.isNotEmpty())
                    {
                        Toast.makeText(this@Drawing,"file saved successfully $result",Toast.LENGTH_LONG).show()

                       sharedImage(result)
                    }
                    else
                    {
                        Toast.makeText(this@Drawing,"Can't save file , Something went to wrong",Toast.LENGTH_LONG).show()
                    }
                }
            }
            catch (e:Exception)
            {
                result = ""
                e.printStackTrace()
            }
        }
        return result
    }
    private fun showCustomDialog()
    {
        customProgressDialog = Dialog(this@Drawing)
        customProgressDialog?.setContentView(R.layout.custom_progress_dialog)
        customProgressDialog?.show()
    }
    private fun cancelCustomProgressDialog()
    {
        customProgressDialog?.dismiss()
        customProgressDialog = null
    }
    private fun sharedImage(result: String)
    {
        MediaScannerConnection.scanFile(this, arrayOf(result),null)
        {
            _,uri->
            val sharedIntent = Intent()
            sharedIntent.action = Intent.ACTION_SEND
            sharedIntent.putExtra(Intent.EXTRA_STREAM,uri)
            sharedIntent.type = "image/png"
            startActivity(Intent.createChooser(sharedIntent, "share"))
        }
    }

}
