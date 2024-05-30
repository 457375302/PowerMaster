import request from '@/config/axios'

// 机柜历史数据 API
export const HistoryDataApi = {

  // 查询机柜历史数据分页
  getHistoryDataPage: async (params: any) => {
    return await request.get({ url: `/cabinet/history-data/page`, params })
  },

  // 查询机柜历史数据详情
  getHistoryDataDetails: async (params: any) => {
    return await request.get({ url: `/cabinet/history-data/details`, params })
  },

  // 导出机柜历史数据 Excel
  exportHistoryData: async (params) => {
    return await request.download({ url: `/cabinet/history-data/export-excel`, params })
  },

  // 查询机柜导航的一小时数据显示
  getNavOneHourData: async (params: any) => {
    return await request.get({ url: `/cabinet/history-data/one-hour`, params })
  },
}
