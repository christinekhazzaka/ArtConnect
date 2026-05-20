package com.project.artconnect.util;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.dao.ArtworkDao;
import com.project.artconnect.dao.CommunityMemberDao;
import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.dao.WorkshopDao;
import com.project.artconnect.persistence.JdbcArtistDao;
import com.project.artconnect.persistence.JdbcArtworkDao;
import com.project.artconnect.persistence.JdbcCommunityMemberDao;
import com.project.artconnect.persistence.JdbcExhibitionDao;
import com.project.artconnect.persistence.JdbcGalleryDao;
import com.project.artconnect.persistence.JdbcWorkshopDao;
import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;

public class ServiceProvider {

    private static final ArtistDao artistDao = new JdbcArtistDao();
    private static final ArtworkDao artworkDao = new JdbcArtworkDao();
    private static final GalleryDao galleryDao = new JdbcGalleryDao();
    private static final WorkshopDao workshopDao = new JdbcWorkshopDao();
    private static final CommunityMemberDao communityMemberDao = new JdbcCommunityMemberDao();
    private static final JdbcExhibitionDao exhibitionDao = new JdbcExhibitionDao();

    private static final ArtistService artistService = new JdbcArtistService(artistDao);
    private static final ArtworkService artworkService = new JdbcArtworkService(artworkDao);
    private static final GalleryService galleryService = new JdbcGalleryService(galleryDao);
    private static final ExhibitionService exhibitionService = new JdbcExhibitionService(exhibitionDao);
    private static final WorkshopService workshopService = new JdbcWorkshopService(workshopDao);
    private static final CommunityService communityService = new JdbcCommunityService(communityMemberDao);

    public static ArtistService getArtistService() {
        return artistService;
    }

    public static ArtworkService getArtworkService() {
        return artworkService;
    }

    public static GalleryService getGalleryService() {
        return galleryService;
    }

    public static ExhibitionService getExhibitionService() {
        return exhibitionService;
    }

    public static WorkshopService getWorkshopService() {
        return workshopService;
    }

    public static CommunityService getCommunityService() {
        return communityService;
    }
}
