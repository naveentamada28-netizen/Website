import AVFoundation
import CoreLocation
import SwiftUI

struct CameraView: View {
    @StateObject private var model = CameraModel()
    @State private var note = ""
    @State private var showLibrary = false

    var body: some View {
        ZStack {
            CameraPreview(session: model.session).ignoresSafeArea()
            VStack {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("NT™ Geo Camera").font(.headline)
                        Text(model.videoMode ? "Video · \(model.qualityLabel)" : "Photo")
                            .font(.caption)
                            .foregroundStyle(Color(red: 0.36, green: 0.42, blue: 0.48))
                    }
                    Spacer()
                }
                .padding(12)
                .background(.white.opacity(0.92))
                .clipShape(RoundedRectangle(cornerRadius: 16))
                Spacer()
                Text(model.statusLine)
                    .font(.caption.monospaced())
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(.white.opacity(0.92))
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                TextField("Site or job note", text: $note)
                    .padding(12)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                HStack {
                    Button("Library") { showLibrary = true }
                    Spacer()
                    Button(action: model.videoMode ? model.toggleRecording : { model.takePhoto(note: note) }) {
                        Circle()
                            .stroke(model.recording ? Color.red : Color(red: 0.66, green: 0.52, blue: 0.18), lineWidth: 5)
                            .background(Circle().fill(.white).padding(6))
                            .frame(width: 76, height: 76)
                    }
                    Spacer()
                    Button(model.videoMode ? "Photo" : "Video") { model.toggleMode() }
                        .buttonStyle(.bordered)
                        .disabled(model.recording)
                }
                .padding(10)
                .background(.white.opacity(0.92))
                .clipShape(RoundedRectangle(cornerRadius: 20))
            }
            .padding(12)
        }
        .background(Color(red: 0.96, green: 0.97, blue: 0.98))
        .onAppear { model.start() }
        .onDisappear { model.stop() }
        .sheet(isPresented: $showLibrary) { LibraryView() }
    }
}

struct LibraryView: View {
    @State private var photos = PhotoLibrary.photos()
    @State private var shareURL: URL?

    var body: some View {
        NavigationStack {
            VStack {
                Button("Make 4-up PDF") {
                    shareURL = PhotoLibrary.makePdf()
                    photos = PhotoLibrary.photos()
                }
                .buttonStyle(.borderedProminent)
                .padding(.top, 8)
                if photos.isEmpty {
                    Text("No photos yet. Take one from the camera.")
                        .foregroundStyle(.secondary)
                        .padding()
                    Spacer()
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            ForEach(photos) { photo in
                                VStack(alignment: .leading, spacing: 4) {
                                    if let image = UIImage(contentsOfFile: photo.imageURL.path) {
                                        Image(uiImage: image).resizable().scaledToFill().frame(height: 140).clipped()
                                    }
                                    Text(photo.when).font(.caption2)
                                    Text(photo.coordinate).font(.caption2.monospaced())
                                    if !photo.note.isEmpty { Text(photo.note).font(.caption2) }
                                }
                                .padding(8)
                                .background(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                        }
                        .padding(12)
                    }
                }
            }
            .background(Color(red: 0.96, green: 0.97, blue: 0.98))
            .navigationTitle("NT™ Gallery")
            .sheet(isPresented: Binding(get: { shareURL != nil }, set: { if !$0 { shareURL = nil } })) {
                if let shareURL { ShareLink(item: shareURL) { Text("Share PDF") }.padding() }
            }
        }
    }
}

struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = session
        view.previewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}
}

final class PreviewView: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
}

final class CameraModel: NSObject, ObservableObject, AVCapturePhotoCaptureDelegate, AVCaptureFileOutputRecordingDelegate, CLLocationManagerDelegate {
    let session = AVCaptureSession()
    private let photoOutput = AVCapturePhotoOutput()
    private let movieOutput = AVCaptureMovieFileOutput()
    private let locationManager = CLLocationManager()
    private let queue = DispatchQueue(label: "nt.camera")
    private var pendingNote = ""
    @Published var videoMode = false
    @Published var recording = false
    @Published var qualityLabel = "highest"
    @Published var statusLine = "Waiting for GPS…"
    private var location: CLLocation?

    func start() {
        locationManager.delegate = self
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
        queue.async { self.configure() }
    }

    func stop() {
        locationManager.stopUpdatingLocation()
        queue.async { if self.session.isRunning { self.session.stopRunning() } }
    }

    func toggleMode() {
        videoMode.toggle()
        queue.async {
            self.session.beginConfiguration()
            self.applyPreset()
            self.session.commitConfiguration()
        }
    }

    func takePhoto(note: String) {
        pendingNote = note
        let settings = AVCapturePhotoSettings()
        if let device = AVCaptureDevice.default(for: .video),
           let max = device.activeFormat.supportedMaxPhotoDimensions.max(by: { ($0.width * $0.height) < ($1.width * $1.height) }) {
            settings.maxPhotoDimensions = max
        }
        photoOutput.capturePhoto(with: settings, delegate: self)
    }

    func toggleRecording() {
        if movieOutput.isRecording {
            movieOutput.stopRecording()
            return
        }
        let url = PhotoLibrary.folder.appendingPathComponent("GEO_\(Int(Date().timeIntervalSince1970)).mov")
        recording = true
        movieOutput.startRecording(to: url, recordingDelegate: self)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        location = locations.last
        if let location {
            statusLine = String(format: "GPS ±%.0f m", location.horizontalAccuracy)
        }
    }

    private func configure() {
        session.beginConfiguration()
        session.inputs.forEach { session.removeInput($0) }
        session.outputs.forEach { session.removeOutput($0) }
        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: camera),
              session.canAddInput(input) else {
            session.commitConfiguration()
            return
        }
        session.addInput(input)
        if let mic = AVCaptureDevice.default(for: .audio),
           let micInput = try? AVCaptureDeviceInput(device: mic),
           session.canAddInput(micInput) {
            session.addInput(micInput)
        }
        if session.canAddOutput(photoOutput) { session.addOutput(photoOutput) }
        if session.canAddOutput(movieOutput) { session.addOutput(movieOutput) }
        applyPreset()
        session.commitConfiguration()
        if !session.isRunning { session.startRunning() }
    }

    private func applyPreset() {
        let preset: AVCaptureSession.Preset = videoMode
            ? (session.canSetSessionPreset(.hd4K3840x2160) ? .hd4K3840x2160 : .high)
            : .photo
        if session.canSetSessionPreset(preset) { session.sessionPreset = preset }
        let label = preset == .hd4K3840x2160 ? "4K" : (videoMode ? "highest available" : "photo")
        DispatchQueue.main.async { self.qualityLabel = label }
    }

    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        guard let data = photo.fileDataRepresentation(), let image = UIImage(data: data) else { return }
        _ = try? PhotoLibrary.save(image: image, location: location, note: pendingNote)
    }

    func fileOutput(_ output: AVCaptureFileOutput, didFinishRecordingTo outputFileURL: URL, from connections: [AVCaptureConnection], error: Error?) {
        DispatchQueue.main.async { self.recording = false }
    }
}
