import SwiftUI

@main
struct NTGeoCameraApp: App {
    var body: some Scene {
        WindowGroup {
            CameraView()
                .tint(Color(red: 0.12, green: 0.44, blue: 0.92))
        }
    }
}
