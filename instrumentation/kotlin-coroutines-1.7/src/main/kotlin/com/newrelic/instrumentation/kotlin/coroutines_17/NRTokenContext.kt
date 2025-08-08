package com.newrelic.instrumentation.kotlin.coroutines_17

import kotlin.coroutines.*
import com.newrelic.api.agent.Token

data class TokenContext(val token: Token)

class TokenContextElement(val context: TokenContext) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<TokenContextElement>
}

fun CoroutineContext.getTokenContext(): TokenContext? = this[TokenContextElement]?.context

fun CoroutineContext.getTokenContextOrNull(): TokenContext? = this[TokenContextElement]?.context

fun addTokenContext(context : CoroutineContext, token : Token) : CoroutineContext {
    val tokenContext  = TokenContext(token)
    return context + TokenContextElement(tokenContext)
}

