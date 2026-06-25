package com.covenantcode.crm.mapper;

import com.covenantcode.crm.dto.lead.LeadCommentResponse;
import com.covenantcode.crm.dto.lead.UserShortResponse;
import com.covenantcode.crm.entity.LeadComment;
import com.covenantcode.crm.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper
public interface LeadCommentMapper {

    @Mapping(target = "leadId", source = "lead.id")
    @Mapping(target = "author", source = "author", qualifiedByName = "toUserShortResponse")
    @Mapping(target = "createdAt", source = "createdAt")
    LeadCommentResponse toResponse(LeadComment leadComment);

    @Named("toUserShortResponse")
    static UserShortResponse toUserShortResponse(User user) {
        if (user == null) {
            return null;
        }

        UserShortResponse response = new UserShortResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }

    default LocalDateTime map(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDateTime() : null;
    }
}
