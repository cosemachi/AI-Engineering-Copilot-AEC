package com.aec.api;

import com.aec.application.PullRequestReviewService;
import com.aec.application.request.ReviewPullRequestCommand;
import com.aec.domain.PrReviewResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/pr/review")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PullRequestReviewResource {

    private final PullRequestReviewService pullRequestReviewService;

    public PullRequestReviewResource(PullRequestReviewService pullRequestReviewService) {
        this.pullRequestReviewService = pullRequestReviewService;
    }

    @POST
    public PrReviewResult review(PullRequestReviewRequest request) {
        return pullRequestReviewService.review(new ReviewPullRequestCommand(
                request.owner(),
                request.repo(),
                request.number()));
    }

    public record PullRequestReviewRequest(String owner, String repo, int number) {
    }
}
