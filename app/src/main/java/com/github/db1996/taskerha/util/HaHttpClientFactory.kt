package com.github.db1996.taskerha.util

import android.content.Context
import android.security.KeyChain
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.logging.LogChannel
import okhttp3.OkHttpClient
import java.net.Socket
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
        val builder = OkHttpClient.Builder()
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

    override fun chooseClientAlias(
        keyTypes: Array<out String>?,
        issuers: Array<out Principal>?,
        socket: Socket?
    ): String = alias

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
        if (alias != this.alias) return null
        return try {
            KeyChain.getCertificateChain(appContext, alias)
        } catch (t: Throwable) {
            CustomLogger.e(
                "AliasKeyManager",
                "getCertificateChain failed for alias=$alias: ${t.message}",
                LogChannel.GENERAL,
                t
            )
            null
        }
    }

    override fun getPrivateKey(alias: String?): PrivateKey? {
        if (alias != this.alias) return null
        return try {
            KeyChain.getPrivateKey(appContext, alias)
        } catch (t: Throwable) {
            CustomLogger.e(
                "AliasKeyManager",
                "getPrivateKey failed for alias=$alias: ${t.message}",
                LogChannel.GENERAL,
                t
            )
            null
        }
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
}
