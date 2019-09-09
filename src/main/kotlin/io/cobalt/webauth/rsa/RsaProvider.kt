package io.cobalt.webauth.rsa

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

fun getPublicKey(key: String): PublicKey {
    val byteKey = Base64.getDecoder().decode(key.toByteArray())
    val x509publicKey = X509EncodedKeySpec(byteKey)
    val kf = KeyFactory.getInstance("RSA")

    return kf.generatePublic(x509publicKey)
}


//TODO Anton cleanUp
fun getPrivateKey(key: String): PrivateKey {
    val byteKey = Base64.getDecoder().decode(key.toByteArray())
    val x509publicKey = X509EncodedKeySpec(byteKey)
    val kf = KeyFactory.getInstance("RSA")

    return kf.generatePrivate(x509publicKey)
}

fun encrypt(plainText: String, publicKey: PublicKey): ByteArray {
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-512ANDMGF1PADDING")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    return cipher.doFinal(plainText.toByteArray())
}

fun decrypt(cipherTextArray: ByteArray, privateKey: PrivateKey): String {
    val cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-512ANDMGF1PADDING")

    cipher.init(Cipher.DECRYPT_MODE, privateKey)

    val decryptedTextArray = cipher.doFinal(cipherTextArray)

    return String(decryptedTextArray)
}