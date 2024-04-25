import request from '@/config/axios'

export interface EnvDataVO {
  id: number 
  pduId: number 
  sensorId: number 
  temValue: number
  humValue: number 
  createTime: string 
}

export const EnvDataApi = {
  // 查询pdu历史数据分页
  getEnvDataPage: async (params: any) => {
    return await request.get({ url: `/pdu/history-data/env-page`, params })
  },

  // 查询pdu历史数据详情
  getEnvDataDetails: async (params: any) => {
    return await request.get({ url: `/pdu/history-data/env-details`, params })
  },

  // 导出pdu历史数据 Excel
  exportEnvData: async (params) => {
    return await request.download({ url: `/pdu/history-data/env-export-excel`, params })
  },
}