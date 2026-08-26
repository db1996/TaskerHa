package com.github.db1996.taskerha.util

import android.content.Context
import android.security.KeyChain
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.logging.LogChannel
import okhttp3.OkHttpClient
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager

/**
 * Builds OkHttpClient instances honoring the user's "Use client certificate" setting.
 *
 * When enabled with a saved KeyChain alias, the resulting client presents that
 * client certificate during TLS handshakes. Permission to use the alias was
 * granted by the user once via KeyChain.choosePrivateKeyAlias() in the settings
 * screen, so KeyChain.getPrivateKey/getCertificateChain do not show any UI at
 * runtime — making this safe to use from background tasks while the device is
 * locked.
 */
object HaHttpClientFactory {
    private const val TAG = "HaHttpClientFactory"

    fun build(
        context: Context,
        clientCertEnabled: Boolean = HaSettings.loadClientCertEnabled(context),
        clientCertAlias: String = HaSettings.loadClientCertAlias(context),
        configure: (OkHttpClient.Builder) -> Unit = {}
    ): OkHttpClient {
        val timeoutSeconds = HaSettings.loadRequestTimeout(context).toLong()
        val builder = OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        configure(builder)

        if (clientCertEnabled) {
            if (clientCertAlias.isNotBlank()) {
                applyClientCert(builder, context.applicationContext, clientCertAlias)
            } else {
                CustomLogger.w(
                    TAG,
                    "Client cert enabled but no alias selected; building plain client",
                    LogChannel.GENERAL
                )
            }
        }

        return builder.build()
    }

    private fun applyClientCert(
        builder: OkHttpClient.Builder,
        appContext: Context,
        alias: String
    ) {
        try {
            val trustManager = defaultTrustManager()
            val keyManager = AliasKeyManager(appContext, alias)
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(arrayOf(keyManager), arrayOf(trustManager), null)
            }
            builder.sslSocketFactory(sslContext.socketFactory, trustManager)
        } catch (t: Throwable) {
            CustomLogger.e(
                TAG,
                "Failed to install client certificate (alias=$alias); falling back to plain TLS: ${t.message}",
                LogChannel.GENERAL,
                t
            )
        }
    }

    private fun defaultTrustManager(): X509TrustManager {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        return tmf.trustManagers.firstOrNull { it is X509TrustManager } as? X509TrustManager
            ?: error("No X509TrustManager available")
    }
}

private class AliasKeyManager(
    private val appContext: Context,
    private val alias: String
) : X509ExtendedKeyManager() {

    // Cache successful KeyChain lookups: the first call after a cold keystore
    // binder can transiently throw, silently degrading the handshake to no client
    // cert. Retrying + caching makes the first handshake reliable.
    @Volatile private var cachedChain: Array<X509Certificate>? = null
    @Volatile private var cachedKey: PrivateKey? = null

    override fun chooseClientAlias(
        keyTypes: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?
    ): String = alias

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
        if (alias != this.alias) return null
        cachedChain?.let { return it }
        return retryKeyChain("getCertificateChain") {
            KeyChain.getCertificateChain(appContext, this.alias)
                ?.takeIf { it.isNotEmpty() }
        }?.also { cachedChain = it }
    }

    override fun getPrivateKey(alias: String?): PrivateKey? {
        if (alias != this.alias) return null
        cachedKey?.let { return it }
        return retryKeyChain("getPrivateKey") {
            KeyChain.getPrivateKey(appContext, this.alias)
        }?.also { cachedKey = it }
    }

    /**
     * Retries a KeyChain lookup with a short backoff so a cold-binder failure on
     * the first call doesn't silently degrade to an unauthenticated request.
     */
    private fun <T> retryKeyChain(op: String, block: () -> T?): T? {
        var lastError: Throwable? = null
        repeat(KEYCHAIN_MAX_ATTEMPTS) { attempt ->
            try {
                block()?.let { return it }
            } catch (t: Throwable) {
                lastError = t
                CustomLogger.w(
                    "AliasKeyManager",
                    "$op attempt ${attempt + 1}/$KEYCHAIN_MAX_ATTEMPTS failed for alias=$alias: ${t.message}",
                    LogChannel.GENERAL
                )
            }
            if (attempt < KEYCHAIN_MAX_ATTEMPTS - 1) {
                try {
                    Thread.sleep(KEYCHAIN_RETRY_BASE_MS * (attempt + 1))
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }
        }
        CustomLogger.e(
            "AliasKeyManager",
            "$op failed after $KEYCHAIN_MAX_ATTEMPTS attempts for alias=$alias: ${lastError?.message}",
            LogChannel.GENERAL,
            lastError
        )
        return null
    }

    override fun getClientAliases(
        keyType: String?,
        issuers: Array<out Principal>?
    ): Array<String> = arrayOf(alias)

    override fun chooseServerAlias(
        keyType: String?,
        issuers: Array<out Principal>?,
        socket: Socket?
    ): String? = null

    override fun getServerAliases(
        keyType: String?,
        issuers: Array<out Principal>?
    ): Array<String>? = null

    private companion object {
        const val KEYCHAIN_MAX_ATTEMPTS = 3
        const val KEYCHAIN_RETRY_BASE_MS = 150L
    }
}
