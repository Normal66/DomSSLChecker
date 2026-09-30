package com.domsslchecker.net.tls

data class TlsCertInfo(
    val position: Int,
    val subject: String,
    val issuer: String,
    val notBeforeUtc: Long,
    val notAfterUtc: Long,
    val serialNumber: String,
    val sha256Fingerprint: String,
)

data class TlsProbeResult(
    val host: String,
    val port: Int,
    val serverHost: String,
    val certs: List<TlsCertInfo>,
    val validationError: String?,
)
