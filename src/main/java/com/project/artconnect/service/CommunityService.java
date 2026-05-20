package com.project.artconnect.service;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Review;
import java.util.List;
import java.util.Optional;

public interface CommunityService {
    List<CommunityMember> getAllMembers();

    Optional<CommunityMember> getMemberByName(String name);

    void save(CommunityMember member);

    void update(CommunityMember member);

    void delete(String memberName);

    List<Review> getReviewsByMember(CommunityMember member);
}
