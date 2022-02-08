package com.example

import java.nio.charset.Charset
import java.security.MessageDigest



const val ComputationLoops = 100
object Computation {
    fun exec(string: String) {
        var hash = string
        repeat(ComputationLoops) {
            hash += md5(hash)
        }
    }
    fun md5(string: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(string.toByteArray(Charset.defaultCharset()))
        val digest = md.digest()
        return String(digest)
    }
}