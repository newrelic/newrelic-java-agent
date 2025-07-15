package com.newrelic.instrumentation.kotlin.coroutines_14;

import com.newrelic.api.agent.Token;

import kotlin.coroutines.AbstractCoroutineContextElement;
import kotlin.coroutines.CoroutineContext;

/*
 *
 * Used to pass the agent async token across the execution of the coroutine
 */
public class NRCoroutineToken extends AbstractCoroutineContextElement
{
    public static Key key = new Key();

    public NRCoroutineToken(Token t) {
        super(key);
        token = t;
    }

    private Token token = null;

    public static final class Key implements CoroutineContext.Key<NRCoroutineToken> {
        private Key() {}
    }

    public Token getToken() {
        return token;
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(!(obj instanceof NRCoroutineToken)) return false;
        NRCoroutineToken other = (NRCoroutineToken) obj;
        return token.equals(other.token);
    }

    @Override
    public String toString() {
        return "NRCoroutineToken";
    }


}
