import SwiftUI

@main
struct iOSApp: App {
    init() {
        AppSetupKt.setupApp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}