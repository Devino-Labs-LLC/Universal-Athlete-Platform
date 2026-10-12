package com.devinolabs.uap.aicoach.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.aicoach.application.AiCoachService;
import com.devinolabs.uap.aicoach.application.CoachContext;
import com.devinolabs.uap.aicoach.application.CoachConversationSnapshot;
import com.devinolabs.uap.aicoach.application.CoachReply;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/coach")
class CoachController {

	private final AiCoachService coachService;

	CoachController(AiCoachService coachService) {
		this.coachService = coachService;
	}

	@GetMapping("/summary")
	CoachSummaryResponse summary(Authentication authentication) {
		CoachContext context = coachService.summary(accountId(authentication));
		return new CoachSummaryResponse(
				coachService.aiEnabled(),
				context.asOf().toString(),
				context.deterministicFacts().stream().map(CoachController::fact).toList());
	}

	@PostMapping("/conversations")
	@ResponseStatus(HttpStatus.CREATED)
	ConversationCreated start(Authentication authentication) {
		return new ConversationCreated(coachService.startConversation(accountId(authentication)));
	}

	@GetMapping("/conversations/current")
	CoachConversationResponse current(Authentication authentication) {
		CoachConversationSnapshot snapshot = coachService.currentConversation(accountId(authentication));
		if (snapshot == null) {
			return new CoachConversationResponse(null, null, List.of());
		}
		return toResponse(snapshot);
	}

	@DeleteMapping("/conversations/{conversationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID conversationId, Authentication authentication) {
		coachService.deleteConversation(accountId(authentication), conversationId);
	}

	@DeleteMapping("/conversations")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void clear(Authentication authentication) {
		coachService.clearConversations(accountId(authentication));
	}

	@PostMapping("/conversations/{conversationId}/messages")
	CoachReplyResponse message(
			@PathVariable UUID conversationId,
			@Valid @RequestBody CoachMessageRequest request,
			Authentication authentication) {
		CoachReply reply = coachService.reply(accountId(authentication), conversationId, request.text());
		return new CoachReplyResponse(
				reply.conversationId(),
				reply.messageId(),
				reply.contentKind(),
				reply.text(),
				reply.forecast(),
				reply.uncertainty(),
				reply.contributingSignals(),
				reply.deterministicFacts());
	}

	private static CoachConversationResponse toResponse(CoachConversationSnapshot snapshot) {
		return new CoachConversationResponse(
				snapshot.id(),
				snapshot.updatedAt(),
				snapshot.messages().stream()
						.map(message -> new CoachConversationResponse.Message(
								message.id(),
								message.role(),
								message.contentKind(),
								message.text(),
								message.forecast(),
								message.uncertainty(),
								message.createdAt()))
						.toList());
	}

	private static CoachSummaryResponse.CoachFactLine fact(String line) {
		String[] parts = line.split(" \\| ", 4);
		if (parts.length < 4) {
			return new CoachSummaryResponse.CoachFactLine("Context", "UNAVAILABLE", false, line);
		}
		return new CoachSummaryResponse.CoachFactLine(
				parts[0],
				parts[1],
				"STALE".equals(parts[2]),
				parts[3]);
	}

	private static UUID accountId(Authentication authentication) {
		AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
		return principal.accountUuid();
	}

	record ConversationCreated(UUID id) {
	}

}
