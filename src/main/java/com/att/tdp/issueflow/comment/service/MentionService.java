package com.att.tdp.issueflow.comment.service;

import com.att.tdp.issueflow.common.util.MentionParser;
import com.att.tdp.issueflow.user.entity.User;
import com.att.tdp.issueflow.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

// Turns @handles in a comment body into User rows.
// Handles are lower cased so the DB lookup is case insensitive.
// Unknown handles are just ignored, a typo does not break the comment.
@Service
public class MentionService {

  private final UserRepository userRepository;

  public MentionService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public Set<User> resolve(String content) {
    Set<String> usernames = MentionParser.extractUsernames(content);
    if (usernames.isEmpty()) {
      return Set.of();
    }
    List<User> mentioned = userRepository.findActiveByUsernameInIgnoreCase(usernames);
    return new HashSet<>(mentioned);
  }
}
