package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolver;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_HTTP_CLIENT_REQUEST_TIMEOUT;

public class JettyHttpMessageSenderProvider implements ProtocolMessageSenderProvider {

    private final HttpClient httpClient;
    private final EndpointAddressResolver endpointAddressResolver;
    private final int requestTimeout;
    private final MetadataAppender<Request> metadataAppender;
    private final HttpAuthorizationProviderFactory authorizationProviderFactory;

    @Inject
    public JettyHttpMessageSenderProvider(
            HttpClient httpClient,
            ConfigFactory configFactory,
            EndpointAddressResolver endpointAddressResolver,
            MetadataAppender<Request> metadataAppender,
            HttpAuthorizationProviderFactory authorizationProviderFactory) {
        this.httpClient = httpClient;
        this.endpointAddressResolver = endpointAddressResolver;
        this.requestTimeout = configFactory.getIntProperty(CONSUMER_HTTP_CLIENT_REQUEST_TIMEOUT);
        this.metadataAppender = metadataAppender;
        this.authorizationProviderFactory = authorizationProviderFactory;
    }


    @Override
    public MessageSender create(EndpointAddress endpoint) {
        ResolvableEndpointAddress resolvableEndpoint = new ResolvableEndpointAddress(endpoint, endpointAddressResolver);
        return new JettyMessageSender(httpClient,
                resolvableEndpoint,
                authorizationProviderFactory.create(endpoint),
                requestTimeout,
                metadataAppender);
    }

    @Override
    public void start() throws Exception {
        if (httpClient.isStopped()) {
            httpClient.start();
        }
    }

    @Override
    public void stop() throws Exception {
        if (httpClient.isRunning()) {
            httpClient.stop();
        }
    }
}
