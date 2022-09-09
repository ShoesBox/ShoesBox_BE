package com.shoesbox.domain.friend;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findAllByToMemberIdAndFriendState(long toMemberId, FriendState friendState);

    Optional<Friend> findByFromMemberIdAndToMemberIdAndFriendState(long fromMemberId, long toMemberId, FriendState friendState);
    Friend findByFromMemberIdAndToMemberId(long fromMemberId, long toMemberId);

    boolean existsByFromMemberIdAndToMemberId(long fromMember, long toMember);
}
