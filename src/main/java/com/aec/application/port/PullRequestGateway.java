package com.aec.application.port;

import com.aec.domain.PullRequestData;

public interface PullRequestGateway {
    PullRequestData fetch(String owner, String repo, int number);
}
