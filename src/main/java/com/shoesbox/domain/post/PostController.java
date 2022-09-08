package com.shoesbox.domain.post;

import com.shoesbox.domain.post.dto.PostRequestDto;
import com.shoesbox.global.common.ResponseHandler;
import com.shoesbox.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    // 생성
    @PostMapping
    public ResponseEntity<Object> createPost(PostRequestDto postRequestDto) {
        long memberId = SecurityUtil.getCurrentMemberIdByLong();
        String nickname = SecurityUtil.getCurrentMemberNickname();
        return ResponseHandler.ok(postService.createPost(nickname, memberId, postRequestDto));
    }

    // 전체 조회
    @GetMapping
    public ResponseEntity<Object> getAllPost(
            @PageableDefault(size = 31, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseHandler.ok(postService.getPostList(pageable));
    }

    // 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<Object> getPost(@PathVariable long postId) {
        long memberId = SecurityUtil.getCurrentMemberIdByLong();
        return ResponseHandler.ok(postService.getPost(memberId, postId));
    }

    // 수정
    @PutMapping("/{postId}")
    public ResponseEntity<Object> updatePost(@PathVariable long postId, @RequestBody PostRequestDto postRequestDto) {
        long memberId = SecurityUtil.getCurrentMemberIdByLong();
        return ResponseHandler.ok(postService.updatePost(memberId, postId, postRequestDto));
    }

    // 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Object> deletePost(@PathVariable long postId) {
        long memberId = SecurityUtil.getCurrentMemberIdByLong();
        return ResponseHandler.ok(postService.deletePost(memberId, postId));
    }

}
