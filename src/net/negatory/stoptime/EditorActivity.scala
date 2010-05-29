package net.negatory.stoptime

import android.app.Activity
import android.hardware.Camera
import android.view.ViewGroup.LayoutParams
import android.view._
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.view.View.OnClickListener
import android.view.Menu.NONE
import collection.JavaConversions._
import android.graphics.{BitmapFactory, PixelFormat, Bitmap}
import android.os.{Message, Handler, Bundle}
import android.widget.{Toast, ImageView, Button}
import java.io.ByteArrayOutputStream
import android.content.{Intent, Context}


class EditorActivity extends Activity with SurfaceHolder.Callback with Logging {

  private val OVERLAY_ALPHA = 64

  private var dao: DAO = null
  private var camera: Option[Camera] = None
  private var sceneStore: SceneStore = null
  private var scene: Scene = Scene.DefaultScene
  private var previewSurface: SurfaceView = null
  private var overlayView: ImageView = null
  private var overlayBitmap: Option[Bitmap] = None
  private var previewSize: (Int, Int) = null

  override def onCreate(savedInstanceState: Bundle) {

    super.onCreate(savedInstanceState)
    
    dao = new DAO(this)
    sceneStore = new SceneStore(dao)
    
    getWindow().setFormat(PixelFormat.TRANSLUCENT)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    getWindow().setFlags(
      WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN)
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    setContentView(R.layout.editor_layout)

    previewSurface = findViewById(R.id.surface_camera) match {
      case sv: SurfaceView =>

        // Configure the surface holder
        val surfaceHolder = sv.getHolder
        surfaceHolder.addCallback(this)
        // We'll manage the buffers
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        sv
      case _ => error("Failed to find SurfaceView for camera")
    }

    overlayView = findViewById(R.id.image_frame) match {
      case iv: ImageView =>
        iv.setAlpha(OVERLAY_ALPHA)
        iv
      case _ => error("Failed to find ImageView for frame display")  
    }


    findViewById(R.id.snapshot_button) match {
      case snapshotButton: Button =>
        snapshotButton.setOnClickListener(new OnClickListener {
          def onClick(view: View) = takeSnapshot
        })
      case _ => error("Failed to find snapshot Button")
    }

    findViewById(R.id.overlay_button) match {
      case b: Button =>
        b.setOnClickListener(new OnClickListener {
          def onClick(v: View) {
            overlayView.setVisibility(overlayView.getVisibility match {
              case View.INVISIBLE => View.VISIBLE
              case View.VISIBLE => View.INVISIBLE
              case _ => error("Unexpected overlay visibility")
            })
          }
        })
      case _ => error("Failed to find overlay Button")
    }

    findViewById(R.id.playback_button) match {
      case b: Button =>
        b.setOnClickListener(new OnClickListener {
          def onClick(v: View) {
            beginPlayback
          }
        })
    }

  }

  override def onDestroy() {
    super.onDestroy
    dao.close
    if (overlayBitmap.isDefined) overlayBitmap.get.recycle
  }

  override def onStart() {
    super.onStart

    val intent = getIntent
    scene = intent.getData match {
      case null => scene
      case data =>
        val sceneId = data.getFragment.toInt
        Log.i("Editing scene " + sceneId)
        new DAO(this).loadScene(sceneId)
    }
  }

  override def surfaceCreated(holder: SurfaceHolder) {

    assert(camera isEmpty)
    camera = Camera.open match {
      case camera =>
        logHardwareStats(camera)
        configureCameraAndViewDimensions(previewSurface, overlayView, camera)
        Some(camera)
    }
    // todo: Check for errors
  }

  def configureCameraAndViewDimensions(surfaceView: SurfaceView, frameImage: ImageView, camera: Camera) {

    val maxPreviewSize: (Int, Int) = {
      val display = getWindowManager.getDefaultDisplay
      (display.getWidth, display.getHeight)
    }
    // Assuming that the display size is a valid preview size
    previewSize = maxPreviewSize

    val (previewWidth, previewHeight) = previewSize
    Log.i("Using preview size " + previewWidth + "x" + previewHeight)

    val cameraParams: Camera#Parameters = camera.getParameters
    // Configure the preview surface size
    val svLayoutParams: LayoutParams = surfaceView.getLayoutParams
    svLayoutParams.width = previewWidth
    svLayoutParams.height = previewHeight
    surfaceView.setLayoutParams(svLayoutParams)

    // Configure the frame overlay size. Note that a device may have preview sizes
    // and picture sizes with different aspect ratios. We may need to do more
    // calculation here.
    val ivLayoutParams: LayoutParams = frameImage.getLayoutParams
    ivLayoutParams.width = previewWidth
    ivLayoutParams.height = previewHeight
    frameImage.setLayoutParams(ivLayoutParams)

    val defaultPictureSize = (200, 100)
    val (width, height) = cameraParams.getSupportedPictureSizes match {
      case sizes: java.util.List[_] =>
        if (sizes.size > 0) {
          val size = sizes.get(sizes.size - 1)
          (size.width, size.height)
        }
        else defaultPictureSize
      case null => defaultPictureSize
    }
    Log.i("Using picture size " + width + "x" + height)
    cameraParams.setPictureSize(width, height)

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
        for (size <- supportedPictureSizes) {
          Log.i(size.width + "x" + size.height)
        }
      // Emulator might return null
      case null => Log.i( "none")
    }

    Log.i("Supported preview sizes:")
    params.getSupportedPreviewSizes match {
      case supportedPreviewSizes: java.util.List[_] =>
        for (size <- supportedPreviewSizes) {
          Log.i(size.width + "x" + size.height)
        }
      // Emulator might return null
      case null => Log.i("none")
    }

    val display = getWindowManager.getDefaultDisplay
    Log.i("Display size: " + display.getWidth + "x" + display.getHeight)
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

        val newBitmap = BitmapFactory.decodeByteArray(data, 0, data.length)
        assert(newBitmap != null)
        val (width, height) = previewSize
        val smallBitmap = Bitmap.createScaledBitmap(newBitmap, width, height, true)
        replaceFrameBitmap(smallBitmap)
        val byteStream = new ByteArrayOutputStream
        smallBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteStream)
        val smallData = byteStream.toByteArray

        try {
          scene.appendFrame(smallData)
        } catch {
          case e => throw e // todo: Error handling
        }

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

  private def replaceFrameBitmap(bitmap: Bitmap) {
    overlayView.setImageBitmap(bitmap)
    // Free up the memory from the previous overlay
    if (overlayBitmap isDefined) overlayBitmap.get.recycle
    overlayBitmap = Some(bitmap)
  }

  private def initializeScene: Scene = if (scene == Scene.DefaultScene) sceneStore.newScene else scene

  private def beginPlayback: Unit = {
    Log.d("Beginning playback")

    val playbackActor = new PlaybackActor(dao, scene)
    lazy val playbackHandler: Handler = {

      val playbackCallback: Handler.Callback = new Handler.Callback {
        override def handleMessage(msg: Message) = {
          Log.d("Handling playback message")
          msg.what match {
            case PlaybackActor.PLAYFRAME =>
              overlayView.setAlpha(255)
              replaceFrameBitmap((msg.obj.asInstanceOf[Frame]).createFrameBitmap)
              playbackActor ! NextFrame(playbackHandler)
            case PlaybackActor.STOP =>
              overlayView.setAlpha(OVERLAY_ALPHA)
              camera.get.startPreview
              ()
          }
          true
        }

      }
      camera.get.stopPreview

      new Handler(playbackCallback)
    }

    playbackActor ! NextFrame(playbackHandler)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    super.onCreateOptionsMenu(menu)

    menu.add(NONE, 0, NONE, "Scenes")

    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case 0 =>
      val intent = new Intent(this, classOf[ScenePickActivity])
      startActivity(intent)
      true
    case _ => error("Unexpected menu item")
  }

}

