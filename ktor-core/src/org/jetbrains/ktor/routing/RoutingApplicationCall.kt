package org.jetbrains.ktor.routing

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.request.*
import org.jetbrains.ktor.response.*
import org.jetbrains.ktor.util.*

/**
 * Represents an application call being handled by [Routing]
 */
class RoutingApplicationCall(private val call: ApplicationCall,
                             val route: Route,
                             receivePipeline: ApplicationReceivePipeline,
                             responsePipeline: ApplicationSendPipeline,
                             resolvedValues: Parameters) : ApplicationCall {

    override val application: Application get() = call.application
    override val attributes: Attributes get() = call.attributes

    override val request = RoutingApplicationRequest(this, receivePipeline, call.request)
    override val response = RoutingApplicationResponse(this, responsePipeline, call.response)

    override val parameters: Parameters by lazy {
        Parameters.build {
            appendAll(call.parameters)
            appendMissing(resolvedValues)
        }
    }

    override fun toString() = "RoutingApplicationCall(route=$route)"
}

class RoutingApplicationRequest(override val call: RoutingApplicationCall,
                                override val pipeline: ApplicationReceivePipeline,
                                request: ApplicationRequest) : ApplicationRequest by request

class RoutingApplicationResponse(override val call: RoutingApplicationCall,
                                 override val pipeline: ApplicationSendPipeline,
                                 response: ApplicationResponse) : ApplicationResponse by response