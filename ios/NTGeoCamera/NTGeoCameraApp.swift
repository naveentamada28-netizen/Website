import SwiftUI

@main
struct NTGeoCameraApp: App {
    var body: some Scene {
        WindowGroup {
            RootView()
                .tint(Color(red: 0.12, green: 0.44, blue: 0.92))
        }
    }
}

struct RootView: View {
    @AppStorage("nt.token") private var token = ""

    var body: some View {
        if token.isEmpty {
            LoginView()
        } else {
            CameraView()
        }
    }
}
