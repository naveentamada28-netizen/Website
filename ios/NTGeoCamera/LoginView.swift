import SwiftUI

struct LoginView: View {
    @AppStorage("nt.server") private var server = ""
    @AppStorage("nt.token") private var token = ""
    @AppStorage("nt.mobile") private var savedMobile = ""
    @State private var mobile = ""
    @State private var password = ""
    @State private var status = "Sign in with the mobile number and password stored on your hosting website."
    @State private var busy = false

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                Image("NtMark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 132, height: 132)
                    .padding(.top, 24)
                Text("NT™ Geo Camera")
                    .font(.largeTitle.bold())
                Text(status)
                    .font(.subheadline)
                    .foregroundStyle(Color(red: 0.36, green: 0.42, blue: 0.48))
                    .multilineTextAlignment(.center)
                field("Hosting website", text: $server)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.URL)
                field("Mobile number", text: $mobile)
                    .keyboardType(.phonePad)
                SecureField("Password", text: $password)
                    .padding(12)
                    .background(Color.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                Button(busy ? "Contacting the hosting website…" : "Login") {
                    Task { await submit(register: false) }
                }
                .buttonStyle(.borderedProminent)
                .disabled(busy)
                Button("Create account") {
                    Task { await submit(register: true) }
                }
                .buttonStyle(.bordered)
                .disabled(busy)
            }
            .padding(24)
        }
        .background(Color(red: 0.96, green: 0.97, blue: 0.98))
    }

    private func field(_ title: String, text: Binding<String>) -> some View {
        TextField(title, text: text)
            .padding(12)
            .background(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func submit(register: Bool) async {
        guard AccountRules.serverOk(server) else { status = "Enter the hosting address, starting with https:// or http://"; return }
        guard AccountRules.mobileOk(mobile) else { status = "Enter a mobile number with 10 to 15 digits."; return }
        guard AccountRules.passwordOk(password) else { status = "Use at least 6 characters."; return }
        busy = true
        defer { busy = false }
        if register {
            let created = await AuthClient.register(server: server, mobile: mobile, password: password)
            guard created.ok else { status = created.message; return }
        }
        let signedIn = await AuthClient.login(server: server, mobile: mobile, password: password)
        if signedIn.ok, let value = signedIn.token {
            savedMobile = signedIn.mobile ?? AccountRules.digits(mobile)
            token = value
        } else {
            status = signedIn.message
        }
    }
}
