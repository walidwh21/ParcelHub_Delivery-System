package com.parcelhub.logistics.mapper;

import com.parcelhub.logistics.dto.request.RiderRegistrationRequest;
import com.parcelhub.logistics.entity.Rider;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface RiderRegistrationMapper {

	Rider toEntity(RiderRegistrationRequest riderRegistrationRequestDTO);
}
