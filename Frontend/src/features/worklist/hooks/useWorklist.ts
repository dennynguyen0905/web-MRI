import { useAsync } from '@/shared/hooks/useAsync'
import type { Department, Device, Page, ServiceItem, StatusCounts, WorklistRow } from '@/shared/lib/types'
import {
  fetchDepartments, fetchDevices, fetchServiceItems, fetchStatusCounts, fetchWorklist,
  type WorklistFilters,
} from '../api/worklistApi'

/**
 * Component chỉ dùng các hook trong file này, không gọi thẳng tầng api (`doc/15` mục 7).
 */
/**
 * `reloadKey` phải là tham số riêng: bộ lọc được so sánh bằng nội dung (JSON), nên nếu nhét
 * một số đếm vào trong `filters` thì nó sẽ bị gửi lên API như một tham số lọc. Tăng `reloadKey`
 * sau mỗi lần ghi (chuyển trạng thái, lưu báo cáo) để danh sách lấy lại dữ liệu mới.
 */
export function useWorklist(filters: WorklistFilters, reloadKey: number) {
  return useAsync<Page<WorklistRow>>(
    signal => fetchWorklist(filters, signal),
    [JSON.stringify(filters), reloadKey],
  )
}

export function useStatusCounts(reloadKey: number) {
  return useAsync<StatusCounts>(signal => fetchStatusCounts(signal), [reloadKey])
}

export interface ReferenceData {
  departments: Department[]
  devices: Device[]
  serviceItems: ServiceItem[]
}

/** Dữ liệu tham chiếu cho cây máy và các select của bộ lọc; tải một lần. */
export function useReferenceData() {
  return useAsync<ReferenceData>(
    async signal => {
      const [departments, devices, serviceItems] = await Promise.all([
        fetchDepartments(signal),
        fetchDevices(signal),
        fetchServiceItems(signal),
      ])
      return { departments, devices, serviceItems }
    },
    [],
  )
}
