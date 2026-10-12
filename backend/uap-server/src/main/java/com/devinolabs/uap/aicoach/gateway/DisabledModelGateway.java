package com.devinolabs.uap.aicoach.gateway;

/**
 * Used while AI is disabled. Completions fail closed and do not contact a provider.
 */
public final class DisabledModelGateway implements ModelGateway {

	@Override
	public ModelCompletionResponse complete(ModelCompletionRequest request) {
		throw new AiProviderException(AiProviderException.Kind.UNAVAILABLE);
	}

}
