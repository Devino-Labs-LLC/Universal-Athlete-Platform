package com.devinolabs.uap.aicoach.gateway;

/**
 * Provider-neutral inference port. Coach rules must not depend on a vendor SDK.
 */
public interface ModelGateway {

	ModelCompletionResponse complete(ModelCompletionRequest request);

}
