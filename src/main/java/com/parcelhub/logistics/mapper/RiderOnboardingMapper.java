package com.parcelhub.logistics.mapper;

import com.parcelhub.logistics.dto.request.RiderOnboardingRequest;
import com.parcelhub.logistics.entity.Rider;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface RiderOnboardingMapper {

	Rider toEntity(RiderOnboardingRequest riderOnboardingDTO);
}
