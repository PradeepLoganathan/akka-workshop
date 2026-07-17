package com.pradeepl.akkakata.api;

import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;

import com.pradeepl.akkakata.domain.agents.SupportAgent;

@HttpEndpoint("/api")
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
public class SupportEndpoint {

    public record Question(String question) {}
    public record Answer(String sessionId, String answer) {}

    private final ComponentClient client;

    public SupportEndpoint(ComponentClient client) {
        this.client = client;
    }

    @Post("/support/{sessionId}")
    public Answer ask(String sessionId, Question req) {
        String answer = client.forAgent()
            .inSession(sessionId)
            .method(SupportAgent::ask)
            .invoke(req.question());
        return new Answer(sessionId, answer);
    }
}
