import type { Gender, PatientClass } from '@/shared/lib/types'
import type { ImagingOrderUpdateInput, PatientUpdateInput, StudyUpdateInput } from '../api/studiesApi'

/** Giá trị form "Cập nhật ca chụp" — mọi ô đều là chuỗi như trong `<input>`/`<select>`. */
export interface UpdateStudyFormState {
  patientCode: string
  fullName: string
  dateOfBirth: string
  gender: string
  socialInsuranceNo: string
  phone: string
  address: string
  visitCode: string
  orderingDoctorName: string
  patientClass: string
  room: string
  bed: string
  clinicalDiagnosis: string
  deviceId: string
  studyInstanceUid: string
}

export interface UpdatePayloads {
  patient: PatientUpdateInput | null
  order: ImagingOrderUpdateInput | null
  study: StudyUpdateInput | null
}

const textOrNull = (v: string) => v.trim() || null

/**
 * Dựng thân các lệnh `PATCH` từ form, **chỉ gồm trường thực sự đổi**.
 *
 * Bắt buộc như vậy vì backend coi mọi trường khác `null` trong `PATCH` là "muốn sửa":
 * - `PATCH /patients/{id}`: ngoài ADMIN/TECHNICIAN, gửi bất kỳ trường định danh nào (họ tên, ngày
 *   sinh, giới tính, BHXH) là bị 403 — kể cả khi giá trị y nguyên (`PatientServiceImpl.update`).
 * - `PATCH /studies/{id}`: gửi `study_instance_uid` khác null mà không phải ADMIN/TECHNICIAN là 403
 *   (`StudyServiceImpl.update`).
 * Gửi cả form thì CLINICIAN chỉ sửa số điện thoại cũng bị từ chối.
 *
 * Lưu ý: xóa trắng một ô sẽ gửi `null`, mà backend hiểu `null` là "giữ nguyên" — hiện chưa xóa
 * được giá trị qua `PATCH` (giới hạn phía backend).
 */
export function buildUpdatePayloads(
  initial: UpdateStudyFormState,
  form: UpdateStudyFormState,
): UpdatePayloads {
  const changed = (key: keyof UpdateStudyFormState) => form[key] !== initial[key]

  const patient: PatientUpdateInput = {}
  if (changed('fullName')) patient.fullName = form.fullName.trim()
  if (changed('dateOfBirth')) patient.dateOfBirth = form.dateOfBirth || null
  if (changed('gender')) patient.gender = form.gender as Gender
  if (changed('socialInsuranceNo')) patient.socialInsuranceNo = textOrNull(form.socialInsuranceNo)
  if (changed('phone')) patient.phone = textOrNull(form.phone)
  if (changed('address')) patient.address = textOrNull(form.address)

  const order: ImagingOrderUpdateInput = {}
  if (changed('visitCode')) order.visitCode = textOrNull(form.visitCode)
  if (changed('orderingDoctorName')) order.orderingDoctorName = form.orderingDoctorName.trim()
  if (changed('patientClass')) order.patientClass = form.patientClass as PatientClass
  if (changed('room')) order.room = textOrNull(form.room)
  if (changed('bed')) order.bed = textOrNull(form.bed)
  if (changed('clinicalDiagnosis')) order.clinicalDiagnosis = textOrNull(form.clinicalDiagnosis)

  const study: StudyUpdateInput = {}
  if (changed('deviceId')) study.deviceId = form.deviceId ? Number(form.deviceId) : null
  if (changed('studyInstanceUid')) study.studyInstanceUid = textOrNull(form.studyInstanceUid)

  const orNull = <T extends object>(o: T): T | null => (Object.keys(o).length ? o : null)
  return { patient: orNull(patient), order: orNull(order), study: orNull(study) }
}
