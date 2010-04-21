package net.negatory.stoptime

import android.app.Activity
import android.os.Bundle
import android.graphics.PixelFormat
import android.hardware.Camera
import collection.jcl.MutableIterator.Wrapper
import android.view.ViewGroup.LayoutParams
import android.view._
import android.content.res.Configuration
import android.content.Context

class EditorActivity extends Activity with SurfaceHolder.Callback with Logging {

  // todo: these should probably be Options
  private var surfaceView: Option[SurfaceView] = None
  private var camera: Option[Camera] = None

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)

    getWindow().setFormat(PixelFormat.TRANSLUCENT)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN)
    setContentView(R.layout.editor_layout)

    surfaceView = findViewById(R.id.surface_camera) match {
      case sv: SurfaceView => Some(sv)
      case _ => error("Failed to find SurfaceView for camera")
    }

    val surfaceHolder = (surfaceView get) getHolder

    surfaceHolder.addCallback(this)
    // We'll manage the buffers
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

    val tmpCamera = Camera.open
    logHardwareStats(tmpCamera)

    fitSurfaceToCamera(surfaceView.get, tmpCamera)

    tmpCamera.release
  }

  override def surfaceCreated(holder: SurfaceHolder) {

    // todo move camera opening here, but make sure it works on hardware
  }

  override def surfaceDestroyed(holder: SurfaceHolder) {

    camera = camera match {
      case Some(camera) =>
        camera.stopPreview
        camera.release
        None
      case None => error("Surface destroyed but no camera?")
    }
  }
  
  override def surfaceChanged(
          holder: SurfaceHolder,
          format: Int,
          width: Int,
          height: Int) {

    Log.d("Surface changed, new size = " + width + "x" + height)

    for (camera <- this.camera) {
      camera.stopPreview
      camera.release
    }

    val camera = Camera.open
    val cameraParams: Camera#Parameters = camera.getParameters
    cameraParams.setPreviewSize(width, height)
    camera.setParameters(cameraParams)
    camera.setPreviewDisplay(holder)
    camera.startPreview
    this.camera = Some(camera)
  }

  private def fitSurfaceToCamera(surfaceView: SurfaceView, camera: Camera) {
        // todo: wtf is up with this # syntax?
    val cameraParams: Camera#Parameters = camera.getParameters
    val (previewWidth, previewHeight) = calculatePreviewSize(cameraParams)
    val layoutParams: LayoutParams = surfaceView.getLayoutParams
    layoutParams.width = previewWidth
    layoutParams.height = previewHeight
    surfaceView.setLayoutParams(layoutParams)
  }

  private def calculatePreviewSize(params: Camera#Parameters): (Int, Int) = {
    val previewScale = this.previewScale(params)
    val mustRotate =
      (previewScale > 1 && orientation == Configuration.ORIENTATION_PORTRAIT) ||
      (previewScale < 1 && orientation == Configuration.ORIENTATION_LANDSCAPE)

    if (mustRotate)
      Log.d("Rotating preview")
    else
      Log.d("Not rotating preview")

    val realPreviewScale = if (!mustRotate) previewScale else 1 / previewScale

    val (maxWidth, maxHeight) = maxPreviewSize
    val frameScale: Float = maxWidth / maxHeight
    // Maximize the preview size on the screen while keeping the surface scaled correctly
    val previewSize: (Int, Int) = {
      if (realPreviewScale < frameScale) ((maxHeight.toFloat * realPreviewScale).toInt, maxHeight)
      else (maxWidth, (maxHeight.toFloat / realPreviewScale).toInt)
    }

    val (previewWidth, previewHeight) = previewSize
    Log.i("Using preview size " + previewWidth + "x" + previewHeight)
    previewSize
  }

  private def previewScale(params: Camera#Parameters): Float = {
    val default = (200, 150)

    val (previewWidth, previewHeight) = params.getSupportedPreviewSizes match {
      case supportedPreviewSizes: java.util.List[_] =>
        // my droid doesn't work with previewSize(0)
        // todo: there might be a better preview sie to select here
        if (supportedPreviewSizes.size > 1) {
          val size: Camera#Size  = supportedPreviewSizes.get(1)
          (size.width, size.height)
        }
        else default
      case null => default
    }

    val previewScale: Float = previewWidth / previewHeight

    Log.d("Preview scale: " + previewScale)
    previewScale
  }

  private def orientation: Int = {
    val wm: WindowManager = getSystemService(Context.WINDOW_SERVICE) match {
      case wm: WindowManager => wm
      case _ => throw new ClassCastException
    }
    val display = wm.getDefaultDisplay
    display.getOrientation
  }

  private def maxPreviewSize: (Int, Int) = {

    val display = getWindowManager.getDefaultDisplay
    (display.getWidth, display.getHeight)
  }

  def logHardwareStats(camera: Camera) {

    val params: Camera#Parameters = camera.getParameters

    Log.i("Supported picture sizes:")
    params.getSupportedPictureSizes match {
      case supportedPictureSizes: java.util.List[_] =>
        for (size <- new Wrapper(supportedPictureSizes.iterator)) {
          Log.i(size.width + "x" + size.height)
        }
      // Emulator might return null
      case null => Log.i( "none")
    }

    Log.i("Supported preview sizes:")
    params.getSupportedPreviewSizes match {
      case supportedPreviewSizes: java.util.List[_] =>
        for (size <- new Wrapper(supportedPreviewSizes.iterator)) {
          Log.i(size.width + "x" + size.height)
        }
      // Emulator might return null
      case null => Log.i("none")
    }
  }


}