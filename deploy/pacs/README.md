# Hướng dẫn nạp dữ liệu DICOM mẫu vào PACS (Orthanc) cho Demo (IN-19)

Tài liệu này cung cấp các lệnh mẫu dùng **dcm4che CLI** (C-ECHO / C-STORE) và **Orthanc REST API** để nạp 1–2 bộ ảnh DICOM công khai đã ẩn danh (TCIA - The Cancer Imaging Archive) vào Orthanc.

---

## 1. Thông số kết nối Orthanc PACS

- **AET Title:** `VRRIS_PACS`
- **DICOM Port (C-ECHO, C-STORE):** `4242`
- **HTTP / DICOMweb Port:** `8042`
- **Host:** `localhost` (khi chạy local bằng Docker Compose hoặc K8s Service LoadBalancer)

---

## 2. Kiểm tra kết nối DICOM (C-ECHO / Ping)

Bạn có thể chạy trực tiếp qua công cụ Docker `dcm4che/dcm4che-tools` mà không cần cài đặt dcm4che lên máy:

### Linux / macOS (Bash)
```bash
docker run --rm --network host dcm4che/dcm4che-tools:5.31.0 echoscu -c VRRIS_PACS@localhost:4242
```

### Windows (PowerShell)
```powershell
docker run --rm --network host dcm4che/dcm4che-tools:5.31.0 echoscu -c VRRIS_PACS@localhost:4242
```

> **Kết quả mong đợi:** `Connected to VRRIS_PACS@localhost:4242 in ...ms` và trả về `Received Echo Response`.

---

## 3. Nạp ảnh DICOM vào Orthanc bằng `storescu` (C-STORE)

Giả sử bạn đã tải file ảnh `.dcm` từ TCIA (hoặc thư mục chứa ảnh) vào thư mục `./samples/`:

### Linux / macOS (Bash)
```bash
docker run --rm --network host \
  -v "$(pwd)/samples:/samples:ro" \
  dcm4che/dcm4che-tools:5.31.0 \
  storescu -c VRRIS_PACS@localhost:4242 --bind SIM_CR01 /samples
```

### Windows (PowerShell)
```powershell
docker run --rm --network host `
  -v "${PWD}/samples:/samples:ro" `
  dcm4che/dcm4che-tools:5.31.0 `
  storescu -c VRRIS_PACS@localhost:4242 --bind SIM_CR01 /samples
```

---

## 4. Cách nạp nhanh qua REST API của Orthanc (Không cần dcm4che)

Orthanc hỗ trợ tải ảnh trực tiếp qua HTTP REST:

### Nạp 1 file:
```bash
curl -X POST http://localhost:8042/instances --data-binary @samples/image-00001.dcm
```

### Nạp cả file ZIP chứa series/study:
```bash
curl -X POST http://localhost:8042/instances --data-binary @samples/study.zip
```

---

## 5. Dữ liệu mẫu TCIA và ánh xạ với RIS

1. **Bộ ảnh CR (X-quang ngực):**
   - Nguồn: TCIA Chest X-Ray / NIH Anonymized.
   - Thử nghiệm với study ID = 2 (`study_instance_uid` = `1.2.826.0.1.3680043.8.498.10000002`).
2. **Bộ ảnh CT (CT Sọ não / Lồng ngực):**
   - Nguồn: TCIA LIDC-IDRI (Anonymized CT).
   - Thử nghiệm với study ID = 1 (`study_instance_uid` = `1.2.826.0.1.3680043.8.498.10000001`).

⚠️ **Lưu ý bảo mật (Nguyên tắc bất biến):**
- Orthanc và OHIF chỉ bind trên mạng nội bộ / `localhost`, **không** expose công khai ra Internet.
- Không dùng dữ liệu bệnh nhân thật dưới bất kỳ hình thức nào.

