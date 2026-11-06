package com.vrris.pacs.dto.response;

/**
 * Mot file PDF da dung xong. Khong import model (luat ArchUnit L2).
 *
 * @param fileName    ten file goi y cho Content-Disposition
 * @param contentType luon la application/pdf trong pham vi nay
 * @param content     noi dung file
 */
public record PrintDocumentResponse(String fileName, String contentType, byte[] content) {
}
