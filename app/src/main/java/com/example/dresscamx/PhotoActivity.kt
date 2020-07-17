package  com.example.dresscamx


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import com.example.dresscamx.ImagePopupView.Companion.ALPHA_TRANSPARENT
import com.example.dresscamx.ImagePopupView.Companion.FADING_ANIMATION_DURATION
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.extensions.BokehImageCaptureExtender
import androidx.camera.extensions.HdrImageCaptureExtender
import androidx.camera.extensions.ImageCaptureExtender
import androidx.camera.extensions.NightImageCaptureExtender
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_photo.*
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PhotoActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val fileUtils: FileUtils by lazy { FileUtilsImpl() }
    private val executor: Executor by lazy { Executors.newSingleThreadExecutor() }

    private  val TAG :String="PHOTOACTIVITY"
    private var imageCapture: ImageCapture? = null
    private var imagePopupView: ImagePopupView? = null
    private var lensFacing = CameraX.LensFacing.BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)
        setClickListeners()
        requestPermissions()
    }

    private fun setClickListeners() {
        toggleCameraLens.setOnClickListener { toggleFrontBackCamera() }
        takeShoot.setOnClickListener { takePicture() }
        takenImage.setOnLongClickListener {
            showImagePopup()
            return@setOnLongClickListener true
        }

        extensionFeatures.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>,
                    selectedItemView: View,
                    position: Int,
                    id: Long
                ) {
                    if (ExtensionFeature.fromPosition(position) != ExtensionFeature.NONE) {
                        previewView.post { startCamera() }
                    }
                }

                override fun onNothingSelected(parentView: AdapterView<*>) {}
            }
    }

    private fun requestPermissions() {
        if (allPermissionsGranted()) {
            previewView.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun takePicture() {
        disableActions()
        if (saveImageSwitch.isChecked) {
            savePictureToFile()
        } else {
            savePictureToMemory()
        }
    }

    private fun savePictureToFile() {
        fileUtils.createDirectoryIfNotExist()
        val file = fileUtils.createFile()

        imageCapture?.takePicture(file, getMetadata(), executor,
            object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    runOnUiThread {
                        takenImage.setImageURI(FileProvider.getUriForFile(this@PhotoActivity,
                            packageName,
                            file))
                        enableActions()
                    }
                }

                override fun onError(imageCaptureError: ImageCapture.ImageCaptureError,
                                     message: String,
                                     cause: Throwable?) {
                    Toast.makeText(this@PhotoActivity,
                        getString(R.string.image_capture_failed),
                        Toast.LENGTH_SHORT).show()

                }
            })
    }

    private fun getMetadata() = ImageCapture.Metadata().apply {
        isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
    }

    private fun savePictureToMemory() {
        imageCapture?.takePicture(executor,
            object : ImageCapture.OnImageCapturedListener() {
                override fun onError(
                    error: ImageCapture.ImageCaptureError,
                    message: String, exc: Throwable?
                ) {
                    Toast.makeText(this@PhotoActivity, getString(R.string.image_save_failed),
                        Toast.LENGTH_SHORT).show()
                }

                override fun onCaptureSuccess(imageProxy: ImageProxy?,
                                              rotationDegrees: Int) {
                    imageProxy?.image?.let {
                        val bitmap = rotateImage(
                            imageToBitmap(it),
                            rotationDegrees.toFloat()
                        )
                        runOnUiThread {
                            takenImage.setImageBitmap(bitmap)
                            enableActions()
                        }
                    }
                    super.onCaptureSuccess(imageProxy, rotationDegrees)
                }
            })
    }

    private fun createImagePopup(imageDrawable: Drawable,
                                 backgroundClickAction: () -> Unit) =
        ImagePopupView.builder(this)
            .imageDrawable(imageDrawable)
            .onBackgroundClickAction(backgroundClickAction)
            .build()


    private fun removeImagePopup() {
        imagePopupView?.let {
            it.animate()
                .alpha(ALPHA_TRANSPARENT)
                .setDuration(FADING_ANIMATION_DURATION)
                .withEndAction {
                    rootView.removeView(it)
                }
                .start()
        }
    }

    private fun showImagePopup() {
        if (takenImage.drawable == null) {
            return
        }
        createImagePopup(takenImage.drawable) { removeImagePopup() }
            .let {
                imagePopupView = it
                addImagePopupViewToRoot(it)
            }
    }

    private fun addImagePopupViewToRoot(imagePopupView: ImagePopupView) {
        rootView.addView(imagePopupView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    //Set whether to use front back camera
    private fun toggleFrontBackCamera() {
        lensFacing = if (lensFacing == CameraX.LensFacing.BACK) {
            CameraX.LensFacing.FRONT
        } else {
            CameraX.LensFacing.BACK
        }
        previewView.post { startCamera() }
    }

    private fun startCamera() {
        CameraX.unbindAll()

        val preview = createPreviewUseCase()

        preview.setOnPreviewOutputUpdateListener {

            val parent = previewView.parent as ViewGroup
            parent.removeView(previewView)
            parent.addView(previewView, 0)

            previewView.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        imageCapture = createCaptureUseCase()
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    // lets bind the camera  to preview setting the camera lensefacing and rotation
    private fun createPreviewUseCase(): Preview {
        val previewConfig = PreviewConfig.Builder()
            .apply {
            setLensFacing(lensFacing)
            setTargetRotation(previewView.display.rotation)

        }.build()

        return Preview(previewConfig)
    }

    private fun createCaptureUseCase(): ImageCapture {
        val imageCaptureConfig = ImageCaptureConfig.Builder()
                //when capturing turn on flash
            .setFlashMode(FlashMode.ON)
                //set LensFacing and rotation
            .apply {
                //take the selected Lens Facing To snap with
                setLensFacing(lensFacing)
                //Bind preview to the UI    element to display the preview
                setTargetRotation(previewView.display.rotation)
                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            }

        applyExtensions(imageCaptureConfig)
        return ImageCapture(imageCaptureConfig.build())
    }

    private fun applyExtensions(builder: ImageCaptureConfig.Builder) {
        when (ExtensionFeature.fromPosition(extensionFeatures.selectedItemPosition)) {
            ExtensionFeature.BOKEH ->
                enableExtensionFeature(BokehImageCaptureExtender.create(builder))
            ExtensionFeature.HDR ->
                enableExtensionFeature(HdrImageCaptureExtender.create(builder))
            ExtensionFeature.NIGHT_MODE ->
                enableExtensionFeature(NightImageCaptureExtender.create(builder))
            else -> {
            }
        }
    }

    private fun enableExtensionFeature(imageCaptureExtender: ImageCaptureExtender) {
        if (imageCaptureExtender.isExtensionAvailable) {
            imageCaptureExtender.enableExtension()
        } else {
            Toast.makeText(this, getString(R.string.extension_unavailable),
                Toast.LENGTH_SHORT).show()
            extensionFeatures.setSelection(0)
        }
    }

    private fun updateTransform() {
        val matrix = Matrix()

        val centerX = previewView.width / 2f
        val centerY = previewView.height / 2f

        val rotationDegrees = when (previewView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        previewView.setTransform(matrix)
    }

    private fun disableActions() {
        previewView.isClickable = false
        takeShoot.isClickable=false
        takenImage.isClickable = false
        toggleCameraLens.isClickable = false
        saveImageSwitch.isClickable = false
    }

    private fun enableActions() {
        previewView.isClickable = true
        takeShoot.isClickable=true
        takenImage.isClickable = true
        toggleCameraLens.isClickable = true
        saveImageSwitch.isClickable = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                previewView.post { startCamera() }
            } else {
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
