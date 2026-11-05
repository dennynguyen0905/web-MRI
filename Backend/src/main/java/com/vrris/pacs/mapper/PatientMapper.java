package com.vrris.pacs.mapper;

import com.vrris.pacs.dto.request.PatientCreateRequest;
import com.vrris.pacs.dto.request.PatientUpdateRequest;
import com.vrris.pacs.dto.response.PatientResponse;
import com.vrris.pacs.model.Patient;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * patient.dob_precision/patient.gender la String trong DB (model.Patient); enu.DobPrecision/enu.Gender
 * o dto - MapStruct tu sinh valueOf()/name() vi cung ten field va kieu dich la enum (giong cach
 * mapper.DeviceMapper dang lam voi modality).
 */
@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "age", ignore = true)
    @Mapping(target = "ageApproximate", ignore = true)
    PatientResponse toResponse(Patient patient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hisPatientId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Patient toEntity(PatientCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patientCode", ignore = true)
    @Mapping(target = "hisPatientId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntityFromRequest(PatientUpdateRequest request, @MappingTarget Patient patient);
}
