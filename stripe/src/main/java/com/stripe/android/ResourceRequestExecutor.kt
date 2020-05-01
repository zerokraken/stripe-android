package com.stripe.android

import java.io.IOException

internal interface ResourceRequestExecutor {
    fun execute(request: JsonResourceRequest): StripeResponse

    class Default internal constructor(
        private val logger: Logger = Logger.noop()
    ) : ResourceRequestExecutor {
        private val connectionFactory = ConnectionFactory.Default()

        override fun execute(request: JsonResourceRequest): StripeResponse {
            return executeInternal(request)
        }

        private fun executeInternal(request: JsonResourceRequest): StripeResponse {
            logger.info(request.toString())

            connectionFactory.create(request).use {
                try {
                    val stripeResponse = it.response
                    logger.info(stripeResponse.toString())
                    return stripeResponse
                } catch (e: IOException) {
                    logger.error("Exception while making Stripe resource request", e)
                    throw e
                }
            }
        }
    }
}
