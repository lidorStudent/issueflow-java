package com.att.tdp.issueflow.common.audit;

// Marks a block of code as "this is the system, not a user", so AuditService writes
// actor=SYSTEM no matter what is in the SecurityContext.
// Used by background work like auto assign and auto escalate, where there is no user.
public final class AuditContext {

  private static final ThreadLocal<String> ACTOR_OVERRIDE = new ThreadLocal<>();

  private AuditContext() {}

  // Runs action with the actor flagged as SYSTEM. The override is cleared in finally so an
  // exception cannot leak the flag into the next request on a pooled thread.
  public static void runAsSystem(Runnable action) {
    ACTOR_OVERRIDE.set("SYSTEM");
    try {
      action.run();
    } finally {
      ACTOR_OVERRIDE.remove();
    }
  }

  public static String currentActorOverride() {
    return ACTOR_OVERRIDE.get();
  }
}
