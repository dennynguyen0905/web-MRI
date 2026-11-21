import { describe, expect, it } from 'vitest'
import { type UpdateStudyFormState, buildUpdatePayloads } from './updatePayloads'

const INITIAL: UpdateStudyFormState = {
  patientCode: 'BN0001',
  fullName: 'Nguyễn Văn Giả',
  dateOfBirth: '1980-01-02',
  gender: 'M',
  socialInsuranceNo: 'DN0000000001',
  phone: '0900000001',
  address: 'Địa chỉ giả',
  visitCode: 'VV0001',
  orderingDoctorName: 'BS. Giả',
  patientClass: 'OUTPATIENT',
  room: '',
  bed: '',
  clinicalDiagnosis: 'Đau đầu',
  deviceId: '1',
  studyInstanceUid: '1.2.3.4',
}

describe('buildUpdatePayloads', () => {
  it('không đổi gì thì không gửi lệnh nào', () => {
    expect(buildUpdatePayloads(INITIAL, { ...INITIAL })).toEqual({ patient: null, order: null, study: null })
  })

  it('chỉ đổi SĐT: KHÔNG gửi trường định danh (CLINICIAN sẽ bị 403 nếu gửi)', () => {
    const p = buildUpdatePayloads(INITIAL, { ...INITIAL, phone: ' 0911111111 ' })
    expect(p.patient).toEqual({ phone: '0911111111' })
    expect(p.order).toBeNull()
    expect(p.study).toBeNull()
  })

  it('chỉ đổi máy chụp: KHÔNG gửi study_instance_uid đang có', () => {
    const p = buildUpdatePayloads(INITIAL, { ...INITIAL, deviceId: '2' })
    expect(p.study).toEqual({ deviceId: 2 })
  })

  it('xóa trắng ô thì gửi null; đổi trường chỉ định chỉ gửi trường đó', () => {
    const p = buildUpdatePayloads(INITIAL, { ...INITIAL, address: '  ', room: 'P101' })
    expect(p.patient).toEqual({ address: null })
    expect(p.order).toEqual({ room: 'P101' })
  })

  it('không bao giờ gửi patient_code', () => {
    const p = buildUpdatePayloads(INITIAL, { ...INITIAL, patientCode: 'BN9999' })
    expect(p.patient).toBeNull()
  })
})
