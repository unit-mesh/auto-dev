package cc.unitmesh.devti.agent.a2a

import io.a2a.client.A2AClient

class A2AClientConsumer {
    fun connect(url: String): A2AClient {
        val client = A2AClient(url)
        return client
    }
}
