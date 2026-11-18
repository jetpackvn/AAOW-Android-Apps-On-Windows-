rootProject.name = "aaow"

include("apk")        // Đọc APK: manifest nhị phân (AXML), resources.arsc
include("framework")  // Cài đặt lại các API android.* chạy trên Windows (Java2D/Swing)
include("launcher")   // Trình khởi chạy: DEX -> JVM bytecode, nạp class, mở Activity
