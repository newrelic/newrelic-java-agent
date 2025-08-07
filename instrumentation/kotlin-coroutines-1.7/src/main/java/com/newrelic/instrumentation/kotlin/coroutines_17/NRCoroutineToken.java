package com.newrelic.instrumentation.kotlin.coroutines_17;

import com.newrelic.api.agent.Token;

import kotlin.coroutines.AbstractCoroutineContextElement;
import kotlin.coroutines.CoroutineContext;

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
		if(this != obj ) {
			if(obj instanceof NRCoroutineToken) {
				NRCoroutineToken t = (NRCoroutineToken)obj;
				return t.token == token;
			}
		} else {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return "NRCoroutineToken";
	}
	
	
}
