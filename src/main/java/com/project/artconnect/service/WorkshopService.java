package com.project.artconnect.service;

import com.project.artconnect.model.Workshop;
import com.project.artconnect.model.Booking;
import com.project.artconnect.model.CommunityMember;
import java.util.List;
import java.util.Optional;

public interface WorkshopService {
    List<Workshop> getAllWorkshops();

    Optional<Workshop> getWorkshopByTitle(String title);

    void save(Workshop workshop);

    void update(Workshop workshop);

    void delete(String workshopTitle);

    void bookWorkshop(Workshop workshop, CommunityMember member);

    List<Booking> getBookingsByMember(CommunityMember member);
}
