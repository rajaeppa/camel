package org.apache.camel.component.cxf.jaxrs;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxrs.testbean.Customer;
import org.apache.camel.component.cxf.jaxrs.testbean.CustomerService;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.AbstractJAXRSFactoryBean;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.message.MessageContentsList;
import org.junit.Test;

import javax.ws.rs.HttpMethod;

public class CxfRsProducerEndpointConfigurerTest extends CamelTestSupport {

    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                CxfRsEndpoint endpoint = new CxfRsEndpoint();
                endpoint.setAddress("http://localhost:8000");
                endpoint.setCamelContext(context);
                endpoint.setResourceClasses(CustomerService.class);
                endpoint.setEndpointUriIfNotSpecified("cxfrs:simple");
                endpoint.setCxfRsEndpointConfigurer(new MyCxfRsEndpointConfigurer());

                from("direct:start")
                        .to(endpoint)
                        .to("mock:end");

                from("jetty:http://localhost:8000?matchOnUriPrefix=true")
                        .to("mock:result")
                        .process(exchange -> exchange.getIn().setBody(new Customer()));
            }
        };
    }

    @Test
    public void testCxfRsEndpoinConfigurerProxyApi() throws InterruptedException {
        template.send("direct:start", exchange -> {
            exchange.setPattern(ExchangePattern.InOut);
            Message inMessage = exchange.getIn();
            inMessage.setHeader(CxfConstants.OPERATION_NAME, "getCustomer");
            inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, Boolean.FALSE);
            MessageContentsList messageContentsList = new MessageContentsList();
            messageContentsList.add("1");
            inMessage.setBody(messageContentsList);
        });
        getMockEndpoint("mock:result").expectedHeaderReceived("foo", "bar");
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCxfRsEndpointConfigurerHttpApi() throws InterruptedException {
        template.send("direct:start", exchange -> {
            exchange.setPattern(ExchangePattern.InOut);
            Message inMessage = exchange.getIn();
            inMessage.setHeader(Exchange.HTTP_PATH, "/customerservice/customers/1");
            inMessage.setHeader(Exchange.HTTP_METHOD, HttpMethod.GET);
            inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, Boolean.TRUE);
            inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS, Customer.class);
        });
        getMockEndpoint("mock:result").expectedHeaderReceived("foo", "bar");
        assertMockEndpointsSatisfied();
    }

    public static class MyCxfRsEndpointConfigurer implements CxfRsEndpointConfigurer {

        @Override
        public void configure(AbstractJAXRSFactoryBean factoryBean) {
        }

        @Override
        public void configureClient(Client client) {
            client.header("foo", "bar");
        }

        @Override
        public void configureServer(Server server) {
        }
    }

}
