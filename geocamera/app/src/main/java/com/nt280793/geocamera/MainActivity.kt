package com.nt280793.geocamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.media.MediaActionSound
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.text.InputFilter
import android.view.Surface
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nt280793.geocamera.databinding.ActivityMainBinding
import com.nt280793.geocamera.databinding.DialogSettingsBinding
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), android.hardware.SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: AppSettings
    private lateinit var tracker: LocationTracker
    private val io = Executors.newSingleThreadExecutor()
    private val geoPool = Executors.newSingleThreadExecutor()
    private val shutterSound = MediaActionSound()
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraBound = false
    private var saving = false
    private var askedOnce = false
    private var geoToken = 0
    private var lastHud = 0L
    private var usingRotationVector = false

    @Volatile private var location: Location? = null
    @Volatile private var address: String? = null
    @Volatile private var addressAnchor: Location? = null
    @Volatile private var heading: Float? = null

    private val accel = FloatArray(3)
    private val mag = FloatArray(3)
    private var haveAccel = false
    private var haveMag = false
    private lateinit var sensorManager: android.hardware.SensorManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasCamera()) showCamera()
        else {
            binding.grantButton.setText(R.string.open_settings)
            binding.grantButton.setOnClickListener { openAppSettings() }
            showPermissionPane(getString(R.string.permission_denied))
        }
        if (hasLocationPermission()) tracker.start()
        renderHud()
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = AppSettings(this)
        sensorManager = getSystemService(android.hardware.SensorManager::class.java)
        tracker = LocationTracker(this, ::acceptLocation)
        shutterSound.load(MediaActionSound.SHUTTER_CLICK)
        binding.noteInput.filters = arrayOf(InputFilter.LengthFilter(80))
        binding.noteInput.setText(settings.note)
        binding.noteInput.doAfterTextChanged { renderHud() }
        binding.flashButton.setOnClickListener { cycleFlash() }
        binding.settingsButton.setOnClickListener { showSettings() }
        binding.flipButton.setOnClickListener { flipCamera() }
        binding.shutter.setOnClickListener { capture() }
        binding.galleryButton.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        binding.grantButton.setOnClickListener { permissionLauncher.launch(neededPermissions()) }
        binding.preview.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            imageCapture?.targetRotation = binding.preview.display?.rotation ?: Surface.ROTATION_0
        }
        binding.chrome.visibility = View.INVISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (hasCamera()) {
            showCamera()
            refreshThumbAsync()
        } else if (!askedOnce) {
            askedOnce = true
            showPermissionPane(getString(R.string.permission_body))
            permissionLauncher.launch(neededPermissions())
        } else {
            showPermissionPane(getString(R.string.permission_denied))
        }
        if (hasLocationPermission()) tracker.start() else tracker.stop()
        registerSensors()
        renderHud()
    }

    override fun onPause() {
        settings.note = binding.noteInput.text?.toString().orEmpty()
        tracker.stop()
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        shutterSound.release()
        io.shutdown()
        geoPool.shutdown()
        super.onDestroy()
    }

    override fun onSensorChanged(event: android.hardware.SensorEvent) {
        when (event.sensor.type) {
            android.hardware.Sensor.TYPE_ROTATION_VECTOR -> {
                usingRotationVector = true
                val rotation = FloatArray(9)
                android.hardware.SensorManager.getRotationMatrixFromVector(rotation, event.values)
                heading = azimuth(rotation)
            }
            android.hardware.Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accel, 0, 3)
                haveAccel = true
                updateFromAccelMag()
            }
            android.hardware.Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, mag, 0, 3)
                haveMag = true
                updateFromAccelMag()
            }
        }
        renderHudThrottled()
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit

    private fun showCamera() {
        binding.permissionPane.visibility = View.GONE
        binding.chrome.visibility = View.VISIBLE
        if (!cameraBound) bindCamera(lensFacing)
    }

    private fun showPermissionPane(message: String) {
        cameraBound = false
        binding.chrome.visibility = View.INVISIBLE
        binding.permissionPane.visibility = View.VISIBLE
        binding.permissionText.text = message
    }

    private fun bindCamera(facing: Int) {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (isDestroyed) return@addListener
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = binding.preview.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setJpegQuality(92)
                    .setTargetRotation(binding.preview.display?.rotation ?: Surface.ROTATION_0)
                    .build()
                val selector = CameraSelector.Builder().requireLensFacing(facing).build()
                provider.unbindAll()
                camera = provider.bindToLifecycle(this, selector, preview, capture)
                imageCapture = capture
                lensFacing = facing
                cameraBound = true
                applyFlash()
            } catch (_: Exception) {
                cameraBound = false
                if (facing != CameraSelector.LENS_FACING_BACK) {
                    bindCamera(CameraSelector.LENS_FACING_BACK)
                } else {
                    showPermissionPane(getString(R.string.camera_unavailable))
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun flipCamera() {
        val next = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCamera(next)
    }

    private fun cycleFlash() {
        settings.flash = when (settings.flash) {
            "off" -> "on"
            "on" -> "auto"
            else -> "off"
        }
        applyFlash()
        renderHud()
    }

    private fun applyFlash() {
        val mode = when (settings.flash) {
            "on" -> ImageCapture.FLASH_MODE_ON
            "auto" -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = mode
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
        camera?.cameraControl?.enableTorch(hasFlash && settings.flash == "on")
    }

    private fun capture() {
        val capture = imageCapture
        if (saving || capture == null) return
        saving = true
        binding.shutter.isEnabled = false
        binding.shutter.alpha = 0.55f
        shutterSound.play(MediaActionSound.SHUTTER_CLICK)
        binding.flashWash.animate().cancel()
        binding.flashWash.alpha = 0.85f
        binding.flashWash.animate().alpha(0f).setDuration(180).start()
        capture.targetRotation = binding.preview.display?.rotation ?: Surface.ROTATION_0
        val note = binding.noteInput.text?.toString().orEmpty()
        settings.note = note
        val shotAt = System.currentTimeMillis()
        val file = File(cacheDir, "shot-$shotAt.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            options,
            io,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val base = currentInfo(note, shotAt)
                    val loc = location
                    val info = if (base.showAddress && base.address.isNullOrBlank() && loc != null) {
                        val line = geocodeForCapture(loc)
                        if (line.isNullOrBlank()) base else base.copy(address = line)
                    } else {
                        base
                    }
                    val uri = try {
                        PhotoProcessor.process(this@MainActivity, file, info)
                    } catch (_: Exception) {
                        null
                    }
                    file.delete()
                    runOnUiThread {
                        saving = false
                        if (isDestroyed) return@runOnUiThread
                        binding.shutter.isEnabled = true
                        binding.shutter.alpha = 1f
                        if (uri != null) {
                            CaptureIndex(this@MainActivity).add(uri)
                            showThumb(uri)
                            renderHud()
                            Toast.makeText(this@MainActivity, R.string.saved, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, R.string.save_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    file.delete()
                    runOnUiThread {
                        saving = false
                        if (isDestroyed) return@runOnUiThread
                        binding.shutter.isEnabled = true
                        binding.shutter.alpha = 1f
                        Toast.makeText(this@MainActivity, R.string.capture_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    private fun acceptLocation(next: Location) {
        val prev = location
        if (prev != null) {
            val newer = next.elapsedRealtimeNanos >= prev.elapsedRealtimeNanos
            val better = next.hasAccuracy() && (!prev.hasAccuracy() || next.accuracy + 5f < prev.accuracy)
            if (!better && !newer) return
            val freshWindow = next.elapsedRealtimeNanos - prev.elapsedRealtimeNanos < 20_000_000_000L
            val muchWorse = prev.hasAccuracy() && next.hasAccuracy() && next.accuracy > prev.accuracy + 25f
            if (newer && muchWorse && freshWindow) return
        }
        location = Location(next)
        renderHud()
        maybeGeocode(location ?: return)
    }

    private fun maybeGeocode(loc: Location) {
        if (!settings.showAddress) return
        val anchor = addressAnchor
        if (address != null && anchor != null && anchor.distanceTo(loc) < 40f) return
        val token = ++geoToken
        val target = Location(loc)
        io.execute {
            val line = geocode(target.latitude, target.longitude) ?: return@execute
            if (token != geoToken) return@execute
            address = line
            addressAnchor = target
            runOnUiThread { if (!isDestroyed) renderHud() }
        }
    }

    private fun geocodeForCapture(loc: Location): String? {
        geoToken += 1
        val line = geocode(loc.latitude, loc.longitude)
        if (!line.isNullOrBlank()) {
            address = line
            addressAnchor = Location(loc)
        }
        return line
    }

    private fun geocode(lat: Double, lon: Double): String? {
        val future = geoPool.submit<String?> { geocodeBlocking(lat, lon) }
        return try {
            future.get(3, TimeUnit.SECONDS)
        } catch (_: Exception) {
            future.cancel(true)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun geocodeBlocking(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            Geocoder(this, Locale.getDefault())
                .getFromLocation(lat, lon, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
                ?.trim()
                ?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun currentInfo(note: String, at: Long): StampInfo {
        val loc = location
        return StampInfo(
            capturedAtMillis = at,
            latitude = loc?.latitude,
            longitude = loc?.longitude,
            accuracyM = loc?.takeIf { it.hasAccuracy() }?.accuracy,
            altitudeM = loc?.takeIf { it.hasAltitude() }?.altitude,
            headingDeg = heading,
            address = address,
            note = note,
            useDms = settings.useDms,
            showAddress = settings.showAddress,
            showAltitude = settings.showAltitude,
            showHeading = settings.showHeading
        )
    }

    private fun renderHudThrottled() {
        val now = SystemClock.uptimeMillis()
        if (now - lastHud < 200) return
        lastHud = now
        renderHud()
    }

    private fun renderHud() {
        if (!::binding.isInitialized) return
        val info = currentInfo(binding.noteInput.text?.toString().orEmpty(), System.currentTimeMillis())
        binding.stampText.text = StampText.block(info)
        binding.gpsChip.text = gpsLabel()
        binding.gpsChip.setTextColor(ContextCompat.getColor(this, gpsColor()))
        binding.flashButton.setText(flashLabel())
    }

    private fun gpsLabel(): String {
        if (!hasLocationPermission()) return getString(R.string.location_off)
        if (!tracker.providersEnabled()) return getString(R.string.location_disabled)
        val loc = location ?: return getString(R.string.waiting_for_gps)
        if (!loc.hasAccuracy()) return getString(R.string.gps_no_accuracy)
        val ageSeconds = (System.currentTimeMillis() - loc.time) / 1000
        val meters = loc.accuracy.roundToInt()
        return if (ageSeconds > 120) {
            getString(R.string.gps_fix_stale, meters)
        } else {
            getString(R.string.gps_fix, meters)
        }
    }

    private fun gpsColor(): Int {
        val loc = location
        if (!hasLocationPermission() || loc == null) return R.color.danger
        if (!loc.hasAccuracy() || loc.accuracy > 30f) return R.color.amber
        return R.color.mint
    }

    private fun flashLabel(): Int = when (settings.flash) {
        "on" -> R.string.flash_on
        "auto" -> R.string.flash_auto
        else -> R.string.flash_off
    }

    private fun showSettings() {
        val dialog = DialogSettingsBinding.inflate(layoutInflater)
        dialog.switchAddress.isChecked = settings.showAddress
        dialog.switchDms.isChecked = settings.useDms
        dialog.switchAltitude.isChecked = settings.showAltitude
        dialog.switchHeading.isChecked = settings.showHeading
        dialog.versionText.text = getString(R.string.version_label, BuildConfig.VERSION_NAME)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setView(dialog.root)
            .setPositiveButton(R.string.done) { _, _ ->
                settings.showAddress = dialog.switchAddress.isChecked
                settings.useDms = dialog.switchDms.isChecked
                settings.showAltitude = dialog.switchAltitude.isChecked
                settings.showHeading = dialog.switchHeading.isChecked
                renderHud()
            }
            .show()
    }

    private fun refreshThumbAsync() {
        io.execute {
            val uri = CaptureIndex(this).all().firstOrNull() ?: PhotoStore.list(this).firstOrNull()
            runOnUiThread { if (!isDestroyed) showThumb(uri) }
        }
    }

    private fun showThumb(uri: android.net.Uri?) {
        if (uri == null) {
            binding.galleryButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.galleryButton.setImageResource(R.drawable.ic_gallery)
            return
        }
        binding.galleryButton.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.galleryButton.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_gallery)
            error(R.drawable.ic_gallery)
        }
    }

    private fun registerSensors() {
        val rotation = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD)
        rotation?.let {
            sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
        accelSensor?.let {
            sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
        magSensor?.let {
            sensorManager.registerListener(this, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun updateFromAccelMag() {
        if (usingRotationVector || !haveAccel || !haveMag) return
        val rotation = FloatArray(9)
        val inclination = FloatArray(9)
        if (android.hardware.SensorManager.getRotationMatrix(rotation, inclination, accel, mag)) {
            heading = azimuth(rotation)
        }
    }

    private fun azimuth(rotation: FloatArray): Float {
        val orientation = FloatArray(3)
        android.hardware.SensorManager.getOrientation(rotation, orientation)
        var degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (degrees < 0) degrees += 360f
        return degrees
    }

    private fun neededPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT <= 28) permissions += Manifest.permission.WRITE_EXTERNAL_STORAGE
        return permissions.toTypedArray()
    }

    private fun hasCamera(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }
}
