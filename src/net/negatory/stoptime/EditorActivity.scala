package net.negatory.stoptime

import android.app.Activity
import android.os.Bundle
import android.graphics.PixelFormat
import android.view.{SurfaceView, WindowManager, Window, SurfaceHolder}
import android.hardware.Camera
import collection.jcl.MutableIterator.Wrapper

class EditorActivity extends Activity with SurfaceHolder.Callback with Logging {

  private var surfaceView: SurfaceView = null
  private var surfaceHolder: SurfaceHolder = null
  private var camera: Camera = null
  private var previewRunning: Boolean = false

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    getWindow().setFormat(PixelFormat.TRANSLUCENT)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN)
    setContentView(R.layout.camera_surface)

    surfaceView = findViewById(R.id.surface_camera) match {
      case sv: SurfaceView => sv
      case _ => throw new ClassCastException
    }

    surfaceHolder = surfaceView.getHolder()
    surfaceHolder.addCallback(this)
    // We'll manage the buffers
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
  }

  override def surfaceCreated(holder: SurfaceHolder) {
    camera = Camera.open
    logHardwareStats
  }

  override def surfaceDestroyed(holder: SurfaceHolder) {
    camera.stopPreview
    previewRunning = false
    camera.release
  }
  
  override def surfaceChanged(
          holder: SurfaceHolder,
          format: Int,
          width: Int,
          height: Int) {

    if (previewRunning) camera.stopPreview

    // todo: wtf is up with this # syntax?
    val params: Camera#Parameters = camera.getParameters
    
    camera.setParameters(params)
    camera.setPreviewDisplay(holder)
    camera.startPreview
    previewRunning = true
  }

  def logHardwareStats {
    val params/*: Camera.Parameters*/ = camera.getParameters

    Log.i("Supported picture sizes:")
    params.getSupportedPictureSizes match {
      case supportedPictureSizes: java.util.List[Camera#Size] =>
        for (size <- new Wrapper(supportedPictureSizes.iterator)) {
          Log.i(size.width + "x" + size.height)
        }
      // Emulator might return null
      case null => Log.i( "none")
    }

    Log.i("Supported preview sizes:")
    params.getSupportedPreviewSizes match {
      case supportedPreviewSizes: java.util.List[Camera#Size] =>
        for (size <- new Wrapper(supportedPreviewSizes.iterator)) {
          Log.i(size.width + "x" + size.height)
        }
      // Emulator might return null
      case null => Log.i("none")
    }
  }
}