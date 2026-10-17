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
        let items = Array(photos().reversed())
        guard !items.isEmpty else { return nil }
        let url = folder.appendingPathComponent("GO-GEO-photos.pdf")
        let renderer = UIGraphicsPDFRenderer(bounds: CGRect(x: 0, y: 0, width: ReportLayout.pageWidth, height: ReportLayout.pageHeight))
        let pages = ReportLayout.pageCount(items.count)
        let margin: CGFloat = 28
        let footer: CGFloat = 40
        do {
            try renderer.writePDF(to: url) { context in
                for page in 0..<pages {
                    context.beginPage()
                    let start = page * ReportLayout.perPage
                    let slice = Array(items.dropFirst(start).prefix(ReportLayout.perPage))
                    let contentTop = drawHeading(slice, margin: margin)
                    let cells = ReportLayout.cells(margin: margin, header: max(48, contentTop - margin), footer: footer, caption: 52)
                    for (offset, photo) in slice.enumerated() {
                        let cell = cells[offset]
                        let card = CGRect(x: cell.x, y: cell.y, width: cell.width, height: cell.height)
                        UIColor.white.setFill()
                        UIBezierPath(roundedRect: card, cornerRadius: 8).fill()
                        UIColor(red: 0.84, green: 0.88, blue: 0.93, alpha: 1).setStroke()
                        UIBezierPath(roundedRect: card, cornerRadius: 8).stroke()
                        if let image = UIImage(contentsOfFile: photo.imageURL.path), cell.imageHeight > 8 {
                            image.draw(in: CGRect(x: cell.x + 6, y: cell.y + 6, width: cell.width - 12, height: cell.imageHeight - 8))
                        }
                        let caption = [photo.when, photo.coordinate, photo.details].filter { !$0.isEmpty }.joined(separator: "\n")
                        (caption as NSString).draw(
                            in: CGRect(x: cell.x + 6, y: cell.y + cell.imageHeight + 2, width: cell.width - 12, height: 46),
                            withAttributes: [.font: UIFont.monospacedSystemFont(ofSize: 7, weight: .regular)]
                        )
                    }
                    drawFooter(page: page + 1, pages: pages, margin: margin, footer: footer)
                }
            }
            return url
        } catch {
            return nil
        }
    }

    private static func drawHeading(_ photos: [SavedPhoto], margin: CGFloat) -> CGFloat {
        let title = "GO-GEO"
        (title as NSString).draw(at: CGPoint(x: margin, y: 22), withAttributes: [.font: UIFont.boldSystemFont(ofSize: 20)])
        var y: CGFloat = 52
        let body: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 9), .foregroundColor: UIColor(red: 0.11, green: 0.14, blue: 0.19, alpha: 1)]
        func line(_ label: String, _ values: [String]) {
            let text = values.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
            guard !text.isEmpty else { return }
            let value = "\(label): \(text.joined(separator: "  ·  "))" as NSString
            let width = ReportLayout.pageWidth - margin * 2
            let height = value.boundingRect(
                with: CGSize(width: width, height: 80),
                options: .usesLineFragmentOrigin,
                attributes: body,
                context: nil
            ).height
            value.draw(in: CGRect(x: margin, y: y, width: width, height: height), withAttributes: body)
            y += height + 2
        }
        var clients: [String] = []
        var mobiles: [String] = []
        var landmarks: [String] = []
        var north: [String] = []
        var east: [String] = []
        var south: [String] = []
        var west: [String] = []
        var boundaries: [String] = []
        var notes: [String] = []
        for photo in photos {
            for raw in photo.site.split(separator: "\n") {
                let row = String(raw)
                func take(_ prefix: String, _ into: inout [String]) {
                    if row.hasPrefix(prefix) {
                        let value = String(row.dropFirst(prefix.count)).trimmingCharacters(in: .whitespaces)
                        if !value.isEmpty && !into.contains(value) { into.append(value) }
                    }
                }
                take("Client:", &clients)
                take("Mobile:", &mobiles)
                take("Landmark:", &landmarks)
                take("North:", &north)
                take("East:", &east)
                take("South:", &south)
                take("West:", &west)
                take("Boundary:", &boundaries)
            }
            let note = photo.note.trimmingCharacters(in: .whitespacesAndNewlines)
            if !note.isEmpty && !notes.contains(note) { notes.append(note) }
        }
        line("Client", clients)
        line("Mobile", mobiles)
        line("Landmark", landmarks)
        if [north, east, south, west].contains(where: { !$0.isEmpty }) {
            let sides = [("North", north), ("East", east), ("South", south), ("West", west)]
            let col = (ReportLayout.pageWidth - margin * 2) / 2
            for (index, side) in sides.enumerated() {
                let rect = CGRect(
                    x: margin + CGFloat(index % 2) * col,
                    y: y + CGFloat(index / 2) * 32,
                    width: col - 6,
                    height: 28
                )
                UIColor(red: 0.96, green: 0.97, blue: 0.98, alpha: 1).setFill()
                UIBezierPath(roundedRect: rect, cornerRadius: 4).fill()
                ("\(side.0): \(side.1.joined(separator: " · "))" as NSString).draw(
                    in: rect.insetBy(dx: 6, dy: 6),
                    withAttributes: body
                )
            }
            y += 68
        }
        line("Boundary", boundaries)
        line("Note", notes)
        let lineY = max(y, 58)
        UIColor(red: 0.84, green: 0.88, blue: 0.93, alpha: 1).setStroke()
        let rule = UIBezierPath()
        rule.move(to: CGPoint(x: margin, y: lineY))
        rule.addLine(to: CGPoint(x: ReportLayout.pageWidth - margin, y: lineY))
        rule.stroke()
        return lineY + 8
    }

    private static func drawFooter(page: Int, pages: Int, margin: CGFloat, footer: CGFloat) {
        let top = ReportLayout.pageHeight - footer
        UIColor(red: 0.84, green: 0.88, blue: 0.93, alpha: 1).setStroke()
        let rule = UIBezierPath()
        rule.move(to: CGPoint(x: margin, y: top))
        rule.addLine(to: CGPoint(x: ReportLayout.pageWidth - margin, y: top))
        rule.stroke()
        let mark = "NT™"
        (mark as NSString).draw(at: CGPoint(x: margin, y: top + 12), withAttributes: [.font: UIFont.boldSystemFont(ofSize: 12), .foregroundColor: UIColor(red: 0.66, green: 0.52, blue: 0.18, alpha: 1)])
        let pageLabel = "Page \(page) / \(pages)" as NSString
        let width = pageLabel.size(withAttributes: [.font: UIFont.boldSystemFont(ofSize: 10)]).width
        pageLabel.draw(at: CGPoint(x: ReportLayout.pageWidth - margin - width, y: top + 14), withAttributes: [.font: UIFont.boldSystemFont(ofSize: 10)])
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

    static func cells(margin: CGFloat = 28, header: CGFloat = 120, footer: CGFloat = 40, gap: CGFloat = 10, caption: CGFloat = 52) -> [ReportCell] {
        let top = margin + header
        let gridBottom = pageHeight - margin - footer
        let column = (pageWidth - margin * 2 - gap) / 2
        let row = max(0, (gridBottom - top - gap) / 2)
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
