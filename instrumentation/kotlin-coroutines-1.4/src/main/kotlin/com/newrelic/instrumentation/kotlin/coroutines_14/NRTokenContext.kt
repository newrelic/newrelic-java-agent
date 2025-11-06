package com.newrelic.instrumentation.kotlin.coroutines_14

import kotlin.coroutines.*
import com.newrelic.api.agent.Token
import org.jetbrains.annotations.Nullable

data class TokenContext(@Nullable var token: Token)

class TokenContextElement(val context: TokenContext) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<TokenContextElement>
}

fun CoroutineContext.getTokenContext(): TokenContext? = this[TokenContextElement]?.context

fun CoroutineContext.getTokenContextOrNull(): TokenContext? = this[TokenContextElement]?.context

fun addTokenContext(context : CoroutineContext, @Nullable token : Token) : CoroutineContext {
    val tokenContext  = TokenContext(token)
    return context + TokenContextElement(tokenContext)
}

fun removeTokenContext(context : CoroutineContext) : CoroutineContext {
    val tokenContext = context.getTokenContextOrNull();
    if (tokenContext != null) {
        @Nullable var token = tokenContext.token
        token.expire()

        return context.minusKey(com.newrelic.instrumentation.kotlin.coroutines_14.TokenContextElement.Key)
    }
    return context
}



