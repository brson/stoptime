package net.negatory.stoptime

import android.app.Activity
import android.os.Bundle
import android.graphics.PixelFormat
import android.view.{SurfaceView, WindowManager, Window, SurfaceHolder}
import android.hardware.Camera

class EditorActivity extends Activity with SurfaceHolder.Callback {

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

    // todo: why doesn't this work
    val params/*: Camera.Parameters*/ = camera.getParameters
    params.setPreviewSize(width, height)
    camera.setParameters(params)
    camera.setPreviewDisplay(holder)
    camera.startPreview
    previewRunning = true
  }
}