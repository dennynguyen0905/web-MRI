package com.vrris.pacs.dto.response;

/**
 * Tra ve deep-link mo OHIF Viewer cho ca chup (IN-19, IN-23, doc/07-pacs-dicom.md muc 6).
 */
public record ViewerLinkResponse(
        Long studyId,
        Long patientId,
        String studyInstanceUid,
        String viewerUrl
) {
}

