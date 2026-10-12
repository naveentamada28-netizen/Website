import CoreLocation
import Foundation
import UIKit

struct SavedPhoto: Identifiable {
    let id: String
    let imageURL: URL
    let when: String
    let coordinate: String
    let details: String
    let note: String
}

enum PhotoLibrary {
    static var folder: URL {
        let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("GeoCamera", isDirectory: true)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }

    static func save(image: UIImage, location: CLLocation?, note: String) throws -> SavedPhoto {
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
        let meta: [String: String] = [
            "when": when,
            "coordinate": coordinate,
            "details": details,
            "note": note
        ]
        try JSONSerialization.data(withJSONObject: meta).write(to: metaURL)
        return SavedPhoto(id: imageURL.lastPathComponent, imageURL: imageURL, when: when, coordinate: coordinate, details: details, note: note)
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
                        let caption = [photo.when, photo.coordinate, photo.details, photo.note]
                            .filter { !$0.isEmpty }
                            .joined(separator: "\n")
                        (caption as NSString).draw(
                            in: CGRect(x: cell.x + 8, y: cell.y + cell.imageHeight + 4, width: cell.width - 16, height: 70),
                            withAttributes: [.font: UIFont.monospacedSystemFont(ofSize: 8, weight: .regular)]
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
