package com.domsslchecker.net.tls

import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.io.use

object TlsProber {
    fun probe(
        hostAscii: String,
        port: Int = 443,
    ): TlsProbeResult {
        val sslContext = SSLContext.getInstance("TLS")
        // Default TrustManagers: handshake will validate the chain + hostname (HTTPS).
        sslContext.init(null, null, null)
        val factory: SSLSocketFactory = sslContext.socketFactory

        val raw = factory.createSocket() as SSLSocket
        raw.use { socket ->
            val sslParams = socket.sslParameters
            sslParams.serverNames = listOf(SNIHostName(hostAscii))
            sslParams.endpointIdentificationAlgorithm = "HTTPS"
            socket.sslParameters = sslParams

            socket.connect(InetSocketAddress(hostAscii, port), 12_000)
            // IMPORTANT: do not call custom checkServerTrusted() after handshake:
            // Conscrypt can throw "Not in handshake; no session available".
            socket.startHandshake()

            val chain = socket.session.peerCertificates.map { it as X509Certificate }.toList()

            val certInfos = chain.mapIndexed { index, cert ->
                TlsCertInfo(
                    position = index,
                    subject = cert.subjectX500Principal.name,
                    issuer = cert.issuerX500Principal.name,
                    notBeforeUtc = cert.notBefore.time,
                    notAfterUtc = cert.notAfter.time,
                    serialNumber = cert.serialNumber.toString(),
                    sha256Fingerprint = sha256Hex(cert.encoded),
                )
            }

            return TlsProbeResult(
                host = hostAscii,
                port = port,
                serverHost = hostAscii,
                certs = certInfos,
                validationError = null,
            )
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { b ->
            val v = b.toInt() and 0xFF
            val h = v.toString(16)
            if (v < 0x10) "0$h" else h
        }
    }
}