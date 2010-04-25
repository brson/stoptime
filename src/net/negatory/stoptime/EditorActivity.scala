package net.negatory.stoptime

import android.app.Activity
import android.os.Bundle
import android.graphics.PixelFormat
import android.hardware.Camera
import android.view.ViewGroup.LayoutParams
import android.view._
import android.content.res.Configuration
import android.content.Context
import android.content.pm.ActivityInfo
import android.widget.Button
import android.view.View.OnClickListener
import collection.jcl.MutableIterator

class EditorActivity extends Activity with SurfaceHolder.Callback with Logging {

  private var camera: Option[Camera] = None
  private var sceneStore: SceneStore = new SceneStore(this)
  private var scene: Scene = Scene.DefaultScene

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)

    getWindow().setFormat(PixelFormat.TRANSLUCENT)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN)
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    setContentView(R.layout.editor_layout)

    findViewById(R.id.surface_camera) match {
      case sv: SurfaceView =>

        // Configure the surface holder
        val surfaceHolder = sv.getHolder
        surfaceHolder.addCallback(this)
        // We'll manage the buffers
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        // TODO: It would be nice not to have to open the camera here
        // Can setting the surface view layout params be deferred?
        val tmpCamera = Camera.open
        logHardwareStats(tmpCamera)

        // Configure the SurfaceView for the camera preview
        new PreviewCalculator(this).setLayoutParams(sv, tmpCamera)

        tmpCamera.release
      case _ => error("Failed to find SurfaceView for camera")
    }


    findViewById(R.id.snapshot_button) match {
      case snapshotButton: Button =>
        snapshotButton.setOnClickListener(new OnClickListener {
          def onClick(view: View) = takeSnapshot
        })
      case _ => error("Failed to find snapshot Button")
    }

  }

  override def surfaceCreated(holder: SurfaceHolder) {

    assert(camera isEmpty)
    camera = Some(Camera.open)
    // todo: Check for errors
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

    camera = camera match {
      case Some(camera) =>
        camera.stopPreview
        val cameraParams: Camera#Parameters = camera.getParameters
        cameraParams.setPreviewSize(width, height)
        camera.setParameters(cameraParams)
        camera.setPreviewDisplay(holder)
        camera.startPreview
        Some(camera)
      case None => error("Surface changed but no camera?")
    }

  }

  def logHardwareStats(camera: Camera) {

    val params: Camera#Parameters = camera.getParameters

    Log.i("Supported picture sizes:")
    params.getSupportedPictureSizes match {
      case supportedPictureSizes: java.util.List[_] =>
        for (size <- new MutableIterator.Wrapper(supportedPictureSizes.iterator)) {
          Log.i(size.width + "x" + size.height)
        }
      // Emulator might return null
      case null => Log.i( "none")
    }

    Log.i("Supported preview sizes:")
    params.getSupportedPreviewSizes match {
      case supportedPreviewSizes: java.util.List[_] =>
        for (size <- new MutableIterator.Wrapper(supportedPreviewSizes.iterator)) {
          Log.i(size.width + "x" + size.height)
        }
      // Emulator might return null
      case null => Log.i("none")
    }
  }

  private def takeSnapshot: Unit = {

    val shutterListener = new Camera.ShutterCallback {
      def onShutter: Unit = {
        Log.d("onShutter")
      }
    }

    val jpegListener = new Camera.PictureCallback {
      def onPictureTaken(data: Array[Byte], camera: Camera): Unit = {
        Log.d("Picture taken")

        scene = initializeScene

        scene.appendFrame(data)

        camera.startPreview
      }
    }

    camera match {
      case Some(camera) =>
        camera.takePicture(
          shutterListener,
          null,
          jpegListener
        )
      // Do nothing if we don't have a camera. This allows us to leave the snapshot button enabled
      // even when we don't have a camera.
      case None => ()
    }
  }

  private def initializeScene: Scene = if (scene == Scene.DefaultScene) sceneStore.newScene else scene

}


class PreviewCalculator(activity: Activity) extends AnyRef with Logging {

    def setLayoutParams(surfaceView: SurfaceView, camera: Camera) {
        // todo: wtf is up with this # syntax?
    val cameraParams: Camera#Parameters = camera.getParameters
    val (previewWidth, previewHeight) = calculatePreviewSize(cameraParams)
    val layoutParams: LayoutParams = surfaceView.getLayoutParams
    layoutParams.width = previewWidth
    layoutParams.height = previewHeight
    surfaceView.setLayoutParams(layoutParams)
  }

  // I think a lot of this is unnecessary since we're forcing landscape mode
  def calculatePreviewSize(params: Camera#Parameters): (Int, Int) = {
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
    val wm: WindowManager = activity.getSystemService(Context.WINDOW_SERVICE) match {
      case wm: WindowManager => wm
      case _ => throw new ClassCastException
    }
    val display = wm.getDefaultDisplay
    display.getOrientation match {
      case Configuration.ORIENTATION_PORTRAIT => Log.d("Orientation: portrait")
      case Configuration.ORIENTATION_LANDSCAPE => Log.d("Orientation: landscape")
      case Configuration.ORIENTATION_SQUARE => Log.d("Orientation: square")
      case Configuration.ORIENTATION_UNDEFINED => Log.d("Orientation: undefined")
      case otherOrientation => Log.d("Orientation: " + otherOrientation)
    }
    display.getOrientation
  }

  private def maxPreviewSize: (Int, Int) = {

    val display = activity.getWindowManager.getDefaultDisplay
    (display.getWidth, display.getHeight)
  }
}