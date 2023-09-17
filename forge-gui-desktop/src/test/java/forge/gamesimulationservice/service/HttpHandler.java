package forge.gamesimulationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HttpHandler<Request, Response> implements com.sun.net.httpserver.HttpHandler {

    private final Class<Request> _requestClass;
    private final TypedHandler<Request, Response> _handler;

    public HttpHandler(TypedHandler<Request, Response> handler, Class<Request> requestClass) {
        _handler = handler;
        _requestClass = requestClass;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if("POST".equals(httpExchange.getRequestMethod())) {
            handlePostRequest(httpExchange);
        }
        else {
            System.out.println("Unsupported Method : " + httpExchange.getRequestMethod());
        }
    }

    private String handlePostRequest(HttpExchange httpExchange) {
        try {
            InputStream requestBody = httpExchange.getRequestBody();
            String jsonString = doHandlePost(requestBody);
            OutputStream outputStream = httpExchange.getResponseBody();
            httpExchange.sendResponseHeaders(200, jsonString.length());
            outputStream.write(jsonString.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    private String doHandlePost(InputStream requestBody) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Request request = objectMapper.readValue(requestBody, _requestClass);
        Response response = _handler.handle(request);
        return objectMapper.writeValueAsString(response);
    }
}

