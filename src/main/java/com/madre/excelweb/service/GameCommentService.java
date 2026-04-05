package com.madre.excelweb.service;

import com.madre.excelweb.api.model.CreateGameCommentRequest;
import com.madre.excelweb.api.model.UpdateGameCommentRequest;
import com.madre.excelweb.api.model.dto.GameCommentDto;
import com.madre.excelweb.api.model.dto.OffsetPageDto;
import com.madre.excelweb.jpa.entity.AppUser;
import com.madre.excelweb.jpa.entity.GameComment;
import com.madre.excelweb.jpa.entity.GameData;
import com.madre.excelweb.jpa.repositories.GameCommentRepository;
import com.madre.excelweb.jpa.repositories.GameRepository;
import com.madre.excelweb.jpa.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameCommentService {
    private final GameCommentRepository gameCommentRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final PublicEntityLookupService publicEntityLookupService;

    public OffsetPageDto<GameCommentDto> getComments(final AppUser appUser,
                                                     final String gameId,
                                                     final Integer limit,
                                                     final Integer offset) {
        final GameData game = ensureGameExists(gameId);
        final Long currentUserId = appUser == null ? null : appUser.getId();
        final int resolvedLimit = clamp(limit, 1, 40, 12);
        final int resolvedOffset = Math.max(0, offset == null ? 0 : offset);
        final List<Long> topLevelIds = gameCommentRepository.findTopLevelCommentIdsByGameId(game.getId(), resolvedLimit + 1, resolvedOffset);
        final boolean hasMore = topLevelIds.size() > resolvedLimit;
        final List<Long> visibleTopLevelIds = topLevelIds.stream().limit(resolvedLimit).toList();
        if (visibleTopLevelIds.isEmpty()) {
            return new OffsetPageDto<>(resolvedOffset, resolvedLimit, false, List.of());
        }

        final List<Long> threadCommentIds = gameCommentRepository.findThreadIdsByRootIds(visibleTopLevelIds);
        final List<GameComment> comments = threadCommentIds.isEmpty()
                ? List.of()
                : gameCommentRepository.findAllByIdInWithAuthorAndParent(threadCommentIds);
        final Map<Long, List<GameComment>> commentsByParentId = comments.stream()
                .filter(comment -> comment.getParentComment() != null && comment.getParentComment().getId() != null)
                .collect(Collectors.groupingBy(comment -> comment.getParentComment().getId()));
        final Map<Long, GameComment> commentsById = comments.stream()
                .collect(Collectors.toMap(GameComment::getId, comment -> comment));
        final List<GameCommentDto> items = visibleTopLevelIds.stream()
                .map(commentsById::get)
                .filter(Objects::nonNull)
                .map(comment -> toThreadDto(comment, commentsByParentId, currentUserId))
                .toList();
        return new OffsetPageDto<>(resolvedOffset, resolvedLimit, hasMore, items);
    }

    public GameCommentDto createComment(final AppUser appUser, final String gameId, final CreateGameCommentRequest request) {
        final AppUser author = requireUser(appUser);
        final GameData game = ensureGameExists(gameId);
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content is required.");
        }

        final String content = request.content().trim();
        if (content.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be at least 2 characters long.");
        }
        if (content.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be 2000 characters or fewer.");
        }

        final GameComment parentComment = resolveParentComment(game, request.parentCommentId());
        final GameComment savedComment = gameCommentRepository.save(GameComment.builder()
                .author(author)
                .game(game)
                .parentComment(parentComment)
                .content(content)
                .build());

        gameRepository.refreshCommentCount(game.getId());
        return GameCommentDto.fromEntity(savedComment, List.of(), author.getId());
    }

    public GameCommentDto updateComment(final AppUser appUser,
                                        final String gameId,
                                        final String commentId,
                                        final UpdateGameCommentRequest request) {
        final AppUser author = requireUser(appUser);
        final GameComment existingComment = requireOwnedComment(author, gameId, commentId);

        if (existingComment.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deleted comments cannot be edited.");
        }
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content is required.");
        }

        final String content = request.content().trim();
        if (content.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be at least 2 characters long.");
        }
        if (content.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be 2000 characters or fewer.");
        }

        existingComment.setContent(content);
        return GameCommentDto.fromEntity(gameCommentRepository.save(existingComment), List.of(), author.getId());
    }

    public void deleteComment(final AppUser appUser, final String gameId, final String commentId) {
        final AppUser author = requireUser(appUser);
        final GameComment existingComment = requireOwnedComment(author, gameId, commentId);

        if (gameCommentRepository.existsByParentComment_Id(existingComment.getId())) {
            existingComment.setDeleted(true);
            existingComment.setContent("");
            gameCommentRepository.save(existingComment);
        } else {
            gameCommentRepository.delete(existingComment);
        }

        gameRepository.refreshCommentCount(existingComment.getGame().getId());
    }

    private AppUser requireUser(final AppUser appUser) {
        if (appUser == null || appUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        }
        return userRepository.getUserById(appUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private GameData ensureGameExists(final String gameId) {
        return publicEntityLookupService.requireGame(gameId);
    }

    private GameCommentDto toThreadDto(final GameComment comment,
                                       final Map<Long, List<GameComment>> commentsByParentId,
                                       final Long currentUserId) {
        final List<GameCommentDto> replies = commentsByParentId.getOrDefault(comment.getId(), List.of()).stream()
                .sorted(replyOrder())
                .map(reply -> toThreadDto(reply, commentsByParentId, currentUserId))
                .toList();

        return GameCommentDto.fromEntity(comment, replies, currentUserId);
    }

    private GameComment resolveParentComment(final GameData game, final String parentCommentId) {
        if (parentCommentId == null || parentCommentId.isBlank()) {
            return null;
        }

        final GameComment parentComment = publicEntityLookupService.requireComment(parentCommentId);
        if (!parentComment.getGame().getId().equals(game.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Replies must target a comment from the same game.");
        }

        return parentComment;
    }

    private GameComment requireOwnedComment(final AppUser appUser, final String gameId, final String commentId) {
        final GameData game = ensureGameExists(gameId);
        final GameComment comment = publicEntityLookupService.requireComment(commentId);
        if (!comment.getGame().getId().equals(game.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment does not belong to this game.");
        }
        if (comment.getAuthor().getId() == null || !comment.getAuthor().getId().equals(appUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own comments.");
        }

        return comment;
    }

    private Comparator<GameComment> threadOrder() {
        return Comparator.comparing(GameComment::getCreatedAt, Comparator.nullsLast(OffsetDateTime::compareTo))
                .thenComparing(GameComment::getId, Comparator.nullsLast(Long::compareTo))
                .reversed();
    }

    private Comparator<GameComment> replyOrder() {
        return Comparator.comparing(GameComment::getCreatedAt, Comparator.nullsLast(OffsetDateTime::compareTo))
                .thenComparing(GameComment::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int clamp(final Integer value, final int min, final int max, final int fallback) {
        final int resolvedValue = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolvedValue));
    }
}
