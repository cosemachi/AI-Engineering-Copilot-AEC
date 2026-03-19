package com.aec.application.request;

public record ReviewPullRequestCommand(String owner, String repo, int number) {
}
