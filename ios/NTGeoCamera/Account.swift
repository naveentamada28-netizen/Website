import Foundation

enum AccountRules {
    static func digits(_ raw: String) -> String {
        raw.filter(\.isNumber)
    }

    static func mobileOk(_ raw: String) -> Bool {
        let count = digits(raw).count
        return count >= 10 && count <= 15
    }

    static func passwordOk(_ raw: String) -> Bool {
        raw.count >= 6
    }

    static func serverOk(_ raw: String) -> Bool {
        let value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        return value.hasPrefix("https://") || value.hasPrefix("http://")
    }
}

struct ReportCell {
    let x: CGFloat
    let y: CGFloat
    let width: CGFloat
    let height: CGFloat
    let imageHeight: CGFloat
}

enum ReportLayout {
    static let pageWidth: CGFloat = 595
    static let pageHeight: CGFloat = 842
    static let perPage = 4

    static func pageCount(_ count: Int) -> Int {
        guard count > 0 else { return 0 }
        return (count + perPage - 1) / perPage
    }

    static func cells(margin: CGFloat = 28, header: CGFloat = 64, gap: CGFloat = 12, caption: CGFloat = 78) -> [ReportCell] {
        let top = margin + header
        let column = (pageWidth - margin * 2 - gap) / 2
        let row = (pageHeight - margin - top - gap) / 2
        var result: [ReportCell] = []
        for r in 0..<2 {
            for c in 0..<2 {
                result.append(ReportCell(
                    x: margin + CGFloat(c) * (column + gap),
                    y: top + CGFloat(r) * (row + gap),
                    width: column,
                    height: row,
                    imageHeight: row - caption
                ))
            }
        }
        return result
    }
}

struct AuthResult {
    let ok: Bool
    let message: String
    let token: String?
    let mobile: String?
}

enum AuthClient {
    static func register(server: String, mobile: String, password: String) async -> AuthResult {
        await post(server: server, path: "register.php", mobile: mobile, password: password)
    }

    static func login(server: String, mobile: String, password: String) async -> AuthResult {
        await post(server: server, path: "login.php", mobile: mobile, password: password)
    }

    private static func post(server: String, path: String, mobile: String, password: String) async -> AuthResult {
        let base = server.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard let url = URL(string: "\(base)/api/\(path)") else {
            return AuthResult(ok: false, message: "Enter a valid hosting address.", token: nil, mobile: nil)
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: String] = ["mobile": AccountRules.digits(mobile), "password": password]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
            let code = (response as? HTTPURLResponse)?.statusCode ?? 500
            let ok = (json["ok"] as? Bool ?? false) && (200...299).contains(code)
            return AuthResult(
                ok: ok,
                message: (json["message"] as? String) ?? "Could not reach the hosting website.",
                token: json["token"] as? String,
                mobile: json["mobile"] as? String
            )
        } catch {
            return AuthResult(ok: false, message: "Could not reach the hosting website.", token: nil, mobile: nil)
        }
    }
}
