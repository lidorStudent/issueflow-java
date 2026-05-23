package com.att.tdp.issueflow.comment.dto;

import java.util.List;

public record MentionsPageResponse(List<CommentResponse> data, long total, int page) {}
