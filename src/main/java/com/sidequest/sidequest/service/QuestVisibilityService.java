package com.sidequest.sidequest.service;

import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestAudience;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestVisibilityService {

    private final CircleService circleService;

    public boolean canViewQuest(AppUser currentUser, Quest quest) {
        if (currentUser == null || quest == null) {
            return false;
        }

        if (isAdmin(currentUser) || quest.getCreator().getId().equals(currentUser.getId())) {
            return true;
        }

        if (quest.getAudience() == QuestAudience.EVERYONE) {
            return true;
        }

        return circleService.isCircleBetween(currentUser, quest.getCreator());
    }

    private boolean isAdmin(AppUser user) {
        return user != null && user.getRole() == AppUserRole.ADMIN;
    }
}
