package gecw.cse.utils

fun waitForPortOpen(port: Int) {
    while (true) {
        try {
            java.net.Socket("localhost", port).close()
            break
        } catch (e: java.net.ConnectException) {
            Thread.sleep(100)
        }
    }
}