import CoreLocation
import Foundation
import UIKit

struct BoundaryPoint: Codable, Hashable {
    var latitude: Double
    var longitude: Double
}

struct SiteRecord: Codable, Equatable {
    var client: String = ""
    var mobile: String = ""
    var landmark: String = ""
    var north: String = ""
    var east: String = ""
    var south: String = ""
    var west: String = ""
    var corners: [BoundaryPoint] = []

    static func digits(_ raw: String) -> String { raw.filter(\.isNumber) }

    static func mobileOk(_ raw: String) -> Bool {
        if raw.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return true }
        let count = digits(raw).count
        return count >= 10 && count <= 15
    }

    var cleanedMobile: String { String(Self.digits(mobile).prefix(15)) }

    var summary: String {
        var parts: [String] = []
        let clientText = client.trimmingCharacters(in: .whitespacesAndNewlines)
        let landmarkText = landmark.trimmingCharacters(in: .whitespacesAndNewlines)
        if !clientText.isEmpty { parts.append(clientText) }
        if !cleanedMobile.isEmpty { parts.append(cleanedMobile) }
        if !landmarkText.isEmpty { parts.append(landmarkText) }
        let hasSides = [north, east, south, west].contains { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        if !corners.isEmpty {
            parts.append("\(corners.count) corners")
        } else if hasSides {
            parts.append("bounds")
        }
        return parts.joined(separator: " · ")
    }

    var caption: String {
        var lines: [String] = []
        func add(_ label: String, _ value: String) {
            let text = value.replacingOccurrences(of: "\n", with: " ").trimmingCharacters(in: .whitespacesAndNewlines)
            if !text.isEmpty { lines.append("\(label): \(text)") }
        }
        add("Client", client)
        if !cleanedMobile.isEmpty { lines.append("Mobile: \(cleanedMobile)") }
        add("Landmark", landmark)
        add("North", north)
        add("East", east)
        add("South", south)
        add("West", west)
        if !corners.isEmpty {
            let path = corners.map { String(format: "%.6f, %.6f", $0.latitude, $0.longitude) }.joined(separator: " → ")
            let closed = corners.count >= 3 ? " (closed)" : ""
            lines.append("Boundary: \(path)\(closed)")
        }
        return lines.joined(separator: "\n")
    }

    static func load() -> SiteRecord {
        guard let data = UserDefaults.standard.data(forKey: "nt.site"),
              let site = try? JSONDecoder().decode(SiteRecord.self, from: data) else {
            return SiteRecord()
        }
        return site
    }

    func save() {
        if let data = try? JSONEncoder().encode(self) {
            UserDefaults.standard.set(data, forKey: "nt.site")
        }
    }
}

struct SavedPhoto: Identifiable {
    let id: String
    let imageURL: URL
    let when: String
    let coordinate: String
    let details: String
    let site: String
    let note: String
}

enum PhotoLibrary {
    static var folder: URL {
        let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("GeoCamera", isDirectory: true)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    static func save(image: UIImage, location: CLLocation?, note: String, site: SiteRecord = SiteRecord()) throws -> SavedPhoto {
        let stamp = ISO8601DateFormatter().string(from: Date()).replacingOccurrences(of: ":", with: "-")
        let base = folder.appendingPathComponent("GEO_\(stamp)")
        let imageURL = base.appendingPathExtension("jpg")
        let metaURL = base.appendingPathExtension("json")
        guard let data = image.jpegData(compressionQuality: 0.92) else {
            throw CocoaError(.fileWriteUnknown)
        }
        try data.write(to: imageURL)
        let when = DateFormatter.localizedString(from: Date(), dateStyle: .medium, timeStyle: .medium)
        let coordinate: String
        let details: String
        if let location {
            coordinate = String(format: "%.6f, %.6f", location.coordinate.latitude, location.coordinate.longitude)
            details = String(format: "Acc ±%.0f m  Alt %.0f m", location.horizontalAccuracy, location.altitude)
        } else {
            coordinate = "GPS unavailable"
            details = ""
        }
        let siteText = site.caption
        let meta: [String: String] = [
            "when": when,
            "coordinate": coordinate,
            "details": details,
            "site": siteText,
            "note": note
        ]
        try JSONSerialization.data(withJSONObject: meta).write(to: metaURL)
        return SavedPhoto(id: imageURL.lastPathComponent, imageURL: imageURL, when: when, coordinate: coordinate, details: details, site: siteText, note: note)
    }

    static func photos() -> [SavedPhoto] {
        let files = (try? FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil)) ?? []
        return files.filter { $0.pathExtension == "jpg" }.sorted { $0.lastPathComponent > $1.lastPathComponent }.compactMap { url in
            let metaURL = url.deletingPathExtension().appendingPathExtension("json")
            let raw = (try? Data(contentsOf: metaURL)).flatMap { try? JSONSerialization.jsonObject(with: $0) } as? [String: String]
            return SavedPhoto(
                id: url.lastPathComponent,
                imageURL: url,
                when: raw?["when"] ?? "",
                coordinate: raw?["coordinate"] ?? "",
                details: raw?["details"] ?? "",
                site: raw?["site"] ?? "",
                note: raw?["note"] ?? ""
            )
        }
    }

    static func makePdf() -> URL? {
        let items = photos().reversed()
        guard !items.isEmpty else { return nil }
        let url = folder.appendingPathComponent("NT-GeoCamera-photos.pdf")
        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: ReportLayout.pageWidth, height: ReportLayout.pageHeight))
        let cells = ReportLayout.cells()
        let pages = ReportLayout.pageCount(items.count)
        do {
            try renderer.writePDF(to: url) { context in
                for page in 0..<pages {
                    context.beginPage()
                    let title = "NT™ Geo Camera    \(page + 1) / \(pages)"
                    (title as NSString).draw(at: CGPoint(x: 28, y: 28), withAttributes: [.font: UIFont.boldSystemFont(ofSize: 16)])
                    ("Trademark of NT  ·  4 photos per page" as NSString).draw(
                        at: CGPoint(x: 28, y: 50),
                        withAttributes: [.font: UIFont.systemFont(ofSize: 10), .foregroundColor: UIColor.darkGray]
                    )
                    let start = page * ReportLayout.perPage
                    let slice = items.dropFirst(start).prefix(ReportLayout.perPage)
                    for (offset, photo) in slice.enumerated() {
                        let cell = cells[offset]
                        let card = CGRect(x: cell.x, y: cell.y, width: cell.width, height: cell.height)
                        UIColor.white.setFill()
                        UIBezierPath(roundedRect: card, cornerRadius: 10).fill()
                        if let image = UIImage(contentsOfFile: photo.imageURL.path) {
                            image.draw(in: CGRect(x: cell.x + 8, y: cell.y + 8, width: cell.width - 16, height: cell.imageHeight - 8))
                        }
                        let caption = [photo.when, photo.coordinate, photo.details, photo.site, photo.note]
                            .filter { !$0.isEmpty }
                            .joined(separator: "\n")
                        (caption as NSString).draw(
                            in: CGRect(x: cell.x + 8, y: cell.y + cell.imageHeight + 4, width: cell.width - 16, height: 168),
                            withAttributes: [.font: UIFont.monospacedSystemFont(ofSize: 7, weight: .regular)]
                        )
                    }
                }
            }
            return url
        } catch {
            return nil
        }
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

    static func cells(margin: CGFloat = 28, header: CGFloat = 64, gap: CGFloat = 12, caption: CGFloat = 180) -> [ReportCell] {
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
