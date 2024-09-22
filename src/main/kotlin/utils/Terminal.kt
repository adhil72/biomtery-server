package gecw.cse.utils

class Terminal {
    companion object {
        fun executeCommand(command: String, async: Boolean, logs: Boolean? = false): Process {
            val process = Runtime.getRuntime().exec(command)

            val outputReader = Thread {
                val output = process.inputStream.bufferedReader().readText()
                println(output)
            }

            val errorReader = Thread {
                if (logs == true) {
                    val error = process.errorStream.bufferedReader().readText()
                    println(error)
                }
            }

            outputReader.start()
            errorReader.start()

            if (!async) {
                outputReader.join()
                errorReader.join()
                process.waitFor()
            } else {
                Thread { process.waitFor() }.start()
            }

            return process
        }

    }
}