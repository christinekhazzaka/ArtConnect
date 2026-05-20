package com.project.artconnect.service.impl;

import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.model.Review;
import com.project.artconnect.service.CommunityService;

import java.util.List;
import java.util.Optional;

public class JdbcCommunityService implements CommunityService {
    private final CommunityMemberDao memberDao;

    public JdbcCommunityService(CommunityMemberDao memberDao) {
        this.memberDao = memberDao;
    }

    @Override
    public List<CommunityMember> getAllMembers() {
        return memberDao.findAll();
    }

    @Override
    public Optional<CommunityMember> getMemberByName(String name) {
        return memberDao.findByName(name);
    }

    @Override
    public void save(CommunityMember member) {
        memberDao.save(member);
    }

    @Override
    public void update(CommunityMember member) {
        if (memberDao.findByName(member.getName()).isPresent()) {
            memberDao.update(member);
        } else {
            memberDao.save(member);
        }
    }

    @Override
    public void delete(String memberName) {
        memberDao.delete(memberName);
    }

    @Override
    public List<Review> getReviewsByMember(CommunityMember member) {
        return member == null ? List.of() : member.getReviews();
    }
}
