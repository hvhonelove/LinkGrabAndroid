package com.example.linkgrab.data.engine

/** Deliberately conservative: never flood GitHub when a device is offline or the CDN fails. */
object EngineUpdatePolicy {
    const val DAY_MS = 24L * 60 * 60 * 1000
    const val RETRY_AFTER_FAILURE_MS = 60L * 60 * 1000

    fun isDue(now: Long, lastCheck: Long, lastSuccessful: Long): Boolean {
        val effectiveCheck = maxOf(lastCheck, lastSuccessful)
        if (effectiveCheck <= 0L || effectiveCheck > now) return true
        val interval = if (lastSuccessful >= effectiveCheck) DAY_MS else RETRY_AFTER_FAILURE_MS
        return now - effectiveCheck >= interval
    }

    fun isRecoverableExtractorFailure(raw: String): Boolean {
        val error = raw.lowercase()
        return listOf(
            "unsupported url", "unable to extract", "failed to extract", "extractor error",
            "requested format", "no video formats", "no formats found", "unable to download",
            "signature extraction failed", "unable to parse", "http error 403", "http error 429"
        ).any { it in error }
    }
}
