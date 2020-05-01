package com.stripe.android

internal data class JsonResourceRequest(
    internal val resourceName: String
) : StripeRequest() {
    override val method: Method = Method.GET
    override val baseUrl: String = "$HOST/$resourceName.json"
    override val mimeType: MimeType = MimeType.Json
    override val params: Map<String, Any> = emptyMap()
    override val headersFactory: RequestHeadersFactory = RequestHeadersFactory.Default()

    internal companion object {
        internal const val HOST = "https://dyqiu3dgtk0l0.cloudfront.net"
    }
}