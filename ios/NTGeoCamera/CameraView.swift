import AVFoundation
import CoreLocation
import SwiftUI

struct CameraView: View {
    @StateObject private var model = CameraModel()
    @State private var note = ""
    @State private var site = SiteRecord.load()
    @State private var showLibrary = false
    @State private var showSite = false

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
                Button(site.summary.isEmpty ? "Add client, mobile, and landmark" : site.summary) {
                    showSite = true
                }
                .buttonStyle(.bordered)
                .frame(maxWidth: .infinity, alignment: .leading)
                TextField("Site or job note", text: $note)
                    .padding(12)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                HStack {
                    Button("Library") { showLibrary = true }
                    Spacer()
                    Button(action: model.videoMode ? { model.toggleRecording(note: note, site: site) } : { model.takePhoto(note: note, site: site) }) {
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
        .sheet(isPresented: $showSite, onDismiss: { site.save() }) {
            SiteEditor(site: $site, coordinate: { model.currentLocation()?.coordinate })
        }
    }
}

struct SiteEditor: View {
    @Binding var site: SiteRecord
    var coordinate: () -> CLLocationCoordinate2D?
    @Environment(\.dismiss) private var dismiss
    @State private var mobileError = ""
    @State private var cornerMessage = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Client details") {
                    TextField("Name, address, or other client details", text: $site.client, axis: .vertical)
                    TextField("Mobile number", text: $site.mobile)
                        .keyboardType(.phonePad)
                    if !mobileError.isEmpty {
                        Text(mobileError).font(.caption).foregroundStyle(.red)
                    }
                }
                Section("Landmark") {
                    TextField("Name and how to recognize this place", text: $site.landmark, axis: .vertical)
                }
                Section {
                    TextField("North boundary", text: $site.north)
                    TextField("East boundary", text: $site.east)
                    TextField("South boundary", text: $site.south)
                    TextField("West boundary", text: $site.west)
                } header: {
                    Text("Boundary")
                } footer: {
                    Text("Name what lies on each side, then stand at the corners and mark them. Three or more corners close the boundary.")
                }
                Section("GPS boundary") {
                    if site.corners.isEmpty {
                        Text("No GPS corners yet.")
                    } else {
                        ForEach(Array(site.corners.enumerated()), id: \.offset) { index, point in
                            Text(String(format: "%d. %.6f, %.6f", index + 1, point.latitude, point.longitude))
                                .font(.caption.monospaced())
                        }
                    }
                    if !cornerMessage.isEmpty {
                        Text(cornerMessage).font(.caption).foregroundStyle(.red)
                    }
                    Button("Mark this corner") {
                        guard let point = coordinate() else {
                            cornerMessage = "Wait for a GPS fix before marking a corner."
                            return
                        }
                        guard site.corners.count < 20 else {
                            cornerMessage = "This boundary already has 20 corners."
                            return
                        }
                        cornerMessage = ""
                        site.corners.append(BoundaryPoint(latitude: point.latitude, longitude: point.longitude))
                    }
                    Button("Remove last") {
                        if !site.corners.isEmpty { site.corners.removeLast() }
                    }
                    .disabled(site.corners.isEmpty)
                }
            }
            .navigationTitle("Client and landmark")
            .toolbar {
                Button("Done") {
                    guard SiteRecord.mobileOk(site.mobile) else {
                        mobileError = "Enter a mobile number with 10 to 15 digits."
                        return
                    }
                    site.mobile = site.cleanedMobile
                    mobileError = ""
                    site.save()
                    dismiss()
                }
            }
        }
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
                                    if !photo.site.isEmpty { Text(photo.site).font(.caption2).lineLimit(4) }
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
    private var pendingSite = SiteRecord()
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

    func currentLocation() -> CLLocation? { location }

    func takePhoto(note: String, site: SiteRecord) {
        pendingNote = note
        pendingSite = site
        let settings = AVCapturePhotoSettings()
        if let device = AVCaptureDevice.default(for: .video),
           let max = device.activeFormat.supportedMaxPhotoDimensions.max(by: { ($0.width * $0.height) < ($1.width * $1.height) }) {
            settings.maxPhotoDimensions = max
        }
        photoOutput.capturePhoto(with: settings, delegate: self)
    }

    func toggleRecording(note: String, site: SiteRecord) {
        if movieOutput.isRecording {
            movieOutput.stopRecording()
            return
        }
        pendingNote = note
        pendingSite = site
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
        _ = try? PhotoLibrary.save(image: image, location: location, note: pendingNote, site: pendingSite)
    }

    func fileOutput(_ output: AVCaptureFileOutput, didFinishRecordingTo outputFileURL: URL, from connections: [AVCaptureConnection], error: Error?) {
        let sidecar = outputFileURL.deletingPathExtension().appendingPathExtension("json")
        let meta = ["note": pendingNote, "site": pendingSite.caption]
        if let data = try? JSONSerialization.data(withJSONObject: meta) {
            try? data.write(to: sidecar)
        }
        DispatchQueue.main.async { self.recording = false }
    }
}
