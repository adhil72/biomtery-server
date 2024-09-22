package gecw.cse.utils

class Uid {
    companion object {
        fun generate(): String {
            return java.util.UUID.randomUUID().toString()
        }
    }
}