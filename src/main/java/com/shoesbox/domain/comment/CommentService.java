package com.shoesbox.domain.comment;

import com.shoesbox.domain.comment.dto.CommentRequestDto;
import com.shoesbox.domain.comment.dto.CommentResponseDto;
import com.shoesbox.domain.friend.FriendRepository;
import com.shoesbox.domain.friend.FriendState;
import com.shoesbox.domain.member.Member;
import com.shoesbox.domain.member.MemberRepository;
import com.shoesbox.domain.post.Post;
import com.shoesbox.domain.post.PostRepository;
import com.shoesbox.global.exception.runtime.EntityNotFoundException;
import com.shoesbox.global.exception.runtime.UnAuthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

import static com.shoesbox.domain.sse.SseController.sseEmitters;


@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final FriendRepository friendRepository;

    @Transactional
    public CommentResponseDto createComment(String content, long currentMemberId, long postId) {
        Post post = getPost(postId);
        checkAuthorization(currentMemberId, post.getMemberId());
        Member currentMember = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new EntityNotFoundException(
                        Member.class.getPackageName()));
        Comment comment = Comment.builder()
                .content(content)
                .member(currentMember)
                .post(post)
                .build();
        commentRepository.save(comment);
        notifyAddCommentEvent(postId, currentMemberId);
        return toCommentResponseDto(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> readComments(long postId, long currentMemberId) {
        Post post = getPost(postId);
        checkAuthorization(currentMemberId, post.getMemberId());
        return post.getComments().stream().map(this::toCommentResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public CommentResponseDto updateComment(long currentMemberId, long commentId, CommentRequestDto commentRequestDto) {
        Comment comment = getComment(commentId);
        checkSelfAuthorization(currentMemberId, comment.getMemberId());
        comment.update(commentRequestDto.getContent());
        return toCommentResponseDto(comment);
    }

    @Transactional
    public String deleteComment(long currentMemberId, long commentId) {
        Comment comment = getComment(commentId);
        checkSelfAuthorization(currentMemberId, comment.getMemberId());
        commentRepository.delete(comment);
        return "commentId: " + commentId + "삭제 성공";
    }

    private Comment getComment(long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class.getPackageName()));
    }

    private Post getPost(long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(Post.class.getPackageName()));
    }

    private CommentResponseDto toCommentResponseDto(Comment comment) {
        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .profileImageUrl(comment.getMember().getProfileImageUrl())
                .nickname(comment.getMember().getNickname())
                .memberId(comment.getMember().getId())
                .postId(comment.getPost().getId())
                .createdAt(comment.getCreatedAt())
                .modifiedAt(comment.getModifiedAt())
                .build();
    }

    private void checkAuthorization(long currentMemberId, long targetId) {
        if (currentMemberId != targetId && !isFriend(currentMemberId, targetId)) {
            throw new UnAuthorizedException("접근 권한이 없습니다.");
        }
    }

    private void checkSelfAuthorization(long currentMemberId, long targetId) {
        if (currentMemberId != targetId) {
            throw new UnAuthorizedException("접근 권한이 없습니다.");
        }
    }

    private boolean isFriend(long currentMemberId, long targetId) {
        return currentMemberId == targetId
                || friendRepository.existsByFromMemberIdAndToMemberIdAndFriendState(targetId, currentMemberId,
                FriendState.FRIEND)
                || friendRepository.existsByFromMemberIdAndToMemberIdAndFriendState(currentMemberId, targetId,
                FriendState.FRIEND);
    }

    public void notifyAddCommentEvent(long postId, long currentId) {

        // 댓글에 대한 처리 후 해당 댓글이 달린 게시글의 pk값으로 게시글을 조회
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new IllegalArgumentException("알림을 보낼 게시물을 찾을 수 없습니다.")
        );

        long memberId = post.getMemberId();

        // 친구관계 검증
        if (!isFriend(currentId, memberId)) {
            throw new IllegalArgumentException("친구관계가 아닌 경우 알림을 보낼 수 없습니다.");
        }

        // 로그인 한 사용자에게 알림 발송
        if (sseEmitters.containsKey(memberId) && currentId != memberId) {
            SseEmitter sseEmitter = sseEmitters.get(memberId);
            try {
                sseEmitter.send(SseEmitter.event().name("addComment").data(postId + "\n\n"), MediaType.APPLICATION_JSON);
            } catch (Exception e) {
                sseEmitters.remove(memberId);
            }
        } // todo : 접속 중이 아닌 유저의 경우 db에 저장 후 차후 알림

    }

}
