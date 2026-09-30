package com.domsslchecker.data.db

enum class DomainExpirySource {
    AUTO,
    MANUAL,
}

enum class UpdateStatus {
    OK,
    ERROR,
    NEEDS_CONFIRMATION,
}

enum class SslStatus {
    OK,
    WARN,
    ERROR,
}

/** Порядок списка доменов на главном экране. */
enum class DomainListSort {
    BY_NAME,
    BY_DOMAIN_EXPIRY,
}

