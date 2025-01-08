package fi.hel.components;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.ProcessorEndpoint;
import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection
@Component("sentry-log")
public class SentryLogComponent extends DefaultComponent {
    private static final Logger LOG = Logger.getLogger(SentryLogComponent.class);

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        String level = (String) parameters.remove("loggingLevel");
        LogProcessor processor = new LogProcessor(remaining, level);
        return new ProcessorEndpoint(uri, this, processor);
    }

    private static class LogProcessor implements Processor {
        private final String message;
        private final String level;

        public LogProcessor(String message, String level) {
            this.message = message;
            this.level = level != null ? level.toUpperCase() : "INFO";
        }

        @Override
        public void process(Exchange exchange) {
            try {
                // First resolve property placeholders (like {{app.name}})
                String resolvedMessage = exchange.getContext().resolvePropertyPlaceholders(message);

                // Then evaluate any remaining Camel expressions
                Expression expression = exchange.getContext().resolveLanguage("simple")
                        .createExpression(resolvedMessage);
                String logMessage = expression.evaluate(exchange, String.class);

                // Get the route ID or use a default value
                String routeId = exchange.getFromRouteId() != null ? exchange.getFromRouteId() : "route";
                Logger routeLogger = Logger.getLogger(routeId);

                switch (level) {
                    case "ERROR":
                        routeLogger.error(logMessage);
                        break;
                    case "WARN":
                        routeLogger.warn(logMessage);
                        break;
                    case "DEBUG":
                        routeLogger.debug(logMessage);
                        break;
                    default:
                    case "INFO":
                        routeLogger.info(logMessage);
                        break;
                }

            } catch (Exception e) {
                LOG.error("Error processing log message: " + message, e);
            }
        }
    }
}